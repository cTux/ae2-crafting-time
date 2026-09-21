$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$configs = @(
    'shared/src/mc1201/resources/ae2craftingtime.mixins.json',
    'versions/1.21.1-neoforge/src/main/resources/ae2craftingtime.mixins.json',
    'versions/26.1.2-neoforge/src/main/resources/ae2craftingtime.mixins.json')
foreach ($relative in $configs) {
    $config = Get-Content -LiteralPath (Join-Path $root $relative) -Raw | ConvertFrom-Json
    foreach ($mixin in @('StorageServiceAccessor', 'CraftingPlanSummaryEntryMixin', 'CraftConfirmTableRendererMixin')) {
        if ($mixin -notin @($config.mixins) -and $mixin -notin @($config.client)) {
            throw "$relative does not register $mixin"
        }
    }
}
foreach ($target in @('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge')) {
    $path = Join-Path $root "versions/$target/src/main/java/com/ctux/ae2craftingtime/mc1201/StatsNetwork.java"
    $source = Get-Content -LiteralPath $path -Raw
    if ($source -notmatch 'PlanStoredVariantsS2C' -or $source -notmatch 'sendTo\(ServerPlayer player, PlanStoredVariantsS2C') {
        throw "$target does not register or send stored-variant packets"
    }
}
Write-Host 'Stored-variant mixin and packet registration is present on all four targets.'

foreach ($relative in @('shared/src/testDriver1201/resources/ae2craftingtime_test_driver.mixins.json',
        'versions/26.1.2-neoforge/src/testDriver/resources/ae2craftingtime_test_driver.mixins.json')) {
    $driverConfig = Get-Content (Join-Path $root $relative) -Raw | ConvertFrom-Json
    if ('StoredVariantServerObservationMixin' -notin $driverConfig.mixins -or
            'StoredVariantPacketObservationMixin' -notin $driverConfig.client -or
            'RecurrentNativePlanMixin' -notin $driverConfig.client) {
        throw "$relative omits a stored-variant observation boundary"
    }
}

# Execute the runner's real contract expressions without launching a client.
$runnerPath = Join-Path $PSScriptRoot 'run-ui-smoke.ps1'
$runnerAst = [System.Management.Automation.Language.Parser]::ParseFile($runnerPath, [ref]$null, [ref]$null)
$contracts = (Get-Content (Join-Path $PSScriptRoot 'ui-smoke-groups.json') -Raw | ConvertFrom-Json).cases
$standardContracts = $contracts
$result = [pscustomobject]@{ checks = [pscustomobject]@{} }
foreach ($caseScenario in @('stored-variant-plan', 'standard-plan-controls')) {
    foreach ($DedicatedAddress in @('', '127.0.0.1:25565')) {
        foreach ($variable in @('$requiredChecks', '$requiredScreenshots')) {
            $assignment = $runnerAst.FindAll({
                param($node)
                $node -is [System.Management.Automation.Language.AssignmentStatementAst] -and
                    $node.Left.Extent.Text -ceq $variable
            }, $true) | Select-Object -First 1
            if (!$assignment) { throw "Missing runner contract expression: $variable" }
            Invoke-Expression $assignment.Extent.Text
        }
        $extra = $caseScenario -eq 'stored-variant-plan' -and !!$DedicatedAddress
        if (('menu-cancel' -in $requiredChecks) -ne $extra -or
                ('stored-variant-network-switch.png' -in $requiredScreenshots) -ne $extra) {
            throw 'Connected stored-variant lifecycle evidence is not enforced by the runner'
        }
        if ($caseScenario -eq 'stored-variant-plan' -and
                @('notification-lifecycle','watcher-cleanup','coexistence','fluid-clear','ordinary-clear' |
                    Where-Object { $_ -notin $requiredChecks }).Count) {
            throw 'Stored-variant result omitted mandatory controls'
        }
    }
}
