function Assert-UiSmokeStatusContinuation {
    param([string]$Path, [string]$ConfigPath, [string]$World, [string]$CampaignId)
    $value = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    if ($value.schema -ne 1 -or $value.world -cne $World -or $value.campaign -cne $CampaignId -or
            !(Test-Path -LiteralPath $ConfigPath -PathType Leaf) -or
            (Get-FileHash -LiteralPath $ConfigPath -Algorithm SHA256).Hash -ine $value.configSha256) {
        throw 'Saved compact-off client config does not match the phase-1 continuation'
    }
}

function Assert-UiSmokeStatusRelaunchCaptures {
    param([string]$Evidence)
    foreach ($capture in @('status-saved-off', 'status-relaunch-off', 'status-relaunch-on', 'status-relaunch-restored')) {
        foreach ($extension in @('png', 'json')) {
            if (!(Test-Path -LiteralPath (Join-Path $Evidence "$capture.$extension") -PathType Leaf)) {
                throw "Status amount relaunch capture is missing: $capture.$extension"
            }
        }
    }
}

function Assert-UiSmokeStatusAddonKeys {
    param([string]$Evidence, [string]$Target)
    $value = Get-Content -LiteralPath (Join-Path $Evidence 'status-addon-keys.json') -Raw | ConvertFrom-Json
    $expected = @()
    switch ($Target) {
        '1.20.1-forge' { $expected = @('mana', 'chemical') }
        '1.20.1-fabric' { $expected = @('mana') }
        '1.21.1-neoforge' { $expected = @('chemical') }
        '26.1.2-neoforge' { $expected = @() }
        default { throw "Unknown status addon target: $Target" }
    }
    if ($value.schema -ne 1 -or $value.appbotLoaded -ne ('mana' -in $expected) -or
            $value.appmekLoaded -ne ('chemical' -in $expected) -or
            (Compare-Object @($value.captured) @($expected) -SyncWindow 0)) {
        throw "Status addon-key inventory does not match $Target"
    }
    foreach ($name in $expected) {
        foreach ($capture in @("status-addon-$name", "status-addon-$name-tooltip")) {
            foreach ($extension in @('png', 'json')) {
                if (!(Test-Path -LiteralPath (Join-Path $Evidence "$capture.$extension") -PathType Leaf)) {
                    throw "Status addon-key capture is missing: $capture.$extension"
                }
            }
        }
    }
}

function Get-UiSmokeJavaLaunchPhases {
    param(
        [Parameter(Mandatory)][string]$Scenario,
        [switch]$ContainsCpuList,
        [switch]$ResumeOnly,
        [switch]$PrepareOnly
    )
    if ($PrepareOnly) { return @() }
    if ($ResumeOnly) { return @(2) }
    if ($Scenario -in @('cpu-list-total-ttc', 'standard-status-controls') -or $ContainsCpuList) { return @(1, 2) }
    return @(1)
}

function Assert-UiSmokeJavaPhaseIdentities {
    param(
        [Parameter(Mandatory)][object[]]$Processes,
        [Parameter(Mandatory)][int[]]$ExpectedPhases,
        [switch]$FinalApproval
    )
    if ($Processes.Count -ne $ExpectedPhases.Count) {
        throw "UI-smoke Java launch count does not match the phase plan: expected $($ExpectedPhases.Count), observed $($Processes.Count)"
    }
    for ($index = 0; $index -lt $ExpectedPhases.Count; $index++) {
        $process = $Processes[$index]
        if ($process.phase -ne $ExpectedPhases[$index] -or $process.pid -le 0 -or !$process.startedAt -or
                $process.exitCode -ne 0 -or !$process.exitedAt) {
            throw 'Incomplete UI-smoke Java phase identity'
        }
    }
    if ($ExpectedPhases.Count -eq 2 -and ($Processes[0].pid -eq $Processes[1].pid -or
            $Processes[0].startedAt -eq $Processes[1].startedAt)) {
        throw 'Relaunch did not produce a distinct second client process identity'
    }
    if ($FinalApproval -and ($ExpectedPhases.Count -ne 2 -or $ExpectedPhases[0] -ne 1 -or $ExpectedPhases[1] -ne 2)) {
        throw 'Final relaunch approval requires the complete two-phase Java plan'
    }
}

