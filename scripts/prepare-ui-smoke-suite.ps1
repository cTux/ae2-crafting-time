param(
    [ValidateSet("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")][string]$Target = "1.20.1-forge",
    [Parameter(Mandatory)][string]$RuntimeDirectory,
    [Parameter(Mandatory)][string]$OutputDirectory,
    [switch]$VanillaMetadata,
    [Parameter(Mandatory)][string[]]$Scenarios
)
$ErrorActionPreference = 'Stop'
$runtime = [IO.Path]::GetFullPath($RuntimeDirectory)
$output = [IO.Path]::GetFullPath($OutputDirectory)
$fixtureTarget = if ($Target -like '*-neoforge') { $Target } else { '1.20.1-forge' }
$fixture = Join-Path (Split-Path -Parent $PSScriptRoot) "versions\$fixtureTarget\run\saves\ae2-crafting-time"
if ($Scenarios.Count -lt 1 -or $Scenarios.Count -gt 64 -or
        @($Scenarios | Select-Object -Unique).Count -ne $Scenarios.Count) { throw 'Supply 1-64 distinct scenarios' }
foreach ($scenario in $Scenarios) {
    if ($scenario -notmatch '^[a-z0-9]+(?:-[a-z0-9]+)+$') { throw "Invalid scenario: $scenario" }
}
$Scenarios = @(& (Join-Path $PSScriptRoot 'expand-ui-smoke-groups.ps1') -Target $Target -Scenarios $Scenarios)
if (Test-Path -LiteralPath $output) { throw 'Suite output must be a new directory; preserve earlier results' }
$sourceMarker = Get-Content -LiteralPath (Join-Path $fixture '.ae2-crafting-time-test-fixture.json') -Raw | ConvertFrom-Json
if ($sourceMarker.sourceFixtureId -ne 'ae2-crafting-time' -or $sourceMarker.disposableWorldId -ne 'SOURCE_ONLY') {
    throw 'Not the tracked source fixture'
}
New-Item -ItemType Directory -Path $output -ErrorAction Stop | Out-Null
New-Item -ItemType Directory -Path (Join-Path $runtime 'saves') -Force | Out-Null
$cases = foreach ($scenario in $Scenarios) {
    $world = 'ae2ct-' + [guid]::NewGuid().ToString('N')
    $copy = Join-Path $runtime "saves\$world"
    & (Join-Path $PSScriptRoot 'copy-ui-smoke-fixture.ps1') -Source $fixture -Destination $copy -Target $Target -VanillaMetadata:$VanillaMetadata
    $sourceMarker.disposableWorldId = $world
    $sourceMarker | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $copy '.ae2-crafting-time-test-fixture.json') -Encoding UTF8
    [ordered]@{scenario=$scenario;world=$world}
}
[ordered]@{schema=1;cases=@($cases)} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $output 'suite-plan.json') -Encoding UTF8
[pscustomobject]@{scenario='suite';world=@($cases)[0].world;output=$output;caseCount=@($cases).Count}
