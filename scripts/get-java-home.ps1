param([Parameter(Mandatory = $true)][ValidateSet(17, 21, 25)][int]$Major)

$ErrorActionPreference = 'Stop'
$name = "JAVA_HOME_$Major"
$candidate = [Environment]::GetEnvironmentVariable($name)
if (-not $candidate) {
    $candidate = (Get-ItemProperty -LiteralPath 'HKCU:\Environment' -ErrorAction SilentlyContinue).$name
}
if (-not $candidate) {
    $candidate = (Get-ItemProperty -LiteralPath 'HKLM:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment' -ErrorAction SilentlyContinue).$name
}
if (-not $candidate) { throw "Set $name to the installed JDK $Major directory" }
$java = Join-Path $candidate 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "$name has no bin\java.exe: $candidate" }
$version = (& { $ErrorActionPreference = 'Continue'; & $java -XshowSettings:properties -version 2>&1 } | Out-String)
if ($LASTEXITCODE -ne 0 -or $version -notmatch "(?m)^\s*java\.version\s*=\s*$Major(?:\.|\s|$)") {
    throw "$name requires JDK ${Major}: $java"
}
[IO.Path]::GetFullPath($candidate)
