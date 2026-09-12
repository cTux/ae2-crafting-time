# CodexVM shared-folder plan

Deliver the [specification](spec.md) using the observed flow and boundaries in
the [design](technical-design.md). Merge this document trio before recovery or
implementation. Follow repository hook/PR ordering; do not run local tests
before the applicable PR exists. Documentation preparation does not authorize
VM mutations or Minecraft execution by itself.

## Ordered work

| Order | Work and check | Criteria |
|---|---|---|
| 1 | Retain a fresh exact-VM/account/share/provider snapshot and repeat the actual HGFS file reads with structured results. Compare it with the dated failure report. | SF-01, SF-04 |
| 2 | Read the existing prepared bundle through the share, recompute host/guest hashes and record its actual source identity. Preserve the original bundle. | SF-02, SF-06 |
| 3 | If failure recurs, distinguish mapping, provider and request-context causes using the same known file. Record a bounded failure and matching successful observation before selecting a correction. | SF-01, SF-04 |
| 4 | Only for a demonstrated cause, back up the exact affected state, verify active work, apply the smallest authorized recovery and compare pre/post inventories. Record rollback and readback. If reads keep passing, skip recovery. | SF-03, SF-04 |
| 5 | Through the exact worktree share, copy a unique disposable input into guest-local NTFS and return a unique report. Check hashes/bytes and both SSH and interactive contexts; clean up only owned artifacts. | SF-02, SF-03 |
| 6 | Run the representative clean prepared smoke below; retain returned status, archive, captures, fixture hashes and exact exit evidence. | SF-05, SF-06 |
| 7 | Review all evidence and document what recovered, what remained unproven, configuration differences, timing and cleanup. Report GitHub CI separately. | SF-01–SF-06 |

## Bounded diagnosis

Use the existing VM skill to resolve the exact local VM, CLI, key and VNC
helper. Use the interactive host account and existing key-based SSH with strict
host-key checking. Do not publish private paths, guest addresses or credentials.
Do not rerun credential provisioning when SSH already works.

Record operation text, UTC timestamps, elapsed time, exit code, exact error,
requested path, mapping and account/session context. A file existence result is
weaker than reading and hashing its bytes. Probe a known valid root, share and
file; do not use `net view` as the HGFS pass/fail signal. Use explicit structured
output rather than mixed PowerShell table objects.

Allow 15 minutes for the initial diagnostic/transfer phase. No Minecraft launch
belongs in that phase. If a request stalls, stop only the identified probe and
retain its timeout; do not reboot or cycle services as a generic retry. A new
probe should distinguish a specific hypothesis. Do not run a destructive
failure toggle merely to manufacture a regression.

For a justified mutation, record the exact target, backup hash, expected effect,
rollback and unchanged shares before acting. Use only the existing supported
VMware controls. A required restart follows the VM skill after checking active
work; never edit the running VMX. A proposed launcher change or independently
useful provisioning tool requires an amended design and check mapping first.

For transfer verification, allocate unique task-owned directories, verify their
resolved paths and preserve existing files. Hash the selected bundle files on
the host, through HGFS and after copying into guest-local storage. Return a
small generated report through HGFS and compare bytes/hashes on the host.
Inspect the existing interactive session before creating any temporary probe
task; record its identity and remove only that task and its output afterward.
Do not treat a scheduled-task exit alone as proof that its file operation passed.

## Representative smoke

Recheck the exact mapping, Java 17, Forge 47.4.10 manifest, native libraries,
assets, source-only fixture marker, local runtime ownership, available VNC and
absence of an active client before launch. Keep Forge 47.4.23 and other targets
out of this run. No Prism or modpack installation is needed.

After the applicable PR exists, preview the existing plan on the host:

```powershell
.\scripts\run-ui-smoke.ps1 -Target 1.20.1-forge -Scenario craft-lifecycle -PlanOnly
```

Review the emitted graph, dependency mode and reasons. This issue's unchanged
transport needs one compatible Forge graph. Run the same host command without
`-PlanOnly` to preserve normal bundle preparation, dispatch, gates and archives:

```powershell
.\scripts\run-ui-smoke.ps1 -Target 1.20.1-forge -Scenario craft-lifecycle `
    -ArchiveRoot '<verified-writable-archive-root>'
```

The matrix registers the exact worktree through the existing dispatcher. It
builds or reuses a sealed bundle for the current revision and emitted graph;
do not override its dependency mode or silently expand scope to a full suite.
The existing #353 catalogue bundle serves the earlier transport checks and
keeps its original identity. Do not inject it into the campaign cache or relabel
it with the current `HeadSha`. No guest Gradle build or SSH transfer fallback
substitutes for this HGFS verification.

Record planned process launches and start a wall-clock timer before the first
smoke action. Allow one initial Minecraft launch, with a 15-minute diagnostic
budget and the runner's existing startup/progress bounds. The prior successful
same scenario took 6m03s, including 2m32s startup; these are historical costs,
not a new measurement. If the budget expires, inspect status and stop only the
verified recorded client through the existing stop path, preserving evidence.
Do not use repeated full launches to diagnose an earlier transfer failure.

Require all 12 current `craft-lifecycle` checks and 13 required screenshots,
sidecars, maximized readable UI, contained bounds, source-fixture hash equality,
report return and confirmed exact-client exit. Run the existing evidence/archive
completion path and inspect every `REVIEW_REQUIRED` image. Retain extra captures
as well. Record automatic and manual visual outcomes
separately. Archive before another attempt can overwrite the report.

If native launch fails, inspect the exact loader/classpath error before changing
the existing installation. The known absent historical classpath entry alone
does not justify provisioning. Record actual setup, staging, loading, assertions,
review, cleanup and failed-attempt timings using the prepared-smoke skill's table.

## Completion conditions

SF-01 through SF-06 each need retained evidence. Tests and smoke remain pending
until executed; current read hashes satisfy only the observed read portion of
SF-02. A new result must name its tested SHA and any later documentation-only
change rather than implying the later commit was executed.

If HGFS remains healthy, report `not reproduced; cause unresolved` and exactly
which transfer/smoke checks passed. Do not claim a repair or that the historical
cause request is complete. If a cause is confirmed and corrected, include its
before/after evidence and rollback, then repeat only invalidated checks before
the final clean smoke. Preserve unresolved limitations in the issue handoff.
