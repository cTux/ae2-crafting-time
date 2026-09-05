param(
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$Profile,
    [Parameter(Mandatory)][string[]]$Scenarios,
    [Parameter(Mandatory)][string]$Evidence,
    [string]$ExpectedAdapters
)
$ErrorActionPreference = 'Stop'
$catalogue = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'ui-smoke-groups.json') -Raw | ConvertFrom-Json
$contracts = $catalogue.cases
foreach ($scenario in $Scenarios) {
    $directory = if ($Scenarios.Count -eq 1) { $Evidence } else { Join-Path $Evidence $scenario }
    $file = Join-Path $directory 'result.json'
    $result = 'NOT_RUN'
    $reason = 'No result from this invocation'
    if (Test-Path -LiteralPath $file -PathType Leaf) {
        try {
            $data = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
            if ($data.schema -ne 1 -or $data.complete -isnot [bool] -or !$data.complete -or $data.target -cne $Target -or $data.profile -cne $Profile -or
                    $data.scenario -cne $scenario -or $data.language -cne 'en_us' -or $data.result -cne 'PASS') { throw 'Failed or mismatched result' }
            if ($ExpectedAdapters) {
                $expected = Get-Content -LiteralPath $ExpectedAdapters -Raw | ConvertFrom-Json
                foreach ($adapter in @($catalogue.adapterCases.psobject.Properties) + @($catalogue.readRecoveryCases.psobject.Properties)) {
                    $dependency = $adapter.Name
                    if ($scenario -cin $adapter.Value -and $expected.$dependency -and
                            ($data.adapters.$dependency.variant -cne $expected.$dependency -or $data.adapters.$dependency.reason -cne 'selected')) {
                        throw "Newest adapter not exercised: $dependency requires $($expected.$dependency)"
                    }
                }
            }
            if ($contracts.$scenario) {
                if (Compare-Object @($contracts.$scenario.checks) @($data.checks.psobject.Properties.Name) -CaseSensitive) { throw 'Incomplete check set' }
                foreach ($check in $contracts.$scenario.checks) { if ($data.checks.$check -isnot [bool] -or !$data.checks.$check) { throw "Failed check: $check" } }
                foreach ($image in $contracts.$scenario.screenshots) {
                    if ($image -cnotin $data.screenshots -or !(Test-Path -LiteralPath (Join-Path $directory $image) -PathType Leaf) -or
                            !(Test-Path -LiteralPath (Join-Path $directory ($image.Replace('.png','.json'))) -PathType Leaf)) { throw "Missing evidence: $image" }
                    $snapshot = Get-Content -LiteralPath (Join-Path $directory ($image.Replace('.png','.json'))) -Raw | ConvertFrom-Json
                    if (!$snapshot.screen -or !$snapshot.gui) { throw "Invalid snapshot: $image" }
                }
            }
            $result = 'PASS'; $reason = ''
        } catch { $result = 'FAIL'; $reason = $_.Exception.Message }
    }
    [pscustomobject]@{ scenario=$scenario; result=$result; reason=$reason; evidence=$directory }
}
