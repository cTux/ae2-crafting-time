param(
    [Parameter(Mandatory)][string]$ServerDirectory,
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][ValidateSet('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge')][string]$Target,
    [ValidateSet('startup-only','advancedae-cpu','lightningtech-cpu','neoeco-cpu','neoeco-fastpath-cpu')][string]$Scenario = 'startup-only',
    [Parameter(Mandatory)][string]$ResultDirectory
)
$ErrorActionPreference = 'Stop'
$server = [IO.Path]::GetFullPath($ServerDirectory)
$resultRoot = [IO.Path]::GetFullPath($ResultDirectory)
if (-not (Test-Path -LiteralPath $server -PathType Container)) { throw "Missing server: $server" }
$java = Join-Path ([IO.Path]::GetFullPath($JavaHome)) 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "Missing Java: $java" }
New-Item -ItemType Directory -Path $resultRoot -Force | Out-Null
$world = Join-Path $server 'ae2ct-dedicated-smoke'
if (Test-Path -LiteralPath $world) {
    $resolved = [IO.Path]::GetFullPath($world)
    if (-not $resolved.StartsWith($server.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe world path' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
Set-Content -LiteralPath (Join-Path $server 'eula.txt') -Value 'eula=true' -Encoding Ascii
@('level-name=ae2ct-dedicated-smoke','online-mode=false','pause-when-empty-seconds=-1','view-distance=6','simulation-distance=6') |
    Set-Content -LiteralPath (Join-Path $server 'server.properties') -Encoding Ascii
$result = Join-Path $resultRoot "$Target-$Scenario.json"
$stdout = Join-Path $resultRoot "$Target-$Scenario.stdout.log"
$stderr = Join-Path $resultRoot "$Target-$Scenario.stderr.log"
Remove-Item -LiteralPath $result,$stdout,$stderr -Force -ErrorAction SilentlyContinue
$properties = "-Dae2ct.testDriver.serverScenario=$Scenario -Dae2ct.testDriver.serverTarget=$Target `"-Dae2ct.testDriver.serverResult=$result`""
if ($Target -eq '1.20.1-fabric') { $arguments = "$properties -Xmx3G -jar fabric-server-launch.jar nogui" }
else {
    $loader = if ($Target -eq '1.20.1-forge') { 'net/minecraftforge/forge/1.20.1-47.4.10' }
        elseif ($Target -eq '1.21.1-neoforge') { 'net/neoforged/neoforge/21.1.238' }
        else { 'net/neoforged/neoforge/26.1.2.99' }
    $arguments = "$properties -Xmx4G @libraries/$loader/win_args.txt nogui"
}
$start = [Diagnostics.ProcessStartInfo]::new($java, $arguments)
$start.WorkingDirectory = $server
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.WindowStyle = [Diagnostics.ProcessWindowStyle]::Hidden
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
$process = [Diagnostics.Process]::new(); $process.StartInfo = $start
if (-not $process.Start()) { throw 'Dedicated server did not start' }
$outTask = $process.StandardOutput.ReadToEndAsync(); $errTask = $process.StandardError.ReadToEndAsync()
try {
    if (-not $process.WaitForExit(420000)) { $process.Kill(); $process.WaitForExit(); throw "Dedicated smoke timed out (PID $($process.Id))" }
    if (-not (Test-Path -LiteralPath $result)) { throw "Dedicated smoke produced no result (PID $($process.Id), exit $($process.ExitCode))" }
    $data = Get-Content -LiteralPath $result -Raw | ConvertFrom-Json
    if ($data.result -ne 'PASS') { throw "Dedicated smoke failed: $($data.error)" }
    if ($process.ExitCode -ne 0) { throw "Dedicated smoke exited with $($process.ExitCode) after its result" }
    Write-Host "Dedicated smoke passed: $Target $Scenario PID $($process.Id)"
} finally {
    $outTask.Result | Set-Content -LiteralPath $stdout -Encoding UTF8
    $errTask.Result | Set-Content -LiteralPath $stderr -Encoding UTF8
    [ordered]@{ pid=$process.Id; startedAt=$process.StartTime.ToUniversalTime().ToString('o')
        finishedAt=[DateTime]::UtcNow.ToString('o'); exitCode=$process.ExitCode } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $resultRoot "$Target-$Scenario.process.json") -Encoding UTF8
    $process.Dispose()
}
