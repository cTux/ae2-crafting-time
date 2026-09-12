# CodexVM shared-folder recovery

Restore and verify the prepared smoke staging route described in
[dev-client.md](../dev-client.md#host-build-and-vm-staging). Tracking issue:
[#400](https://github.com/cTux/ae2-crafting-time/issues/400).

On 2026-09-12, issue #353 verification reported HGFS system error 67 through
both SSH and the logged-in guest session. Registered shares, running drivers,
a guest restart and a Tools service restart did not restore access. The smoke
used a separately verified SSH transfer instead; that did not prove HGFS worked.

Read-only investigation later that day could read existing shares and hash
the existing bundle through HGFS without any recovery change. The historical
failure is not reproduced and its cause is unresolved. Current reads are not
proof of a repair, guest write-back, desktop-session access or a complete smoke.

## Scope

Investigate the failure, make only a demonstrated recovery correction, and
retain a usable recovery record. Reuse the existing key-based SSH, `vmrun`,
localhost VNC, prepared loader and smoke scripts. The guest runtime stays on
local NTFS; the exact shared worktree carries inputs and returned evidence.

This is a host/guest staging issue shared by Forge and Fabric 1.20.1, NeoForge
1.21.1 and NeoForge 26.1.2. It does not change Minecraft behavior, mod support,
wire formats, saved worlds or dependency versions. One representative Forge
smoke covers this transport boundary while launcher behavior is unchanged.

Do not add a transport fallback, replace the launcher, reinstall Tools, remove
historical shares in bulk, introduce password-based control or weaken artifact
and fixture checks. No launcher change is justified by the current evidence.
If a new cause requires code or substantial provisioning, document its exact
scope and verification before implementing it.

## Acceptance criteria

- **SF-01 — Reproducible observations:** Record the exact VM and host account
  context, versions, mapping, provider state, operation, timestamp, native exit
  and error. Distinguish the historical report from current observations. Use
  actual HGFS file operations; SMB enumeration is not an HGFS health check.
- **SF-02 — Transfer proof:** Read the exact existing prepared bundle through
  HGFS and compare host/guest SHA-256 values. In owned disposable locations,
  verify copying into guest-local storage and returning a report through the
  share, including byte/hash equality. Confirm access in both the dispatch and
  interactive guest contexts before treating both as healthy.
- **SF-03 — Preserve other work:** Before any recovery mutation, verify the
  exact VM and active guest work, retain a backup of the affected configuration
  and a share inventory, and define its rollback. Preserve unrelated shares,
  services, credentials and processes. Afterward, compare the inventory and
  clean up only this run's identified files, tasks and processes.
- **SF-04 — Cause before correction:** Confirm a proposed cause with a
  discriminating failure/recovery observation before claiming it fixed or
  changing launcher behavior. If the failure remains absent, record
  `not reproduced; cause unresolved`, preserve the working configuration and
  identify the missing causal evidence. Passing transfers alone do not complete
  the historical cause investigation or justify a fixed claim.
- **SF-05 — Normal staging smoke:** After transfer checks pass, run one clean
  HGFS-dispatched `1.20.1-forge`, `compatible`, Forge `47.4.10`, Java 17
  `craft-lifecycle` smoke. Require the existing 12 semantic checks, 13 required
  captures and their sidecars. Retain extra captures and verify visual review,
  intact source fixture, returned report and confirmed exact-client exit.
  A missing visual baseline remains
  `REVIEW_REQUIRED`; manually reviewed images do not become an automatic PASS.
- **SF-06 — Honest provenance:** Bind inputs, configuration changes, checks
  and final results to their actual source revision and file hashes. Keep prior
  #353 evidence and reused-bundle diagnostics separate from current-head proof.
  Preserve the existing bundle, plan, fixture and archive gates. Report local
  verification separately from GitHub CI and retain measured timings.

The [design](technical-design.md) records the current evidence and boundaries.
The [plan](implementation-plan.md) defines the ordered checks. These documents
do not claim that a recovery or new smoke has already happened.