function Write-UiSmokeRelaunchEvidence {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$CampaignId,
        [Parameter(Mandatory)][string]$Target,
        [Parameter(Mandatory)][string]$Profile,
        [Parameter(Mandatory)][string]$Scenario,
        [Parameter(Mandatory)][string]$World,
        [AllowNull()][string]$ConnectionEpoch,
        [Parameter(Mandatory)][string]$PredecessorCheckpointSha256,
        [Parameter(Mandatory)][object[]]$Processes,
        [Parameter(Mandatory)][object[]]$Artifacts,
        [AllowNull()][string]$DependencyMode,
        [AllowNull()][string]$DependencyCatalogueSha256,
        [AllowNull()][string]$ControlStatePath,
        [switch]$ResumeOnly,
        [switch]$FinalApproval,
        [int]$MaxControlStateBytes = 65536
    )
    $controlState = @()
    if ($ConnectionEpoch) {
        if (!$ControlStatePath -or !(Test-Path -LiteralPath $ControlStatePath -PathType Leaf)) {
            throw 'Relaunch evidence requires the connected control state'
        }
        $controlFile = Get-Item -LiteralPath $ControlStatePath
        if ($controlFile.Length -gt $MaxControlStateBytes) {
            throw "UI-smoke control state exceeds $MaxControlStateBytes bytes"
        }
        $controlState = @([IO.File]::ReadAllLines($controlFile.FullName))
    }
    $processEvidence = @($Processes | ForEach-Object {
        [ordered]@{ phase=[int]$_.phase; pid=[int]$_.pid; startedAt=[string]$_.startedAt
            stdout=[string]$_.stdout; stderr=[string]$_.stderr; taskName=[string]$_.taskName
            executable=[string]$_.executable; argumentFile=[string]$_.argumentFile
            exitCode=[int]$_.exitCode; exitedAt=[string]$_.exitedAt }
    })
    $artifactEvidence = @($Artifacts | ForEach-Object {
        [ordered]@{ name=[string]$_.name; sha256=[string]$_.sha256 }
    })
    $connectionEpochValue = if ($ConnectionEpoch) { [string]$ConnectionEpoch } else { $null }
    $payload = [ordered]@{ schema=1; campaignId=$CampaignId; target=$Target; profile=$Profile; scenario=$Scenario
        world=$World; connectionEpoch=$connectionEpochValue; predecessorCheckpointSha256=$PredecessorCheckpointSha256
        processes=$processEvidence; artifacts=$artifactEvidence; dependencyMode=$DependencyMode
        dependencyCatalogueSha256=$DependencyCatalogueSha256; resumeOnly=[bool]$ResumeOnly
        finalApproval=[bool]$FinalApproval; launchCount=$processEvidence.Count; controlState=$controlState }
    [IO.File]::WriteAllText($Path, ($payload | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
}

function New-UiSmokeScheduledJavaTaskAction {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string]$Arguments,
        [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9-]{1,128}$')][string]$PipeName
    )
    $payload = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes((
        [ordered]@{ executable=$Executable; arguments=$Arguments; pipeName=$PipeName } | ConvertTo-Json -Compress)))
    $script = @'
$config = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('__PAYLOAD__')) | ConvertFrom-Json
$pipe = [IO.Pipes.NamedPipeClientStream]::new('.', $config.pipeName, [IO.Pipes.PipeDirection]::In)
try {
    $pipe.Connect(30000)
    $reader = [IO.StreamReader]::new($pipe, [Text.UTF8Encoding]::new($false), $false, 1024, $true)
    $env:AE2CT_TEST_DRIVER_TOKEN = $reader.ReadLine()
    $reader.Dispose()
    if ($env:AE2CT_TEST_DRIVER_TOKEN -cnotmatch '^[a-f0-9]{64}$') { exit 87 }
    $child = Start-Process -FilePath $config.executable -ArgumentList ([string]$config.arguments) -PassThru -Wait
    exit $child.ExitCode
} finally {
    Remove-Item Env:\AE2CT_TEST_DRIVER_TOKEN -ErrorAction SilentlyContinue
    $pipe.Dispose()
}
'@.Replace('__PAYLOAD__', $payload)
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
    [pscustomobject]@{ Execute=(Get-Process -Id $PID).Path
        Argument="-NoProfile -NonInteractive -WindowStyle Hidden -EncodedCommand $encoded" }
}

