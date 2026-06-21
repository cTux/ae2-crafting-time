param(
    [switch]$Deploy,
    [switch]$DryRun,
    [string]$JavaHome = "C:\Users\cccTu\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.19+10",
    [string]$MatrixPath = (Join-Path $PSScriptRoot "release-matrix.json"),
    [string]$StatePath = (Join-Path (Split-Path -Parent $PSScriptRoot) ".release-state.json")
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $root "gradlew.bat"

if (Test-Path $JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
}

function Read-Json($path, $fallback) {
    if (Test-Path $path) {
        return Get-Content $path -Raw | ConvertFrom-Json
    }
    return $fallback
}

function Write-Json($path, $value) {
    $value | ConvertTo-Json -Depth 8 | Set-Content -Path $path -Encoding UTF8
}

function Assert-ReleaseEntry($entry) {
    $required = @("id", "module", "loader", "loaderName", "minecraftVersion", "projectDir", "artifactBase", "initialVersion", "releaseType")
    foreach ($name in $required) {
        if (-not $entry.$name) {
            throw "Release entry is missing '$name'"
        }
    }
    if ($entry.initialVersion -notmatch '^\d+\.\d+\.\d+$') {
        throw "$($entry.id) initialVersion must be x.y.z"
    }
    if ($entry.releaseType -notin @("alpha", "beta", "release")) {
        throw "$($entry.id) releaseType must be alpha, beta, or release"
    }
}

function Next-PatchVersion([string]$version) {
    if ($version -notmatch '^(\d+)\.(\d+)\.(\d+)$') {
        throw "Version '$version' is not x.y.z"
    }
    return "$($Matches[1]).$($Matches[2]).$([int]$Matches[3] + 1)"
}

function Get-InputFingerprint($entry) {
    $paths = @(
        "build.gradle",
        "settings.gradle",
        "shared/src/main",
        "shared/build.gradle",
        "$($entry.projectDir)/build.gradle",
        "$($entry.projectDir)/src/main"
    )

    $hashes = foreach ($path in $paths) {
        $fullPath = Join-Path $root $path
        if (-not (Test-Path $fullPath)) {
            throw "Missing release input path: $path"
        }

        Get-ChildItem -Path $fullPath -Recurse -File |
            Where-Object { $_.FullName -notmatch '\\(build|run|bin|logs)\\' } |
            Sort-Object FullName |
            ForEach-Object {
                $relative = $_.FullName.Substring($root.Length + 1).Replace('\', '/')
                $fileHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash
                "$relative $fileHash"
            }
    }

    $bytes = [Text.Encoding]::UTF8.GetBytes(($hashes -join "`n"))
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash($bytes)) -replace '-', '').ToLowerInvariant()
    }
    finally {
        $sha.Dispose()
    }
}

function Get-StateEntry($state, [string]$id) {
    if ($state.PSObject.Properties.Name -contains $id) {
        return $state.$id
    }
    return $null
}

function Set-StateEntry($state, [string]$id, $value) {
    if ($state.PSObject.Properties.Name -contains $id) {
        $state.$id = $value
    }
    else {
        $state | Add-Member -NotePropertyName $id -NotePropertyValue $value
    }
}

function Invoke-Curl($arguments) {
    $output = & curl.exe @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "curl failed with exit code $LASTEXITCODE"
    }
    return $output
}

