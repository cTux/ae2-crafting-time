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
    } elseif ($project -eq "P7dR8mSH") { "0.99.0+1.20.1" } elseif ($project -eq "udZtKfzP") { "20.4.2" } else { "1.0.0" }
    $file = [pscustomobject]@{
        filename = $(if ($project -eq "udZtKfzP") { "$project-20.4.2.jar" } else { "$project.jar" })
        hashes = [pscustomobject]@{ sha512 = $hash }
        url = "https://example.invalid/$project.jar"
        primary = $true
    }
    $olderFile = [pscustomobject]@{
        filename = $(if ($project -eq "udZtKfzP") { "$project-20.3.0.jar" } else { "$project.jar" })
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
        version_number = $(if ($project -eq "udZtKfzP") { "20.3.0" } else { "0.0.1" })
        dependencies = @()
        files = @($olderFile)
    })
}

function Assert-Line([string]$text, [string]$expected) {
    if ($expected -notin ($text -split "`r?`n")) { throw "Missing exact line '$expected' in:`n$text" }
}

try {
    $expectedProjects = @{
        "1.20.1-forge" = @("a1RwDz90", "IiATswDj", "E6BFl96N", "udZtKfzP", "ArHeh5Fz", "rxYaglEe", "JiOqfoFM", "xr109llC", "qPydPwtX", "anaGQD2Q", "4inoel9g", "pNabrMMw", "jjuIRIVr", "RYE1pYyr", "IZPmgTLT", "oMgZ004U", "5G4fpXXj", "qelfSMnn", "VQhDBNs8", "SOw6jD6x", "ayN3DZKb")
        "1.20.1-fabric" = @("E6BFl96N", "JiOqfoFM", "pNabrMMw", "jjuIRIVr", "veunMwU3")
        "1.21.1-neoforge" = @("a1RwDz90", "IiATswDj", "rxYaglEe", "E6BFl96N", "udZtKfzP", "ArHeh5Fz", "JiOqfoFM", "xr109llC", "qPydPwtX", "4inoel9g", "pNabrMMw", "jjuIRIVr", "RYE1pYyr", "IZPmgTLT", "oMgZ004U", "qelfSMnn", "VQhDBNs8", "SOw6jD6x", "ayN3DZKb")
        "26.1.2-neoforge" = @("rxYaglEe", "ArHeh5Fz", "JiOqfoFM", "qPydPwtX", "pNabrMMw", "RYE1pYyr", "oMgZ004U", "qelfSMnn", "VQhDBNs8")
    }
    $cases = @(
        @("1.20.1-forge", "runtime loader 1.20.1-99", "runtime ae2 15.99.0", $null,
            "mod udZtKfzP-20.3.0.jar", "mod ArHeh5Fz.jar"),
        @("1.20.1-fabric", "runtime loader 0.99.0", "runtime fabric-api 0.99.0+1.20.1", $null, $null, $null),
        @("1.21.1-neoforge", "runtime loader 21.1.99", "runtime ae2 19.99.0",
            "runtime ae2 group org.appliedenergistics", $null, $null),
        @("26.1.2-neoforge", "runtime loader 26.1.2.99", "runtime ae2 26.99.0-beta", $null, $null, $null)
    )
    foreach ($case in $cases) {
        $output = (& $script -Target $case[0] -Root $temp -ResolveOnly 6>&1 | Out-String)
        Assert-Line $output $case[1]
        Assert-Line $output $case[2]
        if ($case[3]) { Assert-Line $output $case[3] }
        if ($case[4]) { Assert-Line $output $case[4] }
        if ($case[5]) { Assert-Line $output $case[5] }
        foreach ($project in $expectedProjects[$case[0]]) {
            Assert-Line $output $(if ($project -eq "udZtKfzP" -and $case[0] -eq "1.20.1-forge") {
                "mod udZtKfzP-20.3.0.jar"
            } elseif ($project -eq "udZtKfzP") {
                "mod udZtKfzP-20.4.2.jar"
            } else {
                "mod $project.jar"
            })
        }
        $modsDirectory = Join-Path $temp "versions\$($case[0])\run\$(if ($case[0] -eq '1.20.1-forge') { 'resolved-mods' } else { 'mods' })"
        $manifest = Get-Content -LiteralPath (Join-Path $modsDirectory ".ae2-crafting-time-run-mods.json") -Raw | ConvertFrom-Json
        $manifestCount = @($manifest | ForEach-Object { $_ }).Count
        $builtIns = if ($case[0] -eq "1.20.1-fabric") { 1 } else { 2 }
        if ($manifestCount -ne $expectedProjects[$case[0]].Count + $builtIns) {
            throw "Unexpected managed-mod count for $($case[0]): $manifestCount"
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
