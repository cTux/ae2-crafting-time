$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$runtimes = @(
    'shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/TestDriverRuntime.java',
    'versions/26.1.2-neoforge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/TestDriverRuntime.java'
)
$platforms = @(
    'versions/1.20.1-forge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/DriverPlatform.java',
    'versions/1.20.1-fabric/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/DriverPlatform.java',
    'versions/1.21.1-neoforge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/DriverPlatform.java',
    'versions/26.1.2-neoforge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/DriverPlatform.java'
)

foreach ($relative in $runtimes) {
    $text = Get-Content -LiteralPath (Join-Path $root $relative) -Raw
    if ($text -notmatch 'initialDedicatedParent = DriverPlatform\.prepareInitialDedicatedConnect\(minecraft\)' -or
        $text -notmatch 'minecraft\.screen == initialDedicatedParent' -or
        $text -notmatch 'connectServer\(minecraft, initialDedicatedParent,') {
        throw "Initial dedicated connection does not wait for the prepared multiplayer screen: $relative"
    }
}

foreach ($relative in $platforms) {
    $text = Get-Content -LiteralPath (Join-Path $root $relative) -Raw
    if ($text -notmatch 'new net\.minecraft\.client\.gui\.screens\.multiplayer\.JoinMultiplayerScreen\(' -or
        $text -notmatch 'minecraft\.setScreen\(multiplayer\)' -or
        $text -notmatch 'ConnectScreen\.startConnecting\(parent,') {
        throw "Platform does not initialize and retain the multiplayer screen before connecting: $relative"
    }
}

Write-Host 'Connected dedicated driver readiness contract passed'
