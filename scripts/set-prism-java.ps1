param([Parameter(Mandatory = $true)][string]$InstanceDirectory)

$ErrorActionPreference = 'Stop'
$instance = Get-Item -LiteralPath $InstanceDirectory
$groups = Get-Content -LiteralPath (Join-Path $instance.Parent.FullName 'instgroups.json') -Raw | ConvertFrom-Json
if ($instance.Name -cnotin @($groups.groups.Codex.instances)) { throw 'Only Prism instances in the Codex group may be configured' }
$pack = Get-Content -LiteralPath (Join-Path $instance.FullName 'mmc-pack.json') -Raw | ConvertFrom-Json
$game = ($pack.components | Where-Object uid -eq 'net.minecraft').version
$major = switch ($game) {
    '1.20.1' { 17 }
    '1.21.1' { 21 }
    '26.1.2' { 25 }
    default { throw "No Java mapping for Minecraft $game" }
}
$javaHome = & (Join-Path $PSScriptRoot 'get-java-home.ps1') -Major $major
$java = (Join-Path $javaHome 'bin\java.exe').Replace('\', '/')
$path = Join-Path $instance.FullName 'instance.cfg'
$text = [IO.File]::ReadAllText($path)
if ($text -notmatch '(?m)^\[General\]\r?$') { throw 'Missing Prism General settings section' }
foreach ($setting in @('OverrideJavaLocation=true', 'AutomaticJava=false', "JavaPath=$java")) {
    $key = $setting.Split('=')[0]
    $pattern = "(?m)^$key=.*$"
    if ($text -match $pattern) { $text = [regex]::Replace($text, $pattern, [Text.RegularExpressions.MatchEvaluator]{ param($match) $setting }) }
    else { $text = $text.Replace('[General]', "[General]`r`n$setting") }
}
[IO.File]::WriteAllText($path, ($text -replace '\r?\n', "`r`n"), [Text.UTF8Encoding]::new($false))
Write-Host "$($instance.Name): JAVA_HOME_$major -> $java"
