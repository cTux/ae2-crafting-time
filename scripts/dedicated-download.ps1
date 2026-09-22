function Assert-DedicatedUri([uri]$Uri) {
    if (!$Uri.IsAbsoluteUri -or $Uri.Scheme -cne 'https' -or $Uri.UserInfo -or !$Uri.IsDefaultPort -or
            $Uri.DnsSafeHost -notin @('maven.minecraftforge.net','maven.neoforged.net','maven.fabricmc.net',
                'meta.fabricmc.net','piston-meta.mojang.com','piston-data.mojang.com','launchermeta.mojang.com',
                'launcher.mojang.com','libraries.minecraft.net')) {
        throw 'Dedicated download origin is not an approved official HTTPS endpoint'
    }
}

function New-DedicatedTransferBudget {
    return @{files=0;bytes=0L;receipts=[Collections.Generic.List[object]]::new()}
}

function New-DedicatedHttpClient {
    Add-Type -AssemblyName System.Net.Http
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false; $handler.UseCookies = $false
    $handler.UseDefaultCredentials = $false; $handler.UseProxy = $false
    $client = [Net.Http.HttpClient]::new($handler)
    $client.Timeout = [Threading.Timeout]::InfiniteTimeSpan
    return $client
}

function Assert-DedicatedTransferBounds([long]$Received, [long]$Total, [long]$Limit) {
    if ($Received -gt $Limit -or $Total -gt 2GB) { throw 'Dedicated streamed byte bound exceeded' }
}

