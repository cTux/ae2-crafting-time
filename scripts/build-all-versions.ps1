param(
    [string]$JavaHome = "C:\Users\cccTu\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.19+10"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$versionsDir = Join-Path $root "versions"

if (Test-Path $JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
}

$tasks = @()
foreach ($version in Get-ChildItem -Path $versionsDir -Directory | Sort-Object Name) {
    $module = "mc_" + ($version.Name -replace "\.", "_")
    $tasks += ":$($module):distMod"
}

if (-not $tasks) {
    throw "No version directories found in $versionsDir"
}

$gradlew = Join-Path $root "gradlew.bat"
Push-Location $root
try {
    & $gradlew @tasks
}
finally {
    Pop-Location
}
