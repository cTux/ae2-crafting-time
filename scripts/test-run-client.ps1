$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "run-client.ps1"
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-run-client-" + [guid]::NewGuid().ToString("N"))
$bytes = [Text.Encoding]::UTF8.GetBytes("test mod")
$sha512 = [Security.Cryptography.SHA512]::Create()
try { $hash = ([BitConverter]::ToString($sha512.ComputeHash($bytes)) -replace "-", "").ToLowerInvariant() } finally { $sha512.Dispose() }

function Invoke-WebRequest {
    param([switch]$UseBasicParsing, [string]$Uri, [string]$OutFile)
    if ($OutFile) {
        [IO.File]::WriteAllBytes($OutFile, $(if ($global:Ae2CtBadDownload) { [byte[]](0) } else { $bytes }))
        return
    }
    $versions = if ($Uri -like "*minecraftforge*") { @("1.20.1-1", "1.20.1-99") } elseif ($Uri -like "*fabric-loader*") { @("0.1.0", "0.99.0") } else { @("21.1.1", "21.1.99", "26.1.2.1", "26.1.2.99") }
    return [pscustomobject]@{ Content = "<metadata><versioning><versions>$($versions.ForEach({ "<version>$_</version>" }) -join '')</versions></versioning></metadata>" }
}

function Invoke-RestMethod {
    param([string]$Uri)
    $project = [regex]::Match($Uri, "/project/([^/]+)/version").Groups[1].Value
    $decoded = [uri]::UnescapeDataString($Uri)
    $version = if ($project -eq "XxWD5pD3") {
        if ($decoded -like '*26.1.2*') { "26.99.0-beta" } elseif ($decoded -like '*1.21.1*') { "19.99.0" } else { "15.99.0" }
    } elseif ($project -eq "P7dR8mSH") { "0.99.0+1.20.1" } else { "1.0.0" }
    $file = [pscustomobject]@{
        filename = "$project.jar"
        hashes = [pscustomobject]@{ sha512 = $hash }
        url = "https://example.invalid/$project.jar"
        primary = $true
    }
    return @([pscustomobject]@{
        version_type = if ($decoded -like '*26.1.2*') { "beta" } else { "release" }
        version_number = $version
        dependencies = @([pscustomobject]@{ project_id = "XxWD5pD3"; version_id = ""; dependency_type = "required" })
        files = @($file)
    }, [pscustomobject]@{
        version_type = "release"
        version_number = "0.0.1"
        dependencies = @()
        files = @($file)
    })
}

function Assert-Line([string]$text, [string]$expected) {
    if ($expected -notin ($text -split "`r?`n")) { throw "Missing exact line '$expected' in:`n$text" }
}

try {
    New-Item -ItemType Directory -Path (Join-Path $temp "scripts") -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot "release-matrix.json") `
        -Destination (Join-Path $temp "scripts\release-matrix.json")
    $cases = @(
        @("1.20.1-forge", "runtime loader 1.20.1-99", "runtime ae2 15.99.0", $null),
        @("1.20.1-fabric", "runtime loader 0.99.0", "runtime fabric-api 0.99.0+1.20.1", $null),
        @("1.21.1-neoforge", "runtime loader 21.1.99", "runtime ae2 19.99.0",
            "runtime ae2 group org.appliedenergistics"),
        @("26.1.2-neoforge", "runtime loader 26.1.2.99", "runtime ae2 26.99.0-beta", $null)
    )
    foreach ($case in $cases) {
        $output = (& $script -Target $case[0] -Root $temp -ResolveOnly 6>&1 | Out-String)
        Assert-Line $output $case[1]
        Assert-Line $output $case[2]
        if ($case[3]) { Assert-Line $output $case[3] }
    }
    foreach ($projectId in @("udZtKfzP", "ArHeh5Fz")) {
        if (-not (Test-Path -LiteralPath (Join-Path $temp "versions\1.20.1-forge\run\resolved-mods\$projectId.jar"))) {
            throw "Forge run client did not install optional project $projectId"
        }
    }
    Remove-Item -LiteralPath (Join-Path $temp "versions\1.20.1-forge\run\resolved-mods\Ck4E7v7R.jar") -Force
    $global:Ae2CtBadDownload = $true
    try {
        & $script -Target "1.20.1-forge" -Root $temp -ResolveOnly 6>&1 | Out-Null
        throw "Expected a hash mismatch"
    } catch {
        if ($_.Exception.Message -notlike "Hash mismatch for *") { throw }
    } finally {
        $global:Ae2CtBadDownload = $false
    }
    Write-Host "run-client checks passed"
} finally {
    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    $resolvedTemp = [IO.Path]::GetFullPath($temp)
    if ($resolvedTemp.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $resolvedTemp)) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
    }
}
