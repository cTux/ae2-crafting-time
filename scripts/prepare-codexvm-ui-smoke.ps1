param(
    [string]$SshKeyPath = (Join-Path $env:USERPROFILE ".ssh\codexvm_smoke_ed25519"),
    [string]$CredentialPath = (Join-Path $env:APPDATA "Codex\codexvm-smoke.credential.xml")
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$configurationPath = Join-Path $root "build\codexvm-ui-smoke-provision.json"

if (-not (Test-Path -LiteralPath $SshKeyPath -PathType Leaf)) {
    New-Item -ItemType Directory -Path (Split-Path -Parent $SshKeyPath) -Force | Out-Null
    $keygen = Start-Process -FilePath "ssh-keygen.exe" -ArgumentList `
        "-q -t ed25519 -N `"`" -f `"$SshKeyPath`"" -Wait -PassThru -NoNewWindow
    if ($keygen.ExitCode -ne 0) { throw "Failed to create the CodexVM SSH key" }
}

$alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%"
$bytes = [byte[]]::new(32)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$password = -join ($bytes | ForEach-Object { $alphabet[$_ % $alphabet.Length] })
$securePassword = ConvertTo-SecureString $password -AsPlainText -Force
$credential = [Management.Automation.PSCredential]::new("CodexSmoke", $securePassword)
New-Item -ItemType Directory -Path (Split-Path -Parent $CredentialPath) -Force | Out-Null
$credential | Export-Clixml -LiteralPath $CredentialPath

$configuration = [ordered]@{
    vmrunUser = "CodexSmoke"
    vmrunPassword = $password
    authorizedKey = (Get-Content -LiteralPath "$SshKeyPath.pub" -Raw).Trim()
}
New-Item -ItemType Directory -Path (Split-Path -Parent $configurationPath) -Force | Out-Null
[IO.File]::WriteAllText($configurationPath, ($configuration | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
$password = $null
Write-Host "Prepared encrypted vmrun credentials and one-time guest configuration: $configurationPath"
