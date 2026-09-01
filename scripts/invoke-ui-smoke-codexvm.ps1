param(
    [ValidateSet("OpenSSH", "Vmrun")][string]$Transport = "OpenSSH",
    [switch]$Latest,
    [switch]$Interactive,
    [switch]$Stop,
    [ValidatePattern("^(craft-plan|merequester-screen|ae2networkanalyser-screen|aeinfinitybooster-terminal|ae2importexportcard-terminal|ae2(?:wcwt|wtlib)-terminal|[a-z0-9]+(?:-[a-z0-9]+)*-cpu)$")][string]$Scenario = "craft-plan",
    [string[]]$ProjectId,
    [string]$SshUser = "Codex",
    [string]$SshKeyPath = (Join-Path $env:USERPROFILE ".ssh\codexvm_smoke_ed25519"),
    [string]$VmrunUser = "CodexSmoke",
    [string]$CredentialPath = (Join-Path $env:APPDATA "Codex\codexvm-smoke.credential.xml"),
    [string]$GuestSourceRoot
)

$ErrorActionPreference = "Stop"
$vmx = "F:\VMs\Codex-Windows11\Codex-Windows11.vmx"
$vmrun = "C:\Program Files\VMware\VMware Workstation\vmrun.exe"
$root = Split-Path -Parent $PSScriptRoot
if (-not $GuestSourceRoot) {
    $projects = [IO.Path]::GetFullPath("E:\projects")
    $resolvedRoot = [IO.Path]::GetFullPath($root)
    if (-not $resolvedRoot.StartsWith($projects, [StringComparison]::OrdinalIgnoreCase)) {
        throw "GuestSourceRoot is required outside E:\projects"
    }
    $GuestSourceRoot = "\\vmware-host\Shared Folders\projects$($resolvedRoot.Substring($projects.Length))"
}
$guestScript = Join-Path $GuestSourceRoot "scripts\run-ui-smoke-codexvm.ps1"
$smokeArguments = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $guestScript, "-Scenario", $Scenario)
if ($Latest) { $smokeArguments += "-Latest" }
if ($Interactive) { $smokeArguments += "-Interactive" }
if ($ProjectId) { $smokeArguments += @("-ProjectId") + $ProjectId }
if ($Stop) { $smokeArguments += "-Stop" } else { $smokeArguments += @("-Scheduled", "-InteractiveUser", "Codex") }

if ($Transport -eq "OpenSSH") {
    $ip = (& $vmrun -T ws getGuestIPAddress $vmx -wait).Trim()
    if ($LASTEXITCODE -ne 0 -or $ip -notmatch '^\d{1,3}(?:\.\d{1,3}){3}$') { throw "Could not resolve the CodexVM guest IP" }
    $remote = $smokeArguments | ForEach-Object { '"' + ($_ -replace '"', '\"') + '"' }
    & ssh.exe -i $SshKeyPath -o BatchMode=yes -o ConnectTimeout=10 "$SshUser@$ip" powershell.exe ($remote -join ' ')
    if ($LASTEXITCODE -ne 0) { throw "OpenSSH UI-smoke dispatch failed with exit $LASTEXITCODE" }
    exit 0
}

if (-not (Test-Path -LiteralPath $CredentialPath -PathType Leaf)) {
    throw "Missing encrypted vmrun credential: $CredentialPath"
}
$credential = Import-Clixml -LiteralPath $CredentialPath
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($credential.Password)
try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    & $vmrun -T ws -gu $VmrunUser -gp $password runProgramInGuest $vmx `
        "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" @smokeArguments
    if ($LASTEXITCODE -ne 0) { throw "vmrun UI-smoke dispatch failed with exit $LASTEXITCODE" }
} finally {
    $password = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}
