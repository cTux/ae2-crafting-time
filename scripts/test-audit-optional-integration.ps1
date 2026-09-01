$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$script = Join-Path $PSScriptRoot "audit-optional-integration.ps1"
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-audit-test-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $temp -Force | Out-Null

function New-TestJar([string]$name, [string]$entryName, [string]$metadata) {
    $path = Join-Path $temp $name
    $archive = [IO.Compression.ZipFile]::Open($path, "Create")
    try {
        $entry = $archive.CreateEntry($entryName)
        $writer = [IO.StreamWriter]::new($entry.Open())
        try { $writer.Write($metadata) } finally { $writer.Dispose() }
    } finally { $archive.Dispose() }
    return $path
}

function New-Version([string]$id, [string]$number, [string]$path, [string]$type = "release", [string]$date = "2025-01-01T00:00:00Z") {
    [pscustomobject]@{
        id = $id; version_number = $number; version_type = $type; date_published = $date
        files = @([pscustomobject]@{
            filename = [IO.Path]::GetFileName($path); url = "https://example.invalid/$([IO.Path]::GetFileName($path))"; primary = $true
            hashes = [pscustomobject]@{ sha512 = (Get-FileHash -LiteralPath $path -Algorithm SHA512).Hash.ToLowerInvariant() }
        })
    }
}

try {
    $forgeOld = New-TestJar "forge-old.jar" "META-INF/mods.toml" @'
[[dependencies.demo]]
modId="minecraft"
versionRange="[1.20.1]"
[[dependencies.demo]]
modId="forge"
versionRange="[47,)"
[[dependencies.demo]]
modId="ae2"
versionRange="[14,15)"
'@
    $forge = New-TestJar "forge.jar" "META-INF/mods.toml" @'
[[dependencies.demo]]
modId="minecraft"
versionRange="[1.20.1,1.21)"
[[dependencies.demo]]
modId="forge"
versionRange="[47.1.3,)"
[[dependencies.demo]]
modId="ae2"
versionRange="[15.0.10,16)"
'@
    $fabric = New-TestJar "fabric.jar" "fabric.mod.json" '{"depends":{"minecraft":"1.20.1","fabricloader":">=0.14.21","ae2":">=15.0.10 <16"}}'
    $neo = New-TestJar "neo.jar" "META-INF/neoforge.mods.toml" @'
[[dependencies.demo]]
modId="minecraft"
versionRange="[1.21.1]"
[[dependencies.demo]]
modId="neoforge"
versionRange="[21.1.1,)"
[[dependencies.demo]]
modId="ae2"
versionRange="[19.0.24,20)"
'@
    $global:Ae2CtAuditFiles = @{}
    foreach ($path in @($forgeOld, $forge, $fabric, $neo)) { $global:Ae2CtAuditFiles[[IO.Path]::GetFileName($path)] = $path }
    $global:Ae2CtAuditVersions = @{
        forge = @(
            (New-Version "forge-old" "0.9.0" $forgeOld -date "2024-01-01T00:00:00Z"),
            (New-Version "forge-ok" "1.0.0" $forge -date "2024-02-01T00:00:00Z")
        )
        fabric = @((New-Version "fabric-ok" "1.0.0" $fabric))
        neoforge1211 = @((New-Version "neo-ok" "1.0.0" $neo))
        neoforge2612 = @((New-Version "neo-beta" "2.0.0-beta" $neo "beta"))
    }

    function Invoke-RestMethod {
        param([string]$Uri)
        $decoded = [uri]::UnescapeDataString($Uri)
        if ($decoded -like '*"forge"*') { return $global:Ae2CtAuditVersions.forge }
        if ($decoded -like '*"fabric"*') { return $global:Ae2CtAuditVersions.fabric }
        if ($decoded -like '*26.1.2*') { return $global:Ae2CtAuditVersions.neoforge2612 }
        return $global:Ae2CtAuditVersions.neoforge1211
    }
    function Invoke-WebRequest {
        param([switch]$UseBasicParsing, [string]$Uri, [string]$OutFile)
        $name = [IO.Path]::GetFileName(([uri]$Uri).AbsolutePath)
        Copy-Item -LiteralPath $global:Ae2CtAuditFiles[$name] -Destination $OutFile
        if ($global:Ae2CtAuditBadHash) { Add-Content -LiteralPath $OutFile -Value "bad" }
    }

    $result = @(& $script -ProjectId demo)
    if ($result.Count -ne 4 -or @($result | Where-Object Status -eq "SUPPORTED").Count -ne 3) {
        throw "Audit did not report all supported rows: $($result | ConvertTo-Json -Compress)"
    }
    if (($result | Where-Object Target -eq "1.20.1-forge").VersionId -ne "forge-ok") {
        throw "Audit did not reject the incompatible older artifact"
    }
    if (($result | Where-Object Target -eq "26.1.2-neoforge").Status -ne "UNSUPPORTED") {
        throw "Audit selected a prerelease-only row"
    }

    $source = Get-Content -LiteralPath $script -Raw
    if ($source -match '\[string\]\$ReleaseMatrix\s*=|\[string\]\$RunClientMatrix\s*=') {
        throw "Audit matrix defaults must be resolved after parameter binding"
    }
    if ($source -match '\$availableVersions\s*=\s*@\(Invoke-RestMethod') {
        throw "Audit must enumerate Windows PowerShell REST arrays before filtering"
    }

    $global:Ae2CtAuditBadHash = $true
    try {
        & $script -ProjectId demo | Out-Null
        throw "Expected a hash mismatch"
    } catch {
        if ($_.Exception.Message -notlike "SHA-512 mismatch for Modrinth version *") { throw }
    } finally { $global:Ae2CtAuditBadHash = $false }
    Write-Host "optional-integration audit checks passed"
} finally {
    Remove-Item Function:\Invoke-RestMethod -ErrorAction SilentlyContinue
    Remove-Item Function:\Invoke-WebRequest -ErrorAction SilentlyContinue
    Remove-Variable Ae2CtAuditFiles, Ae2CtAuditVersions, Ae2CtAuditBadHash -Scope Global -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
