param([Parameter(Mandatory)][string]$ConfigurationPath)

$ErrorActionPreference = "Stop"
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = [Security.Principal.WindowsPrincipal]::new($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Run CodexVM UI-smoke setup from an elevated PowerShell window"
}

try {
    $configuration = Get-Content -LiteralPath $ConfigurationPath -Raw | ConvertFrom-Json
    $password = ConvertTo-SecureString $configuration.vmrunPassword -AsPlainText -Force
    $account = Get-LocalUser -Name $configuration.vmrunUser -ErrorAction SilentlyContinue
    if ($account) { Set-LocalUser -Name $configuration.vmrunUser -Password $password }
    else { New-LocalUser -Name $configuration.vmrunUser -Password $password -PasswordNeverExpires | Out-Null }
    Add-LocalGroupMember -Group "Administrators" -Member $configuration.vmrunUser -ErrorAction SilentlyContinue
    $stageRoot = Join-Path $env:PUBLIC "Documents\AE2CraftingTimeSmoke"
    New-Item -ItemType Directory -Path $stageRoot -Force | Out-Null
    & icacls.exe $stageRoot /grant "Users:(OI)(CI)M" | Out-Null

    $capability = Get-WindowsCapability -Online -Name "OpenSSH.Server*"
    if ($capability.State -ne "Installed") { Add-WindowsCapability -Online -Name $capability.Name | Out-Null }
    $authorizedKeys = Join-Path $env:ProgramData "ssh\administrators_authorized_keys"
    $existingKeys = if (Test-Path -LiteralPath $authorizedKeys) { Get-Content -LiteralPath $authorizedKeys } else { @() }
    if ($configuration.authorizedKey -notin $existingKeys) {
        [IO.File]::AppendAllText($authorizedKeys, "$($configuration.authorizedKey)`r`n", [Text.UTF8Encoding]::new($false))
    }
    & icacls.exe $authorizedKeys /inheritance:r /grant "Administrators:F" /grant "SYSTEM:F" | Out-Null
    Set-Service -Name sshd -StartupType Automatic
    Start-Service -Name sshd
    if (-not (Get-NetFirewallRule -Name "OpenSSH-Server-In-TCP" -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -Name "OpenSSH-Server-In-TCP" -DisplayName "OpenSSH Server (sshd)" `
            -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22 | Out-Null
    }
    Write-Host "CodexVM UI-smoke transports are ready"
} finally {
    $password = $null
    Remove-Item -LiteralPath $ConfigurationPath -Force -ErrorAction SilentlyContinue
}
