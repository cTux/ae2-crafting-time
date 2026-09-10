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
$mixinConfigs = @(
    'shared/src/testDriver1201/resources/ae2craftingtime_test_driver.mixins.json',
    'versions/1.21.1-neoforge/src/testDriver/resources/ae2craftingtime_test_driver.mixins.json',
    'versions/26.1.2-neoforge/src/testDriver/resources/ae2craftingtime_test_driver.mixins.json'
)
$accessor = 'shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/mixin/JoinMultiplayerScreenAccessor.java'

foreach ($relative in $runtimes) {
    $text = Get-Content -LiteralPath (Join-Path $root $relative) -Raw
    if ($text -notmatch 'initialDedicatedParent = DriverPlatform\.prepareInitialDedicatedConnect\(minecraft\)' -or
        $text -notmatch 'minecraft\.screen == initialDedicatedParent' -or
        $text -notmatch 'connectInitialDedicatedServer\(initialDedicatedParent,') {
        throw "Initial dedicated connection does not use the prepared multiplayer screen callback: $relative"
    }
}

foreach ($relative in $platforms) {
    $text = Get-Content -LiteralPath (Join-Path $root $relative) -Raw
    if ($text -notmatch 'new net\.minecraft\.client\.gui\.screens\.multiplayer\.JoinMultiplayerScreen\(' -or
        $text -notmatch 'minecraft\.setScreen\(multiplayer\)' -or
        $text -notmatch 'JoinMultiplayerScreenAccessor\) multiplayer' -or
        $text -notmatch 'ae2craftingtime_test_driver\$setEditingServer\(server\)' -or
        $text -notmatch 'ae2craftingtime_test_driver\$directJoinCallback\(true\)') {
        throw "Platform does not invoke the initialized multiplayer screen direct-join callback: $relative"
    }
}

$accessorText = Get-Content -LiteralPath (Join-Path $root $accessor) -Raw
if ($accessorText -notmatch '@Accessor\("editingServer"\)' -or
    $accessorText -notmatch '@Invoker\("directJoinCallback"\)') {
    throw 'Multiplayer screen accessor does not expose the direct-join callback contract'
}

foreach ($relative in $mixinConfigs) {
    $config = Get-Content -LiteralPath (Join-Path $root $relative) -Raw | ConvertFrom-Json
    if ($config.client -notcontains 'JoinMultiplayerScreenAccessor') {
        throw "Test-driver mixin config does not register the multiplayer screen accessor: $relative"
    }
}

Write-Host 'Connected dedicated driver direct-join callback contract passed'
