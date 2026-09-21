$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'dedicated-source-contract.ps1')
. (Join-Path $PSScriptRoot 'dedicated-download.ps1')
Add-Type -AssemblyName System.Net.Http
function Assert($value,[string]$message){if(!$value){throw $message}}
function Refuses([scriptblock]$action,[string]$message){$failed=$false;try{& $action|Out-Null}catch{$failed=$true};Assert $failed $message}
$script:responses=[Collections.Generic.Queue[object]]::new()
$script:requests=0
function New-DedicatedHttpClient {
    $fake=[pscustomobject]@{}
    $fake | Add-Member ScriptMethod GetAsync {
        param($uri,$completion,$token)
        $script:requests++
        if (!$script:responses.Count) { throw 'Unexpected mocked HTTP request' }
        return [Threading.Tasks.Task[Net.Http.HttpResponseMessage]]::FromResult($script:responses.Dequeue())
    }
    $fake | Add-Member ScriptMethod Dispose {}
    return $fake
}
function Response([int]$Status=200,[string]$Body='artifact',[string]$Location='',[long]$Length=-1){
    $response=[Net.Http.HttpResponseMessage]::new([Net.HttpStatusCode]$Status)
    $response.Content=[Net.Http.ByteArrayContent]::new([Text.Encoding]::UTF8.GetBytes($Body))
    if($Location){$response.Headers.Location=[uri]$Location}
    if($Length -ge 0){$response.Content.Headers.ContentLength=$Length}
    $script:responses.Enqueue($response)
}
$temporary=Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-download-test-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporary|Out-Null
try{
    $budget=New-DedicatedTransferBudget
    Response
    $receipt=Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'success') $budget
    Assert ($receipt.size -eq 8 -and $budget.bytes -eq 8 -and $receipt.attempt -eq 1) 'Successful stream accounting failed'
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'success') $budget } 'Existing download overwritten'
    Response 503;Response
    $receipt=Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'retry') $budget
    Assert ($receipt.attempt -eq 2) 'Transfer did not use bounded retry'
    Response 302 '' '/redirected';Response
    $receipt=Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'redirect') $budget
    Assert ($receipt.finalUrl -eq 'https://maven.fabricmc.net/redirected') 'Relative redirect identity missing'
    foreach($attempt in 1..2){Response 302 '' 'https://evil.invalid/artifact'}
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'evil') $budget } 'Unapproved redirect followed'
    foreach($attempt in 1..2){foreach($redirect in 1..4){Response 302 '' '/loop'}}
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'loop') $budget } 'Redirect count unbounded'
    foreach($attempt in 1..2){Response 200 'oversize' '' 100}
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'header-limit') $budget 4 } 'Header byte cap ignored'
    foreach($attempt in 1..2){Response 200 'oversize' '' 1}
    $before=$budget.bytes
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'stream-limit') $budget 4 } 'Stream byte cap ignored'
    Assert ($budget.bytes -eq $before+16) 'Retry stream bytes were not counted'
    $aggregate=New-DedicatedTransferBudget;$aggregate.bytes=2GB
    Response
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'aggregate') $aggregate } 'Aggregate cap ignored'
    Assert ($aggregate.bytes -eq 2GB+8) 'Aggregate attempted bytes missing'
    $count=New-DedicatedTransferBudget;$count.files=2048
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'count') $count } 'Request count cap ignored'
    Refuses { Receive-DedicatedFile 'https://maven.fabricmc.net/artifact' (Join-Path $temporary 'limit') $budget 0 } 'Zero bound accepted'
    Assert (@(Get-ChildItem -LiteralPath $temporary -Filter '*.partial').Count -eq 0) 'Incomplete download survived as a cache candidate'
    Assert ($script:responses.Count -eq 0) 'Mock HTTP requests did not match the expected bounded paths'
    $cache=Join-Path $temporary 'cache';New-Item -ItemType Directory -Path $cache|Out-Null
    $url='https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.2/fabric-installer-1.1.2.jar'
    $digest=Get-DedicatedSha256 'artifact'
    foreach($case in @('cold','hit','corrupt','missing','malformed')){
        $report=Join-Path $temporary $case;New-Item -ItemType Directory -Path $report|Out-Null
        if($case -eq 'missing'){
            foreach($request in 1..6){Response 404}
            Refuses { Get-DedicatedInstaller $url $cache $report (New-DedicatedTransferBudget) } 'Missing upstream checksums accepted'
        }elseif($case -eq 'malformed'){
            Response 200 'bad'
            Refuses { Get-DedicatedInstaller $url $cache $report (New-DedicatedTransferBudget) } 'Malformed upstream checksum accepted'
        }else{
            Response 200 $digest
            if($case -eq 'cold'){Response}
            if($case -eq 'corrupt'){
                [IO.File]::AppendAllText((Join-Path $cache "$digest/artifact.jar"),'corrupt')
                Refuses { Get-DedicatedInstaller $url $cache $report (New-DedicatedTransferBudget) } 'Corrupt cache accepted'
                Assert ((Get-Item (Join-Path $cache "$digest/artifact.jar")).Length -eq 15) 'Corrupt cache was overwritten or deleted'
            }else{
                $installer=Get-DedicatedInstaller $url $cache $report (New-DedicatedTransferBudget)
                Assert ($installer.cacheHit -eq ($case -eq 'hit')) 'Installer cache hit classification is wrong'
                Assert ($installer.provenance.upstreamAlgorithm -eq 'SHA256' -and $installer.provenance.sha256 -eq $digest) 'Official provenance missing'
            }
        }
    }
    Assert ($script:responses.Count -eq 0) 'Installer checksum requests were not bounded'
    Write-Host 'dedicated download contracts passed'
}finally{
    $resolved=[IO.Path]::GetFullPath($temporary)
    if(!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()),[StringComparison]::OrdinalIgnoreCase) -or
            [IO.Path]::GetFileName($resolved) -notlike 'ae2ct-download-test-*'){throw 'Unsafe test cleanup'}
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
