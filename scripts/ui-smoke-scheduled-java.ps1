function Get-UiSmokeJavaLaunchPhases {
    param(
        [Parameter(Mandatory)][string]$Scenario,
        [switch]$ResumeOnly,
        [switch]$PrepareOnly
    )
    if ($PrepareOnly) { return @() }
    if ($ResumeOnly) { return @(2) }
    if ($Scenario -eq 'cpu-list-total-ttc') { return @(1, 2) }
    return @(1)
}

function Assert-UiSmokeJavaPhaseIdentities {
    param(
        [Parameter(Mandatory)][object[]]$Processes,
        [Parameter(Mandatory)][int[]]$ExpectedPhases,
        [switch]$FinalApproval
    )
    if ($Processes.Count -ne $ExpectedPhases.Count) { throw 'UI-smoke Java launch count does not match the phase plan' }
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

function Start-UiSmokeScheduledJava {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string]$Arguments,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][string]$TaskName,
        [Parameter(Mandatory)][string]$InteractiveUser,
        [int]$StartTimeoutSeconds = 30
    )
    if ([IO.Path]::GetFileName($Executable) -cne 'java.exe' -or !(Test-Path -LiteralPath $Executable -PathType Leaf)) {
        throw 'Interactive smoke task must launch the prepared Java executable directly'
    }
    if ($Arguments -cnotmatch '^@".+ui-smoke-java\.args"$') { throw 'Interactive smoke task requires the prepared argument file' }
    if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) { throw "UI-smoke Java task already exists: $TaskName" }
    $before = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Select-Object -ExpandProperty ProcessId)
    $action = New-ScheduledTaskAction -Execute $Executable -Argument $Arguments -WorkingDirectory $WorkingDirectory
    $principal = New-ScheduledTaskPrincipal -UserId "$env:COMPUTERNAME\$InteractiveUser" -LogonType Interactive -RunLevel Limited
    Register-ScheduledTask -TaskName $TaskName -Action $action -Principal $principal | Out-Null
    try {
        $notBefore = [DateTime]::UtcNow.AddSeconds(-1)
        Start-ScheduledTask -TaskName $TaskName
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
    }
}

function Remove-UiSmokeScheduledJava {
    param([Parameter(Mandatory)][string]$TaskName)
    Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
}
