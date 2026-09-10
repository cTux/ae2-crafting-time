param(
    [Parameter(Mandatory)][ValidateSet('Capture','Restore')][string]$Mode,
    [Parameter(Mandatory)][string]$ResumeDirectory,
    [string]$WorldDirectory,
    [string]$EvidenceDirectory,
    [Parameter(Mandatory)][string]$BundleDirectory,
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$Profile,
    [Parameter(Mandatory)][string]$Scenario,
    [string]$CampaignId,
    [Parameter(Mandatory)][ValidatePattern('^[a-fA-F0-9]{40}$')][string]$HeadSha
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'ui-smoke-dependency-identity.ps1')

function Get-TreeIdentity([string]$Path) {
    $root = [IO.Path]::GetFullPath($Path).TrimEnd('\')
    if (!(Test-Path -LiteralPath $root -PathType Container)) { throw "Missing resume input: $root" }
    $entries = @(Get-ChildItem -LiteralPath $root -File -Recurse | ForEach-Object {
        [pscustomobject]@{ path=$_.FullName.Substring($root.Length).TrimStart('\').Replace('\','/'); sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
    })
    $lines = [Collections.Generic.List[string]]::new()
    foreach ($entry in $entries) { $lines.Add("$($entry.path)|$($entry.sha256)") }
    $lines.Sort([StringComparer]::Ordinal)
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $text = $lines -join "`n"
        [ordered]@{ sha256=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))) -replace '-',''); files=$entries }
    } finally { $sha.Dispose() }
}

function Assert-Continuation([string]$Path, [string]$ExpectedCampaign) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { throw 'Missing CPU-list relaunch continuation' }
    $value = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    if ($value.schema -ne 1 -or $value.phase -ne 'relaunch-ready' -or
            $value.world -cnotmatch '^ae2ct-[a-f0-9]{32}$' -or $value.epoch -ne $ExpectedCampaign -or
            $null -eq $value.serverState -or @($value.checks) -notcontains 'same-jvm-clear' -or
            $value.serverSequence -lt 0 -or $value.clientSequence -le $value.serverSequence) {
        throw 'Invalid CPU-list relaunch continuation'
    }
    foreach ($capture in @($value.screenshots)) {
        if ([IO.Path]::GetFileName($capture) -ne $capture -or !(Test-Path -LiteralPath (Join-Path (Split-Path $Path) $capture) -PathType Leaf)) {
            throw 'Continuation capture is missing or unsafe'
        }
    }
    $value
}

