param(
    [string]$JavaHome = "C:\Users\cccTu\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.19+10",
    [string]$MatrixPath = (Join-Path $PSScriptRoot "release-matrix.json")
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

if (Test-Path $JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
}

$matrix = @(Get-Content $MatrixPath -Raw | ConvertFrom-Json)
$tasks = $matrix | ForEach-Object { ":$($_.module):distMod" }

if (-not $tasks) {
    throw "No release entries found in $MatrixPath"
}

$gradlew = Join-Path $root "gradlew.bat"
Push-Location $root
try {
    & $gradlew $tasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed"
    }
}
finally {
    Pop-Location
}
