. (Join-Path $PSScriptRoot 'dedicated-source-contract.ps1')
function Get-ResourcePrewarmWatchdog([long]$NowMillis, [long]$DeadlineMillis, [bool]$Activated,
        [long]$CallbackSequence, [double]$CallbackAgeSeconds) {
    if ($CallbackSequence -gt 0 -and $CallbackAgeSeconds -gt 20) { return 'no-callback' }
    if (!$Activated -and $NowMillis -ge $DeadlineMillis) { return 'prewarm-timeout' }
    return $null
}
function Assert-ResourcePrewarmReceipt($Receipt, [string]$Epoch, [string]$Head, [string]$Bundle,
        [int]$ProcessId, [DateTime]$StartedAt, [bool]$Server, [bool]$Attempt = $false) {
    $keys = @('schema','epoch','head','bundle','pid','startTime','player','generation','serverTicks','worldFrames')
    if (@(Compare-Object @($Receipt.PSObject.Properties.Name | Sort-Object) @($keys | Sort-Object)).Count -or
            $Receipt.schema -ne 1 -or $Receipt.epoch -cne ([guid]$Epoch).ToString() -or $Receipt.head -cne $Head -or
            $Receipt.bundle -cne $Bundle.ToLowerInvariant() -or $Receipt.pid -ne $ProcessId -or
            $Receipt.player -cne '446b6d0c-cadd-3e57-baf6-99d70f01a628' -or
            [int]$Receipt.generation -lt 1 -or [int]$Receipt.generation -gt 3 -or
            ([DateTimeOffset]::Parse($Receipt.startTime)).ToUnixTimeMilliseconds() -ne ([DateTimeOffset]$StartedAt).ToUnixTimeMilliseconds() -or
            [int]$Receipt.serverTicks -ne $(if($Server){20}else{0}) -or [int]$Receipt.worldFrames -ne $(if($Server -or $Attempt){0}else{40})) {
        throw 'Prewarm readiness identity, process or native counters mismatch'
    }
}

function Update-ResourcePrewarm([string]$Control, [string]$Epoch, [string]$Head, [string]$Bundle,
        [int]$ServerProcessId, [DateTime]$ServerStartedAt, [int]$ClientProcessId, [DateTime]$ClientStartedAt) {
    if ($ServerProcessId -eq $ClientProcessId) { throw 'Prewarm requires distinct client and server processes' }
    $directory = Join-Path $Control 'prewarm'
    Assert-DedicatedPath $directory -Tree | Out-Null
    $serverPath = Join-Path $directory 'server-ready.json'
    $clientPath = Join-Path $directory 'client-ready.json'
    $armPath = Join-Path $directory 'arm.json'
    $armedPath = Join-Path $directory 'armed.json'
    if (!(Test-Path -LiteralPath $serverPath) -or !(Test-Path -LiteralPath $clientPath)) { return $false }
    $server = Read-DedicatedJson $serverPath 64KB
    $client = Read-DedicatedJson $clientPath 64KB
    Assert-ResourcePrewarmReceipt $server $Epoch $Head $Bundle $ServerProcessId $ServerStartedAt $true
    Assert-ResourcePrewarmReceipt $client $Epoch $Head $Bundle $ClientProcessId $ClientStartedAt $false
    $attempt = Read-DedicatedJson (Join-Path $directory 'attempt.json') 64KB
    Assert-ResourcePrewarmReceipt $attempt $Epoch $Head $Bundle $ClientProcessId $ClientStartedAt $false $true
    if ($attempt.generation -cne $client.generation) { throw 'Prewarm readiness does not match the native attempt' }
    if ($server.generation -cne $client.generation) { throw 'Prewarm peers name different native connections' }
    foreach ($identity in @(@{pid=$ServerProcessId;started=$ServerStartedAt},@{pid=$ClientProcessId;started=$ClientStartedAt})) {
        $live = Get-Process -Id $identity.pid -ErrorAction Stop
        if ($live.StartTime.ToUniversalTime() -ne $identity.started.ToUniversalTime()) { throw 'Prewarm process identity is stale' }
    }
    $expected = [ordered]@{schema='1';epoch=([guid]$Epoch).ToString();head=$Head;bundle=$Bundle.ToLowerInvariant()
        player='446b6d0c-cadd-3e57-baf6-99d70f01a628';generation=[string]$server.generation
        serverReadySha256=(Get-FileHash -LiteralPath $serverPath).Hash.ToLowerInvariant()
        clientReadySha256=(Get-FileHash -LiteralPath $clientPath).Hash.ToLowerInvariant()}
    if (!(Test-Path -LiteralPath $armPath)) {
        if ((Test-Path -LiteralPath (Join-Path $Control 'resource/command.properties')) -or
                (Test-Path -LiteralPath (Join-Path $Control 'resource/state.properties'))) { throw 'Fixture mutated before prewarm activation' }
        $temporary = Join-Path $directory ([guid]::NewGuid().ToString('N') + '.tmp')
        try {
            [IO.File]::WriteAllText($temporary,($expected | ConvertTo-Json -Compress),[Text.UTF8Encoding]::new($false))
            [IO.File]::Move($temporary,$armPath)
        } finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
    }
    foreach ($path in @($armPath) + $(if(Test-Path -LiteralPath $armedPath){@($armedPath)}else{@()})) {
        $actual = Read-DedicatedJson $path 64KB
        if (@($actual.PSObject.Properties).Count -ne $expected.Count) { throw 'Unexpected prewarm activation fields' }
        foreach ($key in $expected.Keys) { if ($actual.$key -cne $expected[$key]) { throw 'Changed or stale prewarm activation' } }
    }
    return (Test-Path -LiteralPath $armedPath)
}
