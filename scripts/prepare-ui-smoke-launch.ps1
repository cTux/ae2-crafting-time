param(
    [Parameter(Mandatory)][string]$LaunchManifest,
    [Parameter(Mandatory)][string]$BundleDirectory,
    [Parameter(Mandatory)][string]$RuntimeDirectory,
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$Profile,
    [Parameter(Mandatory)][string]$Scenario,
    [Parameter(Mandatory)][string]$World,
    [Parameter(Mandatory)][string]$Evidence
)
$ErrorActionPreference = 'Stop'
$launch = Get-Content -LiteralPath $LaunchManifest -Raw | ConvertFrom-Json
$bundle = Get-Content -LiteralPath (Join-Path $BundleDirectory 'profile.json') -Raw | ConvertFrom-Json
if ($bundle.schema -ne 1 -or $bundle.target -ne $Target -or $bundle.profile -ne $Profile -or
        $launch.target -ne $Target -or $launch.java -ne $bundle.java) { throw 'Prepared launch target/profile/Java mismatch' }
# The installed native loader must be the resolved profile's loader, including latest diagnostics.
$loaderArguments = @($launch.arguments | Where-Object { $_ -match "(^|[-])$([regex]::Escape($bundle.loader))($|[-])" })
if (-not $loaderArguments.Count) { throw "Prepared loader does not match resolved loader $($bundle.loader)" }
$runtime = [IO.Path]::GetFullPath($RuntimeDirectory)
$ownedRoot = [IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $PSScriptRoot) 'build/ui-smoke'))
if (-not $runtime.StartsWith($ownedRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Native smoke runtime must stay inside build/ui-smoke'
}
$mods = Join-Path $runtime 'mods'
New-Item -ItemType Directory -Path $mods -Force | Out-Null
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
    if ($argument -in @('--gameDir', '--quickPlaySingleplayer')) { $i++; continue }
    $arguments.Add($argument)
}
$arguments.Insert(0, '-Xmx8G')
foreach ($property in @("scenario=$Scenario", "profile=$Profile", "world=$World", "output=$Evidence", 'vmTextureProbe=true')) {
    $arguments.Insert(0, "-Dae2craftingtime.test.$property")
}
$arguments.Add('--gameDir'); $arguments.Add($runtime)
$arguments.Add('--quickPlaySingleplayer'); $arguments.Add($World)
$argsFile = Join-Path $runtime 'ui-smoke-java.args'
$quoted = @($arguments | ForEach-Object { '"' + $_.Replace('\', '\\').Replace('"', '\"') + '"' })
[IO.File]::WriteAllLines($argsFile, $quoted, [Text.UTF8Encoding]::new($false))
$javaHome = & (Join-Path $PSScriptRoot 'get-java-home.ps1') -Major $bundle.java
[pscustomobject]@{ executable = (Join-Path $javaHome 'bin/java.exe'); arguments = ('@"' + $argsFile + '"') }
