param(
    [Parameter(Mandatory)][ValidateSet('Seal','Reuse')][string]$Mode,
    [Parameter(Mandatory)][string]$CacheDirectory,
    [Parameter(Mandatory)][ValidatePattern('^[a-fA-F0-9]{40}$')][string]$HeadSha,
    [Parameter(Mandatory)][string]$Fingerprint,
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$Profile,
    [Parameter(Mandatory)][string]$GraphId,
    [switch]$BaseOnly
)
$ErrorActionPreference = 'Stop'
$cache = [IO.Path]::GetFullPath($CacheDirectory)
$identityPath = Join-Path $cache 'bundle-identity.json'
function Get-BundleIdentity {
    $root = $cache.TrimEnd('\')
    $files = @(Get-ChildItem -LiteralPath $root -File -Recurse | Where-Object FullName -ne $identityPath | ForEach-Object {
        [pscustomobject]@{path=$_.FullName.Substring($root.Length).TrimStart('\').Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
    })
    $lines = [Collections.Generic.List[string]]::new()
    foreach ($file in $files) { $lines.Add("$($file.path)|$($file.sha256)") }
    $lines.Sort([StringComparer]::Ordinal)
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $text = $lines -join "`n"
        [ordered]@{sha256=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))) -replace '-','');files=$files}
    } finally { $sha.Dispose() }
}
if ($Mode -eq 'Seal') {
    if (!(Test-Path -LiteralPath (Join-Path $cache 'mods') -PathType Container)) { throw "Cannot seal a missing UI-smoke bundle: $cache" }
    if (Test-Path -LiteralPath $identityPath) { throw 'UI-smoke bundle cache is already sealed' }
    $tree = Get-BundleIdentity
    $identity = [ordered]@{schema=2;headSha=$HeadSha.ToLowerInvariant();fingerprint=$Fingerprint;target=$Target;profile=$Profile
        graphId=$GraphId;baseOnly=[bool]$BaseOnly;bundleSha256=$tree.sha256;artifacts=@($tree.files);sealedAt=[DateTime]::UtcNow.ToString('o')}
    [IO.File]::WriteAllText($identityPath,($identity|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
    return [pscustomobject]@{reused=$false;bundleSha256=$tree.sha256;identityPath=$identityPath}
}
if (!(Test-Path -LiteralPath $identityPath -PathType Leaf)) { throw 'UI-smoke bundle cache is not sealed' }
$identity = Get-Content -LiteralPath $identityPath -Raw | ConvertFrom-Json
if ($identity.schema -ne 2 -or $identity.headSha -ne $HeadSha.ToLowerInvariant() -or $identity.fingerprint -ne $Fingerprint -or
        $identity.target -ne $Target -or $identity.profile -ne $Profile -or $identity.graphId -ne $GraphId -or
        [bool]$identity.baseOnly -ne [bool]$BaseOnly) {
    throw 'UI-smoke bundle cache identity mismatch'
}
$tree = Get-BundleIdentity
if ($tree.sha256 -ne $identity.bundleSha256) { throw 'UI-smoke bundle cache content changed after sealing' }
[pscustomobject]@{reused=$true;bundleSha256=$tree.sha256;identityPath=$identityPath}
