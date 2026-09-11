# CodexVM MCP Evaluation Plan

Deliver the [spec](spec.md) using the existing tools described in the
[design](technical-design.md). Merge these three documents before implementation.

## Changes and checks

| Order | Change or evidence | Criteria |
|---|---|---|
| 1 | Add an evaluation/results document with pinned rejection evidence and pending measurement cells; link it from `docs/dev-client.md` and the prepared-client skill | VM-01/02/04/05 |
| 2 | Review the complete documentation diff, commit once conventionally and let the repository hook create the documentation PR | All; repository ordering |
| 3 | Recheck actual host VM, SSH, VNC, Java, prepared manifest and fixture prerequisites; preserve active work | VM-02/06 |
| 4 | Measure existing paths three times warm, retain raw rows and one bounded missing-file recovery check; no MCP execution | VM-03/04 |
| 5 | Run one clean compatible Forge `craft-plan` smoke and review/archive its visual evidence | VM-06 |
| 6 | Fill measured results, failure counts and limitations from artifacts; review final docs and verify CI separately | All |

Use `docs/codexvm-mcp-evaluation/results.md` for the final decision and results.
Any results commit changes documentation only. Bind runtime evidence to the SHA
actually tested; disclose a later results-only commit and use the existing
[provenance rules](../ui-smoke-evidence.md#timing-and-provenance) instead of
pretending it was the runtime's SHA. Do not rerun a full matrix for prose alone.

## Focused benchmark commands

The following are execution instructions for verification after the PR exists,
not measurements performed by this plan. Resolve placeholders locally:

```powershell
$vmrun = '<absolute-vmrun-path>'
$vmx = '<exact-CodexVM-vmx-path>'
$key = '<existing-SSH-key-path>'
$guest = '<existing-user>@<verified-guest-IP>'
$vnc = '<absolute-existing-VNC-helper-path>'
$evidence = '<new-host-evidence-directory>'
$guestDirectory = '<new-guest-temp-directory-with-unique-run-id>'
& $vmrun -T ws list
& $vmrun -T ws checkToolsState $vmx
& $vmrun -T ws getGuestIPAddress $vmx
ssh -i $key -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10 $guest 'powershell.exe -NoProfile -Command "$PSVersionTable.PSVersion.ToString()"'
ssh -i $key -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10 $guest 'powershell.exe -NoProfile -Command "Get-Process -Id $PID | Select-Object Id,ProcessName | ConvertTo-Json -Compress"'
python $vnc capture (Join-Path $evidence 'screen-1.png')
```

For each operation use `[Diagnostics.Stopwatch]::StartNew()`, capture the native
exit code immediately, stop the timer, and save the row defined by the design.
Run three repetitions and use distinct output names. Measure tool/agent latency
separately from the subprocess stopwatch; obtain round-trip counts from the run
record. `getGuestIPAddress` must be bounded by the invoking tool timeout rather
than an indefinite wait. Do not print the resulting private address in published
results.

Create one unique guest directory through SSH PowerShell, checking first that it
does not exist. Create a small host `fixture.log` containing only a generated
marker, then use the installed OpenSSH `scp` with the same key and strict host-key
options to copy it into that directory and back. Quote guest paths according to
the installed Windows OpenSSH transport; use a simple path without spaces for
this disposable fixture. If scp is unavailable, use existing SSH PowerShell to
write/read base64 bytes and label the measured path `SSH PowerShell transfer`.
Do not install a transfer service. Capture file sizes and verify SHA256 with
`Get-FileHash` on both endpoints outside the timed transfer operation.

Retrieve `fixture.log` with SSH PowerShell `Get-Content -Raw -LiteralPath` and
measure the returned UTF-8 byte count. Read a deliberately absent sibling once
using `-ErrorAction Stop`; retain the nonzero exit and verify a valid subsequent
read. Stop on unexpected auth/transport errors and inspect them without retries
that mutate services. Clean up only the verified unique fixture paths after
retaining evidence; never remove a computed parent directory.

Allow 15 minutes for baseline measurements and inspection. Record actual time;
this is a planning budget, not a result. Do not restart the VM for cold samples.
No candidate installation, shim, package-manager setup or MCP registration is
part of this work.

## Focused smoke and documentation QA

After the documentation PR exists, inspect the plan before launch:

```powershell
.\scripts\run-ui-smoke-matrix.ps1 -Target 1.20.1-forge -Scenario craft-plan -PlanOnly
.\scripts\run-ui-smoke-matrix.ps1 -Target 1.20.1-forge -Scenario craft-plan -ArchiveRoot '<absolute-evidence-archive>'
```

The selection must contain one compatible primary graph with `craft-plan`,
using the existing Java 17/Forge installation. `-Latest`, Prism and all-target
mode are outside scope. Confirm no active client, the selected manifest's
native libraries/assets, and the fixture marker before launching. Use the
existing runner's staging/provisioning path; an absent native installation is
a setup blocker, not permission to improvise another loader.

Budget one clean launch initially and 45 minutes for the smoke campaign, using
the runner's existing 300-second startup and progress/checkpoint bounds.
Record actual build, staging, loading, assertions, visual review and cleanup
times. Diagnose any failure from retained artifacts before another launch;
never use repeated full campaigns as a debugging loop. No known previous
cold-start duration is assumed by this plan.

Retain selection, bundle identity, status, result, logs, screenshots, sidecars,
archive report and `gate.json`. Review every `REVIEW_REQUIRED` capture and
mismatch. Confirm maximized-window evidence and capture/review the exact client
through VNC while it is available, without delaying known test-driver actions.
If final driver evidence needs additional VNC interaction, follow the VM skill's
capture-before-input rule. Confirm the recorded client exited; never kill
unrelated Java processes. The required timing table includes total task time
and every failed attempt, with missing values explicitly `not measured`.

Before the documentation commit, self-review links, facts, criterion coverage
and the diff. Do not run tests until the hook-created PR exists. Afterward,
check Markdown links/format and the repository's applicable CI; no new test
framework or production test suite is justified for these docs-only changes.

## Disabled future example and rollback

The results document may show this shape solely to explain future policy. It
does not authorize installation or activation of the rejected pins:

```toml
[mcp_servers.vmware_review_only]
command = '<absolute-node-path>'
args = ['<reviewed-pinned-build-path>/dist/index.js']
enabled = false
required = false
enabled_tools = ['vm_list', 'vm_status', 'vm_get_ip', 'vm_check_tools']
default_tools_approval_mode = 'writes'
```

A future candidate needs a new source/credential review and explicit adoption
decision before enabling even this example. Do not embed credentials or copy
unsafe upstream setup commands. Document Windows Credential Locker as storage
only, not a cure for the reviewed argv/timeout defect. The example is supported
by the [official MCP configuration reference](https://learn.chatgpt.com/docs/extend/mcp?surface=cli).

This run leaves no MCP installation or registration to roll back. If a reader
already has an independent VMware MCP configuration, preserve it and record the
ownership boundary; do not remove it as part of this issue. For a future trial,
back up config first, disable only its stanza, stop only its identified process,
and remove only its recorded files. Preserve SSH, credentials and localhost VNC.

## Completion

All VM-01 through VM-06 evidence must be present. Blocked/unmeasured cells stay
explicit; no speed comparison or working MCP setup is claimed. The documented
no-adoption decision plus measured existing paths and real smoke fulfills this
safety-gated evaluation. An unperformed baseline benchmark or failed/unperformed
smoke remains a blocker to issue completion.
