param(
    [Parameter(Mandatory)][ValidateSet('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge')][string]$Target,
    [Parameter(Mandatory)][string]$ServerDirectory,
    [Parameter(Mandatory)][string]$PreparedLaunch,
    [Parameter(Mandatory)][string]$BundleDirectory,
    [Parameter(Mandatory)][string]$ReportDirectory,
    [string]$Address = '127.0.0.1:25565',
    [string]$JavaHome,
    [switch]$PlanOnly
)
$ErrorActionPreference = 'Stop'
$sourceServer = [IO.Path]::GetFullPath($ServerDirectory)
$bundle = [IO.Path]::GetFullPath($BundleDirectory)
$prepared = [IO.Path]::GetFullPath($PreparedLaunch)
$report = [IO.Path]::GetFullPath($ReportDirectory)
if ($Address -notmatch '^(?<host>[^:]+):(?<port>\d+)$' -or [int]$Matches.port -lt 1 -or [int]$Matches.port -gt 65535) {
    throw 'Dedicated address must be host:port with a valid TCP port'
}
$serverHost = $Matches.host; $serverPort = [int]$Matches.port
if ($serverHost -notin @('127.0.0.1', 'localhost')) { throw 'Disposable dedicated smoke requires a loopback address' }
if (Test-Path -LiteralPath $report) { throw 'Connected smoke requires a new report directory to preserve prior evidence' }
if ($report.StartsWith($sourceServer.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The report must be outside the prepared source server'
}
if (!(Test-Path -LiteralPath $sourceServer -PathType Container)) { throw "Missing prepared dedicated server $sourceServer" }
if ((Get-Item -LiteralPath $sourceServer).Attributes -band [IO.FileAttributes]::ReparsePoint -or
        @(Get-ChildItem -LiteralPath $sourceServer -Recurse -Force -Attributes ReparsePoint).Count) {
    throw 'Prepared dedicated server must not contain filesystem links'
}
if (!(Test-Path -LiteralPath $prepared -PathType Leaf)) { throw "Missing prepared client launch $prepared" }
$markerPath = Join-Path $sourceServer '.ae2-crafting-time-dedicated-fixture.json'
if (!(Test-Path -LiteralPath $markerPath -PathType Leaf)) { throw 'Prepared server is not marked as an AE2 Crafting Time source fixture' }
$marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
if ($marker.schema -ne 1 -or $marker.sourceFixtureId -ne 'ae2-crafting-time' -or
        $marker.role -ne 'source' -or $marker.target -ne $Target) {
    throw 'Prepared server marker does not match the requested source fixture and target'
}
$profilePath = Join-Path $bundle 'profile.json'
if (!(Test-Path -LiteralPath $profilePath -PathType Leaf)) { throw 'Connected bundle has no profile.json' }
$allArtifacts = @(Get-ChildItem -LiteralPath (Join-Path $bundle 'mods') -File -Filter 'ae2-crafting-time-*.jar')
$drivers = @($allArtifacts | Where-Object BaseName -Match '-test-driver$')
$production = @($allArtifacts | Where-Object BaseName -NotMatch '-test-driver$')
if ($drivers.Count -ne 1 -or $production.Count -ne 1) {
    throw 'Connected dedicated smoke requires exactly one production and one test-driver artifact'
}
$artifacts = @($production[0], $drivers[0]) | ForEach-Object {
    [ordered]@{ path=$_.FullName; name=$_.Name; sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
}
if (@($artifacts | Where-Object { !$_.sha256 -or $_.sha256.Length -ne 64 }).Count) {
    throw 'Connected dedicated artifact validation did not produce SHA-256 identities'
}
$profile = Get-Content -LiteralPath $profilePath -Raw | ConvertFrom-Json
$expectedJava = if ($Target -like '1.20.1-*') { 17 } elseif ($Target -eq '1.21.1-neoforge') { 21 } else { 25 }
$targetParts = $Target.Split('-')
if ($profile.target -ne $Target -or $profile.java -ne $expectedJava -or !$profile.loader) {
    throw 'Connected bundle target, Java, and loader must match the requested target'
}
if ($production[0].Name -notlike "*-$($targetParts[1])-$($targetParts[0]).jar" -or
        $drivers[0].BaseName -ne ($production[0].BaseName + '-test-driver')) {
    throw 'Production and driver filenames must identify the same version and target'
}
$preparedProfile = Get-Content -LiteralPath $prepared -Raw | ConvertFrom-Json
if ($preparedProfile.target -ne $Target -or $preparedProfile.java -ne $expectedJava) {
    throw 'Prepared client target and Java must match the connected server'
}
$loaderVersion = $profile.loader -replace ('^' + [regex]::Escape($targetParts[0]) + '-'), ''
$loader = if ($Target -eq '1.20.1-forge') { "net/minecraftforge/forge/1.20.1-$loaderVersion" }
    elseif ($Target -like '*-neoforge') { "net/neoforged/neoforge/$($profile.loader)" }
    else { $null }
$launcher = if ($Target -eq '1.20.1-fabric') { Join-Path $sourceServer 'fabric-server-launch.jar' }
    else { Join-Path $sourceServer "libraries/$loader/win_args.txt" }
if (!(Test-Path -LiteralPath $launcher -PathType Leaf)) { throw "Prepared server launcher is missing: $launcher" }
if (!$JavaHome) { $JavaHome = & (Join-Path $PSScriptRoot 'get-java-home.ps1') -Major $profile.java }
$java = Join-Path ([IO.Path]::GetFullPath($JavaHome)) 'bin/java.exe'
if (!(Test-Path -LiteralPath $java -PathType Leaf)) { throw "Java executable is missing: $java" }

# All inputs are validated before the disposable copy is created or mutated.
New-Item -ItemType Directory -Path $report -Force | Out-Null
$runtimeRoot = Join-Path $report 'runtime'
$server = Join-Path $runtimeRoot "server-$Target"
$resolvedRuntime = [IO.Path]::GetFullPath($runtimeRoot)
$resolvedServer = [IO.Path]::GetFullPath($server)
if (!$resolvedServer.StartsWith($resolvedRuntime.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Disposable server path escapes the report runtime directory'
}
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
Copy-Item -LiteralPath $sourceServer -Destination $resolvedServer -Recurse
$copyMarker = [ordered]@{ schema=1; sourceFixtureId='ae2-crafting-time'; role='disposable'; target=$Target
    source=[IO.Path]::GetFullPath($sourceServer); createdAt=[DateTime]::UtcNow.ToString('o') }
$copyMarker | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $resolvedServer '.ae2-crafting-time-dedicated-fixture.json') -Encoding UTF8
$validatedCopy = Get-Content -LiteralPath (Join-Path $resolvedServer '.ae2-crafting-time-dedicated-fixture.json') -Raw | ConvertFrom-Json
if ($validatedCopy.role -ne 'disposable' -or $validatedCopy.source -ne $sourceServer -or $resolvedServer -eq $sourceServer) {
    throw 'Connected runner refused an unmarked or in-place dedicated server'
}
$copiedWorld = [IO.Path]::GetFullPath((Join-Path $resolvedServer 'ae2ct-cpu-list-connected'))
if (!$copiedWorld.StartsWith($resolvedServer.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Disposable world path escapes its server directory'
}
if (Test-Path -LiteralPath $copiedWorld) { Remove-Item -LiteralPath $copiedWorld -Recurse -Force }

$control = Join-Path $report 'control'
$serverResult = Join-Path $report 'server-result.json'
$serverOut = Join-Path $report 'server.stdout.log'
$serverErr = Join-Path $report 'server.stderr.log'
New-Item -ItemType Directory -Path $control -Force | Out-Null
$mods = Join-Path $resolvedServer 'mods'
New-Item -ItemType Directory -Path $mods -Force | Out-Null
Get-ChildItem -LiteralPath $mods -File -Filter 'ae2-crafting-time-*.jar' | Remove-Item -Force
foreach ($artifact in $artifacts) { Copy-Item -LiteralPath $artifact.path -Destination (Join-Path $mods $artifact.name) }
[ordered]@{ target=$Target; sourceServer=$sourceServer; disposableServer=$resolvedServer
    artifacts=@($artifacts | ForEach-Object { [ordered]@{name=$_.name;sha256=$_.sha256} })
    dependencies=@(Get-ChildItem -LiteralPath $mods -File -Filter '*.jar' | Sort-Object Name | ForEach-Object {
        [ordered]@{name=$_.Name;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
    }) } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $report 'dedicated-artifacts.json') -Encoding UTF8
Set-Content -LiteralPath (Join-Path $resolvedServer 'eula.txt') -Value 'eula=true' -Encoding Ascii
@('level-name=ae2ct-cpu-list-connected','online-mode=false', 'server-ip=127.0.0.1', "server-port=$serverPort",
    'pause-when-empty-seconds=-1','view-distance=6','simulation-distance=6') |
    Set-Content -LiteralPath (Join-Path $resolvedServer 'server.properties') -Encoding Ascii

$serverArgs = @("-Dae2ct.testDriver.serverScenario=cpu-list-total-ttc-connected",
    "-Dae2ct.testDriver.serverTarget=$Target", "-Dae2ct.testDriver.serverResult=$serverResult",
    "-Dae2ct.testDriver.serverControl=$control", '-Xmx4G')
if ($Target -eq '1.20.1-fabric') { $serverArgs += @('-jar','fabric-server-launch.jar','nogui') }
$argsFile = Join-Path $report 'dedicated-java.args'
$serverArgs | ForEach-Object { '"' + $_.Replace('\', '\\').Replace('"', '\"') + '"' } |
    Set-Content -LiteralPath $argsFile -Encoding UTF8
$launchArguments = @("@$argsFile")
if ($Target -ne '1.20.1-fabric') { $launchArguments += @("@libraries/$loader/win_args.txt", 'nogui') }
$planPath = Join-Path $report 'connected-runner-plan.json'
[ordered]@{ target=$Target; sourceServer=$sourceServer; disposableServer=$resolvedServer; java=$java
    argumentFile=$argsFile; arguments=$serverArgs; launchArguments=$launchArguments; preparedLaunch=$prepared; address=$Address } |
    ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $planPath -Encoding UTF8
if ($PlanOnly) { Write-Host "Connected runner plan validated: $planPath"; return }

$portReservation = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $serverPort)
try { $portReservation.Start() } catch { throw "Dedicated smoke port $serverPort is already in use" }
finally { $portReservation.Stop() }

$start = [Diagnostics.ProcessStartInfo]::new()
$start.FileName = $java
foreach ($argument in $launchArguments) { $start.ArgumentList.Add($argument) }
$start.WorkingDirectory = $resolvedServer
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.WindowStyle = [Diagnostics.ProcessWindowStyle]::Hidden
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
$serverProcess = [Diagnostics.Process]::new(); $serverProcess.StartInfo = $start
if (!$serverProcess.Start()) { throw 'Dedicated server did not start' }
$outTask = $serverProcess.StandardOutput.ReadToEndAsync(); $errTask = $serverProcess.StandardError.ReadToEndAsync()
try {
    $deadline = [DateTime]::UtcNow.AddMinutes(3)
    $ready = $false
    while ([DateTime]::UtcNow -lt $deadline -and !$ready) {
        if ($serverProcess.HasExited) { throw "Dedicated server exited $($serverProcess.ExitCode) before accepting a client" }
        $probe = [Net.Sockets.TcpClient]::new()
        try {
            $connect = $probe.ConnectAsync($serverHost, $serverPort)
            $ready = $connect.Wait(250) -and $probe.Connected
        } catch { $ready = $false } finally { $probe.Dispose() }
    }
    if ([DateTime]::UtcNow -ge $deadline) { throw 'Dedicated server did not become ready' }
    & (Join-Path $PSScriptRoot 'run-ui-smoke.ps1') -Target $Target -Scenario cpu-list-total-ttc `
        -ReportDirectory (Join-Path $report 'client') -BundleDirectory $bundle -PreparedLaunch $prepared `
        -DedicatedAddress $Address -ControlDirectory $control
    if ($LASTEXITCODE) { throw "Connected client smoke exited $LASTEXITCODE" }
    if (!$serverProcess.WaitForExit(60000)) { throw 'Dedicated server did not finish after client evidence completed' }
    if (!(Test-Path -LiteralPath $serverResult -PathType Leaf)) { throw 'Connected server produced no result artifact' }
    $result = Get-Content -LiteralPath $serverResult -Raw | ConvertFrom-Json
    if ($result.result -ne 'PASS') { throw "Connected dedicated server failed: $($result.error)" }
    foreach ($required in @((Join-Path $resolvedServer 'logs/latest.log'), (Join-Path $control 'state.properties'))) {
        if (!(Test-Path -LiteralPath $required -PathType Leaf)) { throw "Connected evidence is missing: $required" }
    }
    Copy-Item -LiteralPath (Join-Path $resolvedServer 'logs/latest.log') -Destination (Join-Path $report 'server.latest.log')
    Copy-Item -LiteralPath (Join-Path $control 'state.properties') -Destination (Join-Path $report 'server-estimates.properties')
} finally {
    if (!$serverProcess.HasExited) { $serverProcess.Kill(); $serverProcess.WaitForExit() }
    $outTask.Result | Set-Content -LiteralPath $serverOut -Encoding UTF8
    $errTask.Result | Set-Content -LiteralPath $serverErr -Encoding UTF8
    $serverProcess.Dispose()
}
