param([string[]]$ProjectId)
$ids = [string[]]@($ProjectId | Where-Object { $null -ne $_ })
[Array]::Sort($ids, [StringComparer]::Ordinal)
$unique = [Collections.Generic.List[string]]::new()
foreach ($id in $ids) {
    if (!$unique.Count -or ![StringComparer]::Ordinal.Equals($unique[$unique.Count - 1], $id)) { $unique.Add($id) }
}
$canonical = [string[]]$unique.ToArray()
$json = ConvertTo-Json -InputObject $canonical -Compress
$sha = [Security.Cryptography.SHA256]::Create()
try { $digest = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($json))) -replace '-', '').ToLowerInvariant() }
finally { $sha.Dispose() }
[pscustomobject]@{ projectId=$canonical; digest=$digest }