if ($Scenario -ne 'cpu-list-total-ttc') { throw 'Resume bundles are restricted to cpu-list-total-ttc diagnostics' }
$resume = [IO.Path]::GetFullPath($ResumeDirectory)
$bundleIdentity = Get-TreeIdentity $BundleDirectory
$dependencyIdentity = Get-UiSmokeDependencyIdentity $BundleDirectory
if ($Mode -eq 'Capture') {
    if (!$WorldDirectory -or !$EvidenceDirectory -or !$CampaignId) { throw 'Capture requires world, evidence, and campaign inputs' }
    $continuation = Assert-Continuation (Join-Path $EvidenceDirectory 'cpu-list-continuation.json') $CampaignId
    $markerPath = Join-Path $WorldDirectory '.ae2-crafting-time-test-fixture.json'
    if (!(Test-Path -LiteralPath $markerPath -PathType Leaf)) { throw 'Missing disposable world marker' }
    $marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
    if ($marker.schema -ne 1 -or $marker.sourceFixtureId -ne 'ae2-crafting-time' -or
            $marker.disposableWorldId -ne $continuation.world) { throw 'Resume world is not the marked continuation world' }
    $null = Assert-UiSmokeWorldDependencyIdentity $markerPath $dependencyIdentity
    if (Test-Path -LiteralPath $resume) { throw 'Resume bundle already exists' }
    $temporary = "$resume.$([guid]::NewGuid().ToString('N')).tmp"
    New-Item -ItemType Directory -Path $temporary | Out-Null
    try {
        Copy-Item -LiteralPath $WorldDirectory -Destination (Join-Path $temporary 'world') -Recurse
        Copy-Item -LiteralPath $EvidenceDirectory -Destination (Join-Path $temporary 'evidence') -Recurse
        $worldIdentity = Get-TreeIdentity (Join-Path $temporary 'world')
        $evidenceIdentity = Get-TreeIdentity (Join-Path $temporary 'evidence')
        $manifest = [ordered]@{ schema=1; target=$Target; profile=$Profile; scenario=$Scenario; headSha=$HeadSha.ToLowerInvariant()
            campaignId=$CampaignId; world=$continuation.world; phase=2; finalApproval=$false; launchCount=1
            worldSha256=$worldIdentity.sha256; evidenceSha256=$evidenceIdentity.sha256; bundleSha256=$bundleIdentity.sha256
            dependencyMode=$dependencyIdentity.mode; dependencyCatalogueSha256=$dependencyIdentity.catalogueSha256
            continuationSha256=(Get-FileHash -LiteralPath (Join-Path $temporary 'evidence/cpu-list-continuation.json') -Algorithm SHA256).Hash }
        [IO.File]::WriteAllText((Join-Path $temporary 'resume.json'), ($manifest | ConvertTo-Json -Depth 6), [Text.UTF8Encoding]::new($false))
        Move-Item -LiteralPath $temporary -Destination $resume
    } catch {
        if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Recurse -Force }
        throw
    }
}

$manifestPath = Join-Path $resume 'resume.json'
if (!(Test-Path -LiteralPath $manifestPath -PathType Leaf)) { throw 'Missing resume manifest' }
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
if ($manifest.schema -ne 1 -or $manifest.target -ne $Target -or $manifest.profile -ne $Profile -or
        $manifest.scenario -ne $Scenario -or $manifest.headSha -ne $HeadSha.ToLowerInvariant() -or
        $manifest.phase -ne 2 -or $manifest.finalApproval -ne $false -or $manifest.launchCount -ne 1 -or
        $manifest.dependencyMode -ne $dependencyIdentity.mode -or
        $manifest.dependencyCatalogueSha256 -ne $dependencyIdentity.catalogueSha256) {
    throw 'Resume manifest does not match the requested immutable build'
}
$worldIdentity = Get-TreeIdentity (Join-Path $resume 'world')
$evidenceIdentity = Get-TreeIdentity (Join-Path $resume 'evidence')
if ($worldIdentity.sha256 -ne $manifest.worldSha256 -or $evidenceIdentity.sha256 -ne $manifest.evidenceSha256 -or
        $bundleIdentity.sha256 -ne $manifest.bundleSha256) { throw 'Resume world, evidence, or artifact bundle was modified' }
$continuationPath = Join-Path $resume 'evidence/cpu-list-continuation.json'
$continuation = Assert-Continuation $continuationPath $manifest.campaignId
if ((Get-FileHash -LiteralPath $continuationPath -Algorithm SHA256).Hash -ne $manifest.continuationSha256) { throw 'Resume continuation was modified' }
$null = Assert-UiSmokeWorldDependencyIdentity (Join-Path $resume 'world/.ae2-crafting-time-test-fixture.json') $dependencyIdentity
[pscustomobject]@{ phase=2; finalApproval=$false; launchCount=1; world=$manifest.world; campaignId=$manifest.campaignId
    worldDirectory=(Join-Path $resume 'world'); evidenceDirectory=(Join-Path $resume 'evidence'); continuationPath=$continuationPath
    predecessorSha256=$manifest.continuationSha256; bundleSha256=$manifest.bundleSha256
    dependencyMode=$manifest.dependencyMode; dependencyCatalogueSha256=$manifest.dependencyCatalogueSha256 }
