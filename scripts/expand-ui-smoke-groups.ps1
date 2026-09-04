param(
    [Parameter(Mandatory)][string[]]$Scenarios,
    [Parameter(Mandatory)][string]$Target,
    [string]$MatrixDirectory = $PSScriptRoot
)
$ErrorActionPreference = 'Stop'
$release = Get-Content -LiteralPath (Join-Path $MatrixDirectory 'release-matrix.json') -Raw | ConvertFrom-Json
if ($Target -cnotin $release.id) { throw "Unknown target: $Target" }
$catalogue = Get-Content -LiteralPath (Join-Path $MatrixDirectory 'ui-smoke-groups.json') -Raw | ConvertFrom-Json
if ($catalogue.schema -ne 1) { throw 'Invalid smoke group schema' }
$groups = @($catalogue.groups.psobject.Properties.Name)
$leaves = @($catalogue.cases.psobject.Properties.Name)
foreach ($group in $groups) {
    $members = @($catalogue.groups.$group)
    $seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    if (!$members.Count) { throw "Empty group: $group" }
    foreach ($member in $members) {
        if ($member -cin $groups -or $member -cnotin $leaves -or !$seen.Add($member)) {
            throw "Unknown, nested or duplicate group member: $member"
        }
    }
}
$suiteName = if ($Target -eq '26.1.2-neoforge') { 'neoforge-26.1.2' } else { $Target.Split('-')[1] }
$suite = Get-Content -LiteralPath (Join-Path $MatrixDirectory "ui-smoke-$suiteName-suite.json") -Raw | ConvertFrom-Json
$supported = @($suite | ForEach-Object { if ($_ -cin $groups) { $catalogue.groups.$_ } else { $_ } })
# Focused adapter fixtures can intentionally live outside the compatible suite.
$coverage = Get-Content -LiteralPath (Join-Path $MatrixDirectory 'ui-smoke-coverage.json') -Raw | ConvertFrom-Json
$supported += @($coverage.$Target.psobject.Properties.Value.scenario | Where-Object { $_ -cnotin $groups })
foreach ($adapter in $catalogue.adapterCases.psobject.Properties) {
    if (@($adapter.Value | Where-Object { $_ -cin $supported }).Count) { $supported += @($adapter.Value) }
}
$seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$expanded = @($Scenarios | ForEach-Object {
    if ($_ -ceq 'suite') { $suite | ForEach-Object { if ($_ -cin $groups) { $catalogue.groups.$_ } else { $_ } } }
    elseif ($_ -cin $groups) { $catalogue.groups.$_ }
    else { $_ }
} | ForEach-Object {
    if ($_ -cnotmatch '^[a-z0-9]+(?:-[a-z0-9]+)+$' -or $_ -cnotin $supported) { throw "Unsupported case for ${Target}: $_" }
    if ($seen.Add($_)) { $_ }
})
if ($expanded.Count -lt 1 -or $expanded.Count -gt 64) { throw 'Supply 1-64 distinct smoke cases' }
$expanded
