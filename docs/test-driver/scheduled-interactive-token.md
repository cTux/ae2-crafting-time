# Scheduled interactive test-driver token

Tracking: [#426](https://github.com/cTux/ae2-crafting-time/issues/426).

This page separates the original failure from the current source and remaining
runtime acceptance. It is not a new security or connected-client test result.

## Original failure

At the issue's recorded revision, `run-ui-smoke.ps1` generated
`AE2CT_TEST_DRIVER_TOKEN` only in the calling process environment. A scheduled
task launched Java outside that inherited environment. The interactive driver
then rejected initialization with `interactive token must be 256-bit lowercase hex`.

Automated non-interactive suites did not exercise this path. The report used a
prepared Forge 1.20.1 client and is distinct from the shaded Tomcat classloader
problem in [#353](https://github.com/cTux/ae2-crafting-time/issues/353).

## Current source path

At documentation baseline `cc453f7e`, the launcher passes an interactive token
only when interactive mode is selected. `Start-UiSmokeScheduledJava` in
`scripts/ui-smoke-scheduled-java.ps1` creates a per-run named pipe and a scheduled
PowerShell relay. The relay reads the token, checks its format, puts it in its
process environment, and starts the prepared Java executable. It clears its
environment value and disposes the pipe on exit.

Non-interactive launches omit the token argument and retain direct scheduled Java
execution. The source tracks the actual Java process identity and scheduled-task
cleanup separately. Do not describe the original missing propagation as absent
from current source.

`scripts/test-ui-smoke-scheduled-java.ps1` includes relay inheritance, absent-token,
malformed-token, and task-action checks. Their presence is source evidence, not a
claim that this documentation task ran them or verified Minecraft authentication.

## Remaining verification

1. Use the prepared scheduled interactive launch path on the supported Windows
   guest and record exact source, Java, loader, task, and Java process identities.
2. Confirm development endpoint initialization and authenticated access with the
   fresh per-run token. Missing and wrong credentials must be rejected.
3. Verify the token is absent from command-line arguments, scheduled task action,
   logs, retained reports, and persistent user-level environment. Do not include
   the token itself in evidence.
4. Exercise normal exit, startup failure, timeout, and cleanup. Preserve the
   prepared executable/argument-file identity checks and avoid stale tasks or
   processes after a failed launch.
5. Run the existing scheduled-launcher checks and a non-interactive regression
   case. Record applicable target coverage and every untested boundary.

Retain token format and authentication checks; disabling them is not a fix.
Link runtime evidence and implementation history on #426 before classifying it
complete. Merging this explanation alone does not close the issue.
