$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "run-client.ps1"
$matrix = Get-Content -LiteralPath (Join-Path $PSScriptRoot "run-client-versions.json") -Raw | ConvertFrom-Json
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-run-client-" + [guid]::NewGuid().ToString("N"))
$bytes = [Text.Encoding]::UTF8.GetBytes("test mod")
$sha512 = [Security.Cryptography.SHA512]::Create()
try { $hash = ([BitConverter]::ToString($sha512.ComputeHash($bytes)) -replace "-", "").ToLowerInvariant() } finally { $sha512.Dispose() }
New-Item -ItemType Directory -Path $temp -Force | Out-Null
$modVersion = ((Get-Content -LiteralPath (Join-Path (Split-Path -Parent $PSScriptRoot) "gradle.properties")) |
    Where-Object { $_ -match '^modVersion=' } | Select-Object -First 1) -replace '^modVersion=', ''
[IO.File]::WriteAllText((Join-Path $temp "gradle.properties"), "modVersion=$modVersion`n", [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $temp "gradlew.bat"), @"
@echo off
if defined AE2CT_DRIVER_BUILD_FAIL exit /b 9
if not exist "%~dp0build\test-driver" mkdir "%~dp0build\test-driver"
>"%~dp0build\test-driver\ae2-crafting-time-$modVersion-forge-1.20.1-test-driver.jar" echo driver
exit /b 0
"@, [Text.UTF8Encoding]::new($false))
$testMatrix = Join-Path $temp "run-client-versions.json"
foreach ($entry in $matrix) {
    foreach ($dependency in @($entry.curseforge | Where-Object { $_ })) {
        $dependency.compatible.sha512 = $hash
        $dependency.latest.sha512 = $hash
    }
}
[IO.File]::WriteAllText($testMatrix, ($matrix | ConvertTo-Json -Depth 20), [Text.UTF8Encoding]::new($false))

$global:Ae2CtVersions = @{}
$releaseTargets = @((Get-Content -LiteralPath (Join-Path $PSScriptRoot "release-matrix.json") -Raw | ConvertFrom-Json).id)
if (Compare-Object $releaseTargets @($matrix.id)) { throw "Run-client and release target matrices differ" }
foreach ($entry in $matrix) {
    if (@($entry.projects.project_id | Group-Object | Where-Object Count -gt 1).Count) { throw "Duplicate projects in $($entry.id)" }
    if (@($entry.compatible.versions.project_id | Group-Object | Where-Object Count -gt 1).Count) { throw "Duplicate compatible locks in $($entry.id)" }
    foreach ($project in @($entry.projects | Where-Object { $_.compatible -ne $false }).project_id) {
        if ($project -notin $entry.compatible.versions.project_id) { throw "Missing compatible lock for $project in $($entry.id)" }
    }
    foreach ($project in @($entry.test_driver_projects | Where-Object { $_ })) {
        if ($project -notin $entry.compatible.versions.project_id) { throw "Missing test-driver fixture lock for $project in $($entry.id)" }
    }
    foreach ($version in @($entry.compatible.versions)) { $global:Ae2CtVersions[$version.version_id] = @($version.project_id, $version.version) }
    $global:Ae2CtVersions[$entry.compatible.ae2_version_id] = @("XxWD5pD3", $entry.compatible.ae2_version)
    if ($entry.compatible.fabric_api_version_id) { $global:Ae2CtVersions[$entry.compatible.fabric_api_version_id] = @("P7dR8mSH", $entry.compatible.fabric_api_version) }
}

function Invoke-WebRequest {
    param([switch]$UseBasicParsing, [string]$Uri, [string]$OutFile)
    if ($OutFile) {
        [IO.File]::WriteAllBytes($OutFile, $(if ($global:Ae2CtBadDownload) { [byte[]](0) } else { $bytes }))
        return
    }
    $versions = if ($Uri -like "*minecraftforge*") { @("1.20.1-1", "1.20.1-99") } elseif ($Uri -like "*fabric-loader*") { @("0.1.0", "0.99.0") } else { @("21.1.1", "21.1.99", "26.1.2.1", "26.1.2.100") }
    [pscustomobject]@{ Content = "<metadata><versioning><versions>$($versions.ForEach({ "<version>$_</version>" }) -join '')</versions></versioning></metadata>" }
}

function New-TestVersion([string]$project, [string]$version) {
    [pscustomobject]@{
        id = "$project-$version"; version_number = $version
        dependencies = @([pscustomobject]@{
            project_id = $(if ($project -eq "XxWD5pD3" -and $global:Ae2CtAe2Dependency) { "Ck4E7v7R" } else { "XxWD5pD3" })
            version_id = "old-pin"; dependency_type = "required"
        })
        files = @([pscustomobject]@{ filename = "$project.jar"; hashes = [pscustomobject]@{ sha512 = $hash }; url = "https://example.invalid/$project.jar"; primary = $true })
    }
}

function Invoke-RestMethod {
    param([string]$Uri)
    $versionId = [regex]::Match($Uri, "/version/([^/?]+)").Groups[1].Value
    if ($versionId) {
        $record = $global:Ae2CtVersions[$versionId]
        if (-not $record) { throw "Unknown mocked version $versionId" }
        return New-TestVersion $record[0] $record[1]
    }
    $project = [regex]::Match($Uri, "/project/([^/]+)/version").Groups[1].Value
    $decoded = [uri]::UnescapeDataString($Uri)
    $version = if ($project -eq "XxWD5pD3") {
        if ($decoded -like '*26.1.2*') { "26.99.0-beta" } elseif ($decoded -like '*1.21.1*') { "19.99.0" } else { "15.99.0" }
    } elseif ($project -eq "P7dR8mSH") { "0.99.0+1.20.1" } else { "latest" }
    return @(New-TestVersion $project $version)
}

function Assert-Line([string]$text, [string]$expected) {
    if ($expected -notin ($text -split "`r?`n")) { throw "Missing exact line '$expected' in:`n$text" }
}

try {
    foreach ($entry in $matrix) {
        if ($entry.id -eq "1.20.1-forge") {
            $stale = Join-Path $temp "versions\1.20.1-forge\run\resolved-mods\ae2-crafting-time-old-forge-1.20.1-test-driver.jar"
            New-Item -ItemType Directory -Path (Split-Path -Parent $stale) -Force | Out-Null
            Set-Content -LiteralPath $stale -Value "stale"
        }
        $output = (& $script -Target $entry.id -Root $temp -VersionMatrix $testMatrix -ResolveOnly 6>&1 | Out-String)
        Assert-Line $output "profile compatible"
        Assert-Line $output "runtime loader $($entry.compatible.loader_version)"
        Assert-Line $output "runtime ae2 $($entry.compatible.ae2_version)"
        if ($entry.compatible.fabric_api_version) { Assert-Line $output "runtime fabric-api $($entry.compatible.fabric_api_version)" }
        $compatibleProjects = @($entry.projects | Where-Object { $_.compatible -ne $false })
        foreach ($project in $compatibleProjects.project_id) { Assert-Line $output "mod $project.jar" }
        $mods = Join-Path $temp "versions\$($entry.id)\run\$(if ($entry.id -eq '1.20.1-forge') { 'resolved-mods' } else { 'mods' })"
        $manifest = Get-Content -LiteralPath (Join-Path $mods ".ae2-crafting-time-run-mods.json") -Raw | ConvertFrom-Json
        $curseCount = @($entry.curseforge | Where-Object { $_ }).Count
        $driverCount = if ($entry.id -eq "1.20.1-forge") { 1 } else { 0 }
        if ($manifest.Count -ne $compatibleProjects.Count + $curseCount + $driverCount) { throw "Unexpected compatible managed-mod count for $($entry.id)" }
        if ($entry.id -eq "1.20.1-forge") {
            $driverName = "ae2-crafting-time-$modVersion-forge-1.20.1-test-driver.jar"
            if ($driverName -notin $manifest -or -not (Test-Path -LiteralPath (Join-Path $mods $driverName))) { throw "Missing compatible driver" }
            if (Test-Path -LiteralPath $stale) { throw "Stale driver was not removed" }
        } elseif (Get-ChildItem -LiteralPath $mods -Filter "*test-driver.jar" -File -ErrorAction SilentlyContinue) {
            throw "Driver leaked into $($entry.id)"
        }

        $output = (& $script -Target $entry.id -Root $temp -VersionMatrix $testMatrix -Latest -ResolveOnly 6>&1 | Out-String)
        Assert-Line $output "profile latest"
        foreach ($project in $entry.projects.project_id) { Assert-Line $output "mod $project.jar" }
        $latestMods = Join-Path $temp "versions\$($entry.id)\run-latest\$(if ($entry.id -eq '1.20.1-forge') { 'resolved-mods' } else { 'mods' })"
        if (-not (Test-Path -LiteralPath (Join-Path $latestMods ".ae2-crafting-time-run-mods.json"))) { throw "Missing latest manifest for $($entry.id)" }
    }

    $customRuntime = Join-Path $temp "custom-runtime"
    $global:Ae2CtAe2Dependency = $true
    try {
        $focusedOutput = (& $script -Target "1.20.1-forge" -Root $temp -VersionMatrix $testMatrix `
            -RuntimeDirectory $customRuntime -DriverScenario ae2wtlib-terminal -DriverOutputDirectory $temp `
            -DriverWorld ae2ct-00000000000000000000000000000000 -ProjectId pNabrMMw -ResolveOnly 6>&1 | Out-String)
    } finally { $global:Ae2CtAe2Dependency = $false }
    Assert-Line $focusedOutput "focused projects pNabrMMw"
    if (-not (Test-Path -LiteralPath (Join-Path $customRuntime "resolved-mods\ae2-crafting-time-$modVersion-forge-1.20.1-test-driver.jar"))) {
        throw "Custom runtime directory did not receive the driver"
    }
    $focusedManifest = Get-Content -LiteralPath (Join-Path $customRuntime "resolved-mods\.ae2-crafting-time-run-mods.json") -Raw | ConvertFrom-Json
    if (@($focusedManifest).Count -ne 4 -or "pNabrMMw.jar" -notin $focusedManifest -or
            "Ck4E7v7R.jar" -notin $focusedManifest -or "PbNc6qBY.jar" -notin $focusedManifest) {
        throw "Focused profile omitted an AE2 dependency or loaded unrelated projects"
    }

    & $script -Target "1.20.1-forge" -Root $temp -VersionMatrix $testMatrix -RuntimeDirectory $customRuntime `
        -ProjectId 1624558 -ResolveOnly 6>&1 | Out-Null
    $curseManifest = Get-Content -LiteralPath (Join-Path $customRuntime "resolved-mods\.ae2-crafting-time-run-mods.json") -Raw | ConvertFrom-Json
    if (@($curseManifest).Count -ne 3 -or
            "omnisequence-transfinite-1.3.9-forge.jar" -notin $curseManifest -or
            "ProjectE-1.20.1-PE1.0.1.jar" -notin $curseManifest) {
        throw "Focused CurseForge profile omitted an explicit dependency"
    }

    try {
        & $script -Target "1.20.1-forge" -Root $temp -VersionMatrix $testMatrix -ProjectId missing -ResolveOnly 6>&1 | Out-Null
        throw "Expected an unknown focused project failure"
    } catch {
        if ($_.Exception.Message -ne "Unknown project missing for 1.20.1-forge") { throw }
    }
    try {
        & $script -Target "1.20.1-forge" -Root $temp -VersionMatrix $testMatrix -ProjectId ayN3DZKb -ResolveOnly 6>&1 | Out-Null
        throw "Expected an excluded focused project failure"
    } catch {
        if ($_.Exception.Message -ne "Focused project ayN3DZKb is excluded from the compatible profile") { throw }
    }

    $env:AE2CT_DRIVER_BUILD_FAIL = "1"
    try {
        & $script -Target "1.20.1-forge" -Root $temp -VersionMatrix $testMatrix -ResolveOnly 6>&1 | Out-Null
        throw "Expected a test-driver build failure"
    } catch {
        if ($_.Exception.Message -ne "Test-driver build failed") { throw }
    } finally { Remove-Item Env:\AE2CT_DRIVER_BUILD_FAIL -ErrorAction SilentlyContinue }

    $badFile = Join-Path $temp "versions\1.20.1-forge\run\resolved-mods\a1RwDz90.jar"
    Remove-Item -LiteralPath $badFile -Force
    $global:Ae2CtBadDownload = $true
    try {
        & $script -Target "1.20.1-forge" -Root $temp -VersionMatrix $testMatrix -ResolveOnly 6>&1 | Out-Null
        throw "Expected a hash mismatch"
    } catch {
        if ($_.Exception.Message -notlike "Hash mismatch for *") { throw }
    } finally { $global:Ae2CtBadDownload = $false }
    Write-Host "run-client checks passed"
} finally {
    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    $resolvedTemp = [IO.Path]::GetFullPath($temp)
    if ($resolvedTemp.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $resolvedTemp)) { Remove-Item -LiteralPath $resolvedTemp -Recurse -Force }
}
