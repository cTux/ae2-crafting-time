param(
    [Parameter(Mandatory)][string]$Log,
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][ValidateSet('client','dedicated_server')][string]$Environment,
    [switch]$OptionalAbsent
)
$ErrorActionPreference = 'Stop'
$lines = Get-Content -LiteralPath $Log
$context = @($lines | Where-Object { $_ -match 'phase=startup_context ' })
if ($context.Count -ne 1 -or $context[0] -notmatch "target=$([regex]::Escape($Target)) environment=$Environment ") {
    throw 'Expected one context with the actual target and physical environment'
}
$inventory = @($lines | Where-Object { $_ -match 'integration=\S+ .* phase=startup ' })
$ids = @($inventory | ForEach-Object { [regex]::Match($_, 'integration=(\S+)').Groups[1].Value } | Sort-Object -Unique)
if ($inventory.Count -ne 27 -or $ids.Count -ne 27) { throw 'Expected core plus 26 unique integration rows' }
$summary = @($lines | Where-Object { $_ -match 'phase=entrypoint_checks ' })
if ($summary.Count -ne 1) { throw 'Expected one entrypoint summary per process' }
foreach ($capability in @('config-registration','network-registration')) {
    if (-not ($lines -match "integration=ae2craftingtime .*capability=$capability state=confirmed")) {
        throw "Required registration not observed: $capability"
    }
}
if ($Environment -eq 'dedicated_server' -and ($lines -match 'capability=(plan-|status-|key-registration|client-network-registration).*state=confirmed')) {
    throw 'Dedicated startup falsely confirmed a client capability'
}
if ($OptionalAbsent -and $summary[0] -notmatch 'initialized=0 skipped=26 pending=0 partial=0 failed=0') {
    throw 'Absent optional integrations were not all skipped'
}
Write-Output "PASS: $Target $Environment startup diagnostics"
