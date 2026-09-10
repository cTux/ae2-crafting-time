param(
    [Parameter(Mandatory)][string]$LaunchManifest,
    [Parameter(Mandatory)][string]$BundleDirectory,
    [Parameter(Mandatory)][string]$RuntimeDirectory,
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$Profile,
    [Parameter(Mandatory)][string]$Scenario,
    [Parameter(Mandatory)][string]$World,
    [Parameter(Mandatory)][string]$Evidence,
    [string[]]$ProjectId,
    [string]$DedicatedAddress,
    [string]$ControlDirectory,
    [string]$ContinuationPath,
    [string]$CampaignId,
    [switch]$ResumeOnly,
    [switch]$Interactive
)
$ErrorActionPreference = 'Stop'
$launch = Get-Content -LiteralPath $LaunchManifest -Raw | ConvertFrom-Json
$bundle = Get-Content -LiteralPath (Join-Path $BundleDirectory 'profile.json') -Raw | ConvertFrom-Json
if ($bundle.schema -ne 1 -or $bundle.target -ne $Target -or $bundle.profile -ne $Profile -or
        $bundle.dependencyMode -notin @('base','catalogue') -or
        $launch.target -ne $Target -or $launch.java -ne $bundle.java) { throw 'Prepared launch target/profile/Java mismatch' }
# The installed native loader must be the resolved profile's loader, including latest diagnostics.
$loaderVersion = $bundle.loader -replace ('^' + [regex]::Escape($Target.Split('-')[0]) + '-'), ''
$loaderArguments = @($launch.arguments | Where-Object { $_ -match "(^|[-])$([regex]::Escape($loaderVersion))($|[-])" })
if (-not $loaderArguments.Count) { throw "Prepared loader does not match resolved loader $($bundle.loader)" }
$runtime = [IO.Path]::GetFullPath($RuntimeDirectory)
$ownedRoot = [IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $PSScriptRoot) 'build/ui-smoke'))
if (-not $runtime.StartsWith($ownedRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Native smoke runtime must stay inside build/ui-smoke'
}
$mods = Join-Path $runtime 'mods'
New-Item -ItemType Directory -Path $mods -Force | Out-Null
if ($Target -like '*-neoforge') {
    $fmlConfig = Join-Path $runtime 'config/fml.toml'
    $fmlTemplate = Join-Path ([string]$launch.guest) 'config/fml.toml'
    if (-not (Test-Path -LiteralPath $fmlTemplate -PathType Leaf)) { throw 'Prepared NeoForge launch has no FML config template' }
    $snapshot = Join-Path $Evidence 'fml-prelaunch.toml'
    $absent = Join-Path $Evidence 'fml-prelaunch.absent'
    Remove-Item -LiteralPath $snapshot, $absent -Force -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $fmlConfig -PathType Leaf) {
        Copy-Item -LiteralPath $fmlConfig -Destination $snapshot
        Remove-Item -LiteralPath $fmlConfig -Force
    } else {
        [IO.File]::WriteAllText($absent, '', [Text.UTF8Encoding]::new($false))
    }
    New-Item -ItemType Directory -Path (Split-Path $fmlConfig) -Force | Out-Null
    Copy-Item -LiteralPath $fmlTemplate -Destination $fmlConfig -Force
}
$manifest = Get-Content -LiteralPath (Join-Path $BundleDirectory 'mods/.ae2-crafting-time-run-mods.json') -Raw | ConvertFrom-Json
foreach ($name in $manifest) {
    if ([IO.Path]::GetFileName($name) -ne $name -or $name -notlike '*.jar') { throw 'Invalid bundle filename' }
}
Get-ChildItem -LiteralPath $mods -File -Filter '*.jar' | Remove-Item -Force
foreach ($name in $manifest) {
    $source = Join-Path $BundleDirectory "mods/$name"
    $destination = Join-Path $mods $name
    Copy-Item -LiteralPath $source -Destination $destination
    if ((Get-FileHash -LiteralPath $source).Hash -ne (Get-FileHash -LiteralPath $destination).Hash) {
        throw "Staged artifact hash differs: $name"
    }
}
Copy-Item -LiteralPath (Join-Path $BundleDirectory 'mods/.ae2-crafting-time-run-mods.json') -Destination $mods -Force
Copy-Item -LiteralPath (Join-Path $BundleDirectory 'profile.json') -Destination $Evidence -Force
$arguments = [Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $launch.arguments.Count; $i++) {
    $argument = [string]$launch.arguments[$i]
    if ($argument -match '^-Dae2craftingtime.test\.' -or $argument -match '^-Xm[xs]') { continue }
    if ($argument -in @('--gameDir', '--quickPlaySingleplayer', '--quickPlayMultiplayer')) { $i++; continue }
    $arguments.Add($argument)
}
$arguments.Insert(0, '-Xmx8G')
if ($Interactive) { $arguments.Insert(0, '-Dae2craftingtime.test.interactive=true') }
if ('rxYaglEe' -in @($ProjectId)) { $arguments.Insert(0, '-Dae2craftingtime.test.advancedStatus=true') }
foreach ($property in @("scenario=$Scenario", "profile=$Profile", "world=$World", "output=$Evidence", 'vmTextureProbe=true')) {
    $arguments.Insert(0, "-Dae2craftingtime.test.$property")
}
if ($CampaignId) {
    if ($CampaignId -cnotmatch '^[A-Za-z0-9._-]{1,128}$') { throw 'Invalid UI-smoke campaign identity' }
    $arguments.Insert(0, "-Dae2craftingtime.test.campaign=$CampaignId")
}
if ($ContinuationPath) {
    $continuation = [IO.Path]::GetFullPath($ContinuationPath)
    if (!(Test-Path -LiteralPath $continuation -PathType Leaf) -or
            !($continuation.StartsWith(([IO.Path]::GetFullPath($Evidence)).TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase))) {
        throw 'Relaunch continuation must be an existing evidence file'
    }
    $arguments.Insert(0, "-Dae2craftingtime.test.continuation=$continuation")
}
if ($ResumeOnly) {
    if (!$ContinuationPath -or $Scenario -ne 'cpu-list-total-ttc') { throw 'Resume-only launch requires a CPU-list continuation' }
    $arguments.Insert(0, '-Dae2craftingtime.test.resumeOnly=true')
}
if ($DedicatedAddress) {
    if (-not $ControlDirectory) { throw 'Connected dedicated launch requires a control directory' }
    $arguments.Insert(0, '-Dae2craftingtime.test.connectedDedicated=true')
    $arguments.Insert(0, "-Dae2craftingtime.test.dedicatedAddress=$DedicatedAddress")
    $arguments.Insert(0, "-Dae2craftingtime.test.control=$([IO.Path]::GetFullPath($ControlDirectory))")
}
$arguments.Add('--gameDir'); $arguments.Add($runtime)
if (-not $DedicatedAddress) { $arguments.Add('--quickPlaySingleplayer'); $arguments.Add($World) }
$argsFile = Join-Path $runtime 'ui-smoke-java.args'
$quoted = @($arguments | ForEach-Object { '"' + $_.Replace('\', '\\').Replace('"', '\"') + '"' })
[IO.File]::WriteAllLines($argsFile, $quoted, [Text.UTF8Encoding]::new($false))
$javaHome = & (Join-Path $PSScriptRoot 'get-java-home.ps1') -Major $bundle.java
[pscustomobject]@{ executable = (Join-Path $javaHome 'bin/java.exe'); arguments = ('@"' + $argsFile + '"'); finalApproval = !$ResumeOnly }