function Start-UiSmokeScheduledJava {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string]$Arguments,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][string]$TaskName,
        [Parameter(Mandatory)][string]$InteractiveUser,
        [ValidatePattern('^[a-f0-9]{64}$')][string]$InteractiveToken,
        [int]$StartTimeoutSeconds = 30
    )
    if ([IO.Path]::GetFileName($Executable) -cne 'java.exe' -or !(Test-Path -LiteralPath $Executable -PathType Leaf)) {
        throw 'Interactive smoke task must launch the prepared Java executable directly'
    }
    if ($Arguments -cnotmatch '^@".+ui-smoke-java\.args"$') { throw 'Interactive smoke task requires the prepared argument file' }
    if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) { throw "UI-smoke Java task already exists: $TaskName" }
    $before = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Select-Object -ExpandProperty ProcessId)
    $tokenPipe = $null
    if ($InteractiveToken) {
        $pipeName = 'ae2ct-' + [guid]::NewGuid().ToString('N')
        $tokenPipe = [IO.Pipes.NamedPipeServerStream]::new($pipeName, [IO.Pipes.PipeDirection]::Out, 1,
            [IO.Pipes.PipeTransmissionMode]::Byte, [IO.Pipes.PipeOptions]::Asynchronous)
        $relay = New-UiSmokeScheduledJavaTaskAction -Executable $Executable -Arguments $Arguments -PipeName $pipeName
        $action = New-ScheduledTaskAction -Execute $relay.Execute -Argument $relay.Argument -WorkingDirectory $WorkingDirectory
    } else {
        $action = New-ScheduledTaskAction -Execute $Executable -Argument $Arguments -WorkingDirectory $WorkingDirectory
    }
    $principal = New-ScheduledTaskPrincipal -UserId "$env:COMPUTERNAME\$InteractiveUser" -LogonType Interactive -RunLevel Limited
    Register-ScheduledTask -TaskName $TaskName -Action $action -Principal $principal | Out-Null
    try {
        $notBefore = [DateTime]::UtcNow.AddSeconds(-1)
        Start-ScheduledTask -TaskName $TaskName
        if ($tokenPipe) {
            $connected = $tokenPipe.WaitForConnectionAsync()
            if (!$connected.Wait($StartTimeoutSeconds * 1000)) { throw 'Scheduled Java token relay did not connect' }
            $writer = [IO.StreamWriter]::new($tokenPipe, [Text.UTF8Encoding]::new($false), 1024, $true)
            $writer.WriteLine($InteractiveToken)
            $writer.Flush()
            $writer.Dispose()
        }
        $deadline = [DateTime]::UtcNow.AddSeconds($StartTimeoutSeconds)
        do {
            Start-Sleep -Milliseconds 250
            $candidate = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {
                $_.ProcessId -notin $before -and $_.ExecutablePath -ieq $Executable -and
                $_.CommandLine.Contains($Arguments.TrimStart('@').Trim('"')) -and
                $_.CreationDate.ToUniversalTime() -ge $notBefore
            } | Sort-Object CreationDate | Select-Object -First 1
        } while (!$candidate -and [DateTime]::UtcNow -lt $deadline)
        if (!$candidate) { throw 'Scheduled Java task did not create the prepared client process' }
        $process = Get-Process -Id $candidate.ProcessId
        $null = $process.Handle
        [pscustomobject]@{ process=$process; taskName=$TaskName; commandLine=$candidate.CommandLine
            executable=$candidate.ExecutablePath; startedAt=$candidate.CreationDate.ToUniversalTime().ToString('o') }
    } catch {
        Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
        throw
    } finally {
        if ($tokenPipe) { $tokenPipe.Dispose() }
    }
}

function Get-UiSmokeScheduledJavaStartParameters {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string]$Arguments,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][string]$TaskName,
        [Parameter(Mandatory)][string]$InteractiveUser,
        [AllowNull()][string]$InteractiveToken
    )
    $parameters = @{ Executable=$Executable; Arguments=$Arguments; WorkingDirectory=$WorkingDirectory
        TaskName=$TaskName; InteractiveUser=$InteractiveUser }
    if ($InteractiveToken) {
        if ($InteractiveToken -cnotmatch '^[a-f0-9]{64}$') { throw 'Interactive token must be 256-bit lowercase hex' }
        $parameters.InteractiveToken = $InteractiveToken
    }
    return $parameters
}

function Remove-UiSmokeScheduledJava {
    param([Parameter(Mandatory)][string]$TaskName)
    Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
}

function Get-UiSmokeScheduledJavaProcessState {
    param(
        [Parameter(Mandatory)][int]$ProcessId,
        [Parameter(Mandatory)][string]$TaskName,
        [scriptblock]$ProcessLookup = { param($id) Get-Process -Id $id -ErrorAction SilentlyContinue },
        [scriptblock]$TaskLookup = { param($name) Get-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue },
        [scriptblock]$InfoLookup = { param($task) $task | Get-ScheduledTaskInfo }
    )
    if ($null -ne (& $ProcessLookup $ProcessId)) { return [pscustomobject]@{state='running';exitCode=$null} }
    $task = & $TaskLookup $TaskName
    if ($null -eq $task) {
        return [pscustomobject]@{state='disappeared';exitCode=$null}
    }
    if ($task.State -eq 'Running') { return [pscustomobject]@{state='finishing';exitCode=$null} }
    if ($task.State -ne 'Ready') { return [pscustomobject]@{state='disappeared';exitCode=$null} }
    $info = & $InfoLookup $task
    $unsignedResult = [uint32]$info.LastTaskResult
    $exitCode = [BitConverter]::ToInt32([BitConverter]::GetBytes($unsignedResult), 0)
    return [pscustomobject]@{state='exited';exitCode=$exitCode}
}