function Publish-Modrinth($entry, [string]$version, [string]$jarPath) {
    if (-not $entry.modrinthProjectId) { return }
    if (-not $env:MODRINTH_TOKEN) { throw "MODRINTH_TOKEN is required for Modrinth upload" }

    $dataPath = New-TemporaryFile
    try {
        @{
            name = "$($entry.artifactBase)-$version"
            version_number = $version
            changelog = $entry.changelog
            dependencies = @()
            game_versions = @($entry.minecraftVersion)
            version_type = $entry.releaseType
            loaders = @($entry.loader)
            featured = $false
            status = "listed"
            requested_status = "listed"
            project_id = $entry.modrinthProjectId
            file_parts = @("file")
            primary_file = "file"
        } | ConvertTo-Json -Depth 6 | Set-Content -Path $dataPath -Encoding UTF8

        Invoke-Curl @(
            "-sS", "-f",
            "-H", "Authorization: $env:MODRINTH_TOKEN",
            "-H", "User-Agent: ctux/ae2-crafting-time-release-script",
            "-F", "data=<$dataPath;type=application/json",
            "-F", "file=@$jarPath;type=application/java-archive",
            "https://api.modrinth.com/v2/version"
        ) | Out-Null
    }
    finally {
        Remove-Item -LiteralPath $dataPath -Force -ErrorAction SilentlyContinue
    }
}

function Publish-CurseForge($entry, [string]$version, [string]$jarPath) {
    if (-not $entry.curseProjectId) { return }
    if (-not $env:CURSEFORGE_TOKEN) { throw "CURSEFORGE_TOKEN is required for CurseForge upload" }

    $metadataPath = New-TemporaryFile
    try {
        @{
            changelog = $entry.changelog
            changelogType = "text"
            displayName = "$($entry.artifactBase)-$version"
            gameVersionNames = @($entry.minecraftVersion, $entry.loaderName)
            releaseType = $entry.releaseType
            isMarkedForManualRelease = $false
        } | ConvertTo-Json -Depth 6 | Set-Content -Path $metadataPath -Encoding UTF8

        Invoke-Curl @(
            "-sS", "-f",
            "-H", "X-Api-Token: $env:CURSEFORGE_TOKEN",
            "-F", "metadata=<$metadataPath;type=application/json",
            "-F", "file=@$jarPath;type=application/java-archive",
            "https://minecraft.curseforge.com/api/projects/$($entry.curseProjectId)/upload-file"
        ) | Out-Null
    }
    finally {
        Remove-Item -LiteralPath $metadataPath -Force -ErrorAction SilentlyContinue
    }
}

$matrix = @(Read-Json $MatrixPath @())
$state = Read-Json $StatePath ([pscustomobject]@{})

Push-Location $root
try {
    foreach ($entry in $matrix) {
        Assert-ReleaseEntry $entry
        $fingerprint = Get-InputFingerprint $entry
        $previous = Get-StateEntry $state $entry.id
        $currentVersion = if ($previous -and $previous.version) { $previous.version } else { $entry.initialVersion }

        if ($previous -and $previous.fingerprint -eq $fingerprint) {
            Write-Host "skip $($entry.id): unchanged at $currentVersion"
            continue
        }

        $nextVersion = if ($previous) { Next-PatchVersion $currentVersion } else { $currentVersion }
        $task = ":$($entry.module):distMod"
        $jarPath = Join-Path $root "dist\$($entry.artifactBase)-$nextVersion.jar"

        Write-Host "build $($entry.id): $nextVersion"
        if (-not $DryRun) {
            & $gradlew $task "-PmodVersion=$nextVersion"
            if ($LASTEXITCODE -ne 0) {
                throw "Gradle failed for $($entry.id)"
            }
            if (-not (Test-Path $jarPath)) {
                throw "Expected jar was not created: $jarPath"
            }
        }

        if ($Deploy) {
            if ($DryRun) {
                Write-Host "dry-run deploy $($entry.id): $jarPath"
            }
            else {
                Publish-Modrinth $entry $nextVersion $jarPath
                Publish-CurseForge $entry $nextVersion $jarPath
            }
        }

        if (-not $DryRun) {
            Set-StateEntry $state $entry.id ([pscustomobject]@{
                version = $nextVersion
                fingerprint = $fingerprint
                jar = $jarPath.Substring($root.Length + 1).Replace('\', '/')
                updatedAt = (Get-Date).ToUniversalTime().ToString("o")
            })
            Write-Json $StatePath $state
        }
    }
}
finally {
    Pop-Location
}