function Receive-DedicatedFile([uri]$Uri, [string]$Destination, $Budget, [long]$Limit = 512MB) {
    Assert-DedicatedUri $Uri
    Assert-DedicatedPath $Destination | Out-Null
    if ($Limit -lt 1 -or $Limit -gt 512MB -or ++$Budget.files -gt 2048) { throw 'Dedicated transfer file bound exceeded' }
    if (Test-Path -LiteralPath $Destination) { throw 'Dedicated download destination already exists' }
    $client = New-DedicatedHttpClient
    try {
        for ($attempt = 1; $attempt -le 2; $attempt++) {
            $temporary = $Destination + '.' + [guid]::NewGuid().ToString('N') + '.partial'
            $clock = [Diagnostics.Stopwatch]::StartNew()
            $received = 0L
            $current = $Uri
            $response = $null
            try {
                for ($redirect = 0; ; $redirect++) {
                    Assert-DedicatedUri $current
                    $connect = [Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(30))
                    try {
                        $response = $client.GetAsync($current,[Net.Http.HttpCompletionOption]::ResponseHeadersRead,$connect.Token).GetAwaiter().GetResult()
                    } finally { $connect.Dispose() }
                    if ([int]$response.StatusCode -notin @(301,302,303,307,308)) { break }
                    if ($redirect -ge 3 -or !$response.Headers.Location) { throw 'Dedicated redirect bound exceeded' }
                    $current = [uri]::new($current,$response.Headers.Location)
                    $response.Dispose(); $response = $null
                }
                $response.EnsureSuccessStatusCode() | Out-Null
                if ($response.Content.Headers.ContentLength -gt $Limit) { throw 'Dedicated response exceeds its byte bound' }
                $transfer = [Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(120))
                try {
                    $input = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
                    $output = [IO.File]::Open($temporary,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
                    try {
                        $buffer = [byte[]]::new(65536)
                        while (($count = $input.ReadAsync($buffer,0,$buffer.Length,$transfer.Token).GetAwaiter().GetResult()) -gt 0) {
                            $received += $count; $Budget.bytes += $count
                            Assert-DedicatedTransferBounds $received $Budget.bytes $Limit
                            $output.Write($buffer,0,$count)
                        }
                    } finally { $output.Dispose(); $input.Dispose() }
                } finally { $transfer.Dispose() }
                Assert-DedicatedPath $temporary | Out-Null
                Move-Item -LiteralPath $temporary -Destination $Destination
                $receipt = [ordered]@{url=$Uri.AbsoluteUri;finalUrl=$current.AbsoluteUri;size=$received
                    sha256=(Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash.ToLowerInvariant()
                    attempt=$attempt;milliseconds=$clock.ElapsedMilliseconds}
                $Budget.receipts.Add($receipt)
                return $receipt
            } catch {
                $Budget.receipts.Add([ordered]@{url=$Uri.AbsoluteUri;attempt=$attempt;receivedBytes=$received
                    milliseconds=$clock.ElapsedMilliseconds;result='FAILED'})
                if ($attempt -eq 2 -or $Budget.bytes -gt 2GB) { throw }
            } finally {
                if ($response) { $response.Dispose() }
                if (Test-Path -LiteralPath $temporary) {
                    Assert-DedicatedPath $temporary | Out-Null
                    Remove-Item -LiteralPath $temporary -Force
                }
            }
        }
    } finally { $client.Dispose() }
}

function Assert-DedicatedDigest([string]$Path, [string]$Algorithm, [string]$Expected) {
    $length = switch ($Algorithm) {'SHA1' {40} 'SHA256' {64} 'SHA512' {128} default {throw 'Unsupported upstream digest'}}
    if ($Expected -notmatch "^[a-fA-F0-9]{$length}$" -or
            (Get-FileHash -LiteralPath $Path -Algorithm $Algorithm).Hash -ine $Expected) {
        throw 'Official dedicated artifact checksum mismatch'
    }
}

function Get-DedicatedInstaller([string]$Url, [string]$CacheRoot, [string]$Report, $Budget) {
    $sidecar = $null
    foreach ($algorithm in @('SHA256','SHA512','SHA1')) {
        $checksumPath = Join-Path $Report ('installer.' + $algorithm.ToLowerInvariant())
        try {
            $sidecar = Receive-DedicatedFile ([uri]($Url + '.' + $algorithm.ToLowerInvariant())) $checksumPath $Budget 10MB
        } catch [Net.Http.HttpRequestException] { continue }
        $digest = ([IO.File]::ReadAllText($checksumPath)).Trim()
        $length = switch ($algorithm) {'SHA256' {64} 'SHA512' {128} 'SHA1' {40}}
        if ($digest -notmatch "^[a-fA-F0-9]{$length}$") { throw 'Official installer checksum is malformed' }
        break
    }
    if (!$sidecar) { throw 'Official installer checksum is unavailable' }
    # Index lookup is only a hint; provenance and both digests are rechecked.
    foreach ($directory in @(Get-ChildItem -LiteralPath $CacheRoot -Directory -ErrorAction SilentlyContinue)) {
        if ($directory.Name -cnotmatch '^[a-f0-9]{64}$') { continue }
        Assert-DedicatedPath $directory.FullName -Tree | Out-Null
        $provenance = Read-DedicatedJson (Join-Path $directory.FullName 'provenance.json') 64KB
        if ($provenance.url -cne $Url) { continue }
        $cached = Join-Path $directory.FullName 'artifact.jar'
        Assert-DedicatedDigest $cached $algorithm $digest
        Assert-DedicatedDigest $cached SHA256 $directory.Name
        if ($Url.Contains('/forge/1.20.1-47.4.23/')) {
            Assert-DedicatedDigest $cached SHA256 '75cfcb11f60cc641dc83757c1357c08c9bdba255ec529feb115c5e89a59cf75a'
        }
        if ($provenance.sha256 -cne $directory.Name -or $provenance.size -ne (Get-Item -LiteralPath $cached).Length) {
            throw 'Installer cache provenance changed; quarantine without deleting it'
        }
        return [ordered]@{path=$cached;provenance=$provenance;cacheHit=$true}
    }
    $download = Join-Path $Report 'installer.jar'
    $receipt = Receive-DedicatedFile ([uri]$Url) $download $Budget
    Assert-DedicatedDigest $download $algorithm $digest
    if ($Url.Contains('/forge/1.20.1-47.4.23/')) {
        Assert-DedicatedDigest $download SHA256 '75cfcb11f60cc641dc83757c1357c08c9bdba255ec529feb115c5e89a59cf75a'
    }
    $receipt.upstreamAlgorithm = $algorithm
    $receipt.upstreamDigest = $digest.ToLowerInvariant()
    $receipt.checksumUrl = $sidecar.url
    $directory = Join-Path $CacheRoot $receipt.sha256
    if (Test-Path -LiteralPath $directory) { throw 'Installer cache destination already exists; quarantine it' }
    $staging = Join-Path $CacheRoot ('.new-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $staging | Out-Null
    try {
        Assert-DedicatedPath $staging -Tree | Out-Null
        Copy-Item -LiteralPath $download -Destination (Join-Path $staging 'artifact.jar')
        Assert-DedicatedDigest (Join-Path $staging 'artifact.jar') SHA256 $receipt.sha256
        $receipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $staging 'provenance.json') -Encoding UTF8
        Move-Item -LiteralPath $staging -Destination $directory
    } finally {
        if (Test-Path -LiteralPath $staging) {
            Assert-DedicatedPath $staging -Tree | Out-Null
            Remove-Item -LiteralPath $staging -Recurse -Force
        }
    }
    return [ordered]@{path=(Join-Path $directory 'artifact.jar');provenance=$receipt;cacheHit=$false}
}
