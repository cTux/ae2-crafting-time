param(
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$ArtifactHashes,
    [Parameter(Mandatory)][string]$Evidence,
    [Parameter(Mandatory)][string]$FixtureDirectory
)
$ErrorActionPreference = 'Stop'
function Hash-Text([string]$value) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($value)))).Replace('-','').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
$mods = @(Get-Content -LiteralPath $ArtifactHashes -Raw | ConvertFrom-Json | Where-Object file -NotLike 'ae2-crafting-time-*' | Sort-Object file)
$fixture = @(Get-ChildItem -LiteralPath $FixtureDirectory -Recurse -File | Sort-Object FullName | ForEach-Object {
    "$($_.FullName.Substring($FixtureDirectory.Length))=$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash)"
})
$snapshot = Get-ChildItem -LiteralPath $Evidence -Filter '*.json' -Recurse -File | Where-Object Name -NotIn @('result.json','suite-plan.json') |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json } | Where-Object capture | Select-Object -First 1
if (!$snapshot.capture.os -or !$snapshot.capture.renderer) { throw 'Missing captured render environment' }
$resources = Get-Content -LiteralPath (Join-Path $Evidence 'resource-hashes.json') -Raw | ConvertFrom-Json
[ordered]@{schema=1;target=$Target;graphSha256=(Hash-Text ($mods | ConvertTo-Json -Compress -Depth 5))
    fixtureSha256=(Hash-Text ($fixture -join "`n"));resourceSha256=(Hash-Text ($resources | ConvertTo-Json -Compress -Depth 5))
    os=$snapshot.capture.os;gpuDriver=$snapshot.capture.renderer}
