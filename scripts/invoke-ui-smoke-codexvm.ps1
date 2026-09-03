param(
    [ValidateSet("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")][string]$Target = "1.20.1-forge",
    [ValidateSet("OpenSSH", "Vmrun")][string]$Transport = "OpenSSH",
    [switch]$Latest,
    [switch]$Interactive,
    [switch]$Stop,
    [ValidatePattern("^(suite|standard-ae2|craft-plan|no-space-status|no-provider-status|no-power-status|crafting-tree-screen|merequester-screen|ae2networkanalyser-screen|aeinfinitybooster-terminal|ae2importexportcard-terminal|ae2(?:wcwt|wtlib)-terminal|[a-z0-9]+(?:-[a-z0-9]+)*-cpu)$")][string]$Scenario = "craft-plan",
    [string[]]$ProjectId,
    [string]$SshUser = "Codex",
    [string]$SshKeyPath = (Join-Path $env:USERPROFILE ".ssh\codexvm_smoke_ed25519"),
    [string]$VmrunUser = "CodexSmoke",
    [string]$CredentialPath = (Join-Path $env:APPDATA "Codex\codexvm-smoke.credential.xml"),
    [string]$GuestSourceRoot,
    [string]$BundleDirectory,
    [string]$PreparedLaunchRoot = 'C:\Users\Public\Documents\AE2CraftingTimeSmoke\prepared'
)

$ErrorActionPreference = "Stop"
$vmx = "F:\VMs\Codex-Windows11\Codex-Windows11.vmx"
$vmrun = "C:\Program Files\VMware\VMware Workstation\vmrun.exe"
$root = Split-Path -Parent $PSScriptRoot
if (-not $Stop -and -not $BundleDirectory) {
    $campaign = @{ Target = $Target; Scenario = $Scenario; Latest = $Latest; PreparedLaunchRoot = $PreparedLaunchRoot
        ProjectId = $ProjectId; Interactive = $Interactive }
    if ($GuestSourceRoot) { $campaign.GuestSourceRoot = $GuestSourceRoot }
    & (Join-Path $PSScriptRoot 'run-ui-smoke-matrix.ps1') @campaign
    exit $LASTEXITCODE
}
if (-not $GuestSourceRoot) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $name = 'ae2ct-' + ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($root))) -replace '-', '').Substring(0, 12).ToLowerInvariant() }
    finally { $sha.Dispose() }
    $null = & $vmrun -T ws setSharedFolderState $vmx $name $root writable 2>&1
    if ($LASTEXITCODE -ne 0) {
        & $vmrun -T ws addSharedFolder $vmx $name $root
        if ($LASTEXITCODE -ne 0) { throw 'Could not share the current worktree with CodexVM' }
    }
    & $vmrun -T ws enableSharedFolders $vmx
    if ($LASTEXITCODE -ne 0) { throw 'Could not enable the CodexVM worktree share' }
    $GuestSourceRoot = "\\vmware-host\Shared Folders\$name"
}
$guestScript = Join-Path $GuestSourceRoot "scripts\run-ui-smoke-codexvm.ps1"
$smokeArguments = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $guestScript, "-Target", $Target, "-Scenario", $Scenario)
if ($BundleDirectory) {
    $bundlePath = [IO.Path]::GetFullPath($BundleDirectory)
    if (-not $bundlePath.StartsWith($root.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Bundle must be inside the shared worktree'
    }
    $guestBundle = Join-Path $GuestSourceRoot $bundlePath.Substring($root.Length).TrimStart('\')
    $smokeArguments += @('-BundleDirectory', $guestBundle, '-PreparedLaunchRoot', $PreparedLaunchRoot)
}
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
