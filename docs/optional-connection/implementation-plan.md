# Optional connection implementation plan

See the [scope and status](spec.md) and [design](technical-design.md).

## Prerequisite gate

Implement the focused observation mode described in the design before running the
matrix. This test prerequisite is part of the issue's explicit sixteen-cell
verification scope. It reuses the existing driver and runner; no absent-side driver
or standalone observer is introduced. Do not add a general multiplayer framework.

The matrix uses all four compatible prepared targets and their native AE2 graph:
Forge 1.20.1 and Fabric 1.20.1 with Java 17, NeoForge 1.21.1 with Java 21, and
NeoForge 26.1.2 with Java 25. Prepared manifests are
`C:/Users/Public/Documents/AE2CraftingTimeSmoke/prepared/<target>/launch.json`.
Use existing dedicated provisioning and disposable fixture markers; never edit
the sealed source or a player's server. Production-absent cells must also omit
the existing production-dependent test driver.

Initial preflight on 2026-09-26 found the host Java 17/21/25 environment paths,
the configured CodexVM VMX and VMware CLI. The VM was initially stopped. Guest
manifest, runtime, source-fixture and launcher verification is recorded below;
configuration names alone are not runtime evidence.

Read-only guest inspection then verified all four manifests and their runtime
directories. Installed guest Java executables and release files identify
17.0.20.1, 21.0.12.1 and 25.0.4.1. Fabric's manifest has 73 classpath entries,
all present. The default Forge, NeoForge 1.21.1 and NeoForge 26.1.2 manifests
have respectively 94, 107 and 102 entries, each with one absent loader-version
JAR entry. These may be installer-generated placeholder paths; no launch was
performed, so the manifests remain unqualified rather than proven broken.
Version-specific manifests also exist and must be matched to the selected graph.

A schema-2 Fabric dedicated source marker and its launcher were found. Other
dedicated source trees include old runs and incomplete staging; their presence
does not establish reusable sealed sources for the new graph. Validate existing
sources or use `scripts/prepare-dedicated-ui-smoke-server.ps1` after provisioning
is authorized. No installer, game, server or test was run during preflight.

The VM reached its desktop and established key-based SSH worked. VMware Tools
did not report an address; the active DHCP lease matched the VM's configured MAC
and provided the SSH route. Guest assets were read through that route. This is
a viable inspection path, but the standard dispatcher's guest-address lookup
needs recovery or an explicitly reviewed equivalent route before execution.

## Ordered work after prerequisite approval

| Step | Change | Criteria | Check |
| --- | --- | --- | --- |
| 0 | Add focused driver observation mode, required loader-send invocation hooks, Forge fixture-registration suppression and runner per-side staging/manual checkpoints. Update test-driver spec/design for this mode. | OC-2, OC-8 | Receipt-validation and staging self-checks after PR creation; all twelve send seams accounted for; missing hooks/receipts fail. |
| 1 | Confirm optional-channel APIs against all resolved loader versions. Add smallest shared compatibility/state decisions and regression cases. | OC-1, OC-2, OC-6 | Absent, matching, incompatible and disconnected inputs; 100% shared line/branch coverage. |
| 2 | Change all four registrations and guard all twelve payload directions. Route stats/CPU bypasses through the adapters. | OC-1, OC-2, OC-6 | Compile all targets; packet boundary checks; source sweep for sends outside guarded adapters. |
| 3 | Gate effective client features and direct renderer preferences; consolidate session cleanup and discard late old-session callbacks. | OC-3, OC-4 | Supported/unsupported transitions, every cleared state, delayed callbacks, native row/amount/control behavior. |
| 4 | Update installation guidance, GuideME and wiki source; retain existing switches and data formats. | OC-7 | Links, source paths, locale parity if text keys change, and consistency with the four combinations. |
| 5 | Review complete patch, then create the one conventional commit and hook-created PR. | All | Repository ordering; no local test execution before the PR. |
| 6 | Run focused deterministic tests, packet checks and coverage; inspect changed-smoke selection. | OC-2 through OC-6 | Current-head unit/boundary checks and required CI reported separately. |
| 7 | Run focused supported-peer UI and integrated singleplayer checks, then the connected installation matrix using the approved prerequisite procedure. | OC-1 through OC-5, OC-8 | Four installation modes on each target; same-process supported/unsupported/supported sequence. |
| 8 | Reconcile evidence and documents, review current-head checks, merge only through the authorized issue workflow. | All | Tested/reviewed/merged SHA, exact matrix receipts and final issue state. |

## Connected evidence contract

Run these modes with `production + driver` only on the installed sides:

| Cell | Client artifacts | Server artifacts | Payload proof | UI proof |
| --- | --- | --- | --- | --- |
| Both | Production and observation-mode driver | Production and observation-mode driver | Qualified nonzero sender receipts for supported traffic | Existing focused checks plus plan/status captures |
| Server only | Neither artifact | Production and observation-mode driver | All server outbound probes attempted, zero loader sends | Native client VNC captures; real craft and history retained |
| Client only | Production and observation-mode driver | Neither artifact | All client outbound probes attempted, zero loader sends | Compare VNC plan/status captures with neither cell |
| Neither | Neither artifact | Neither artifact | Both inventories prove no Crafting Time sender is loaded | Native AE2 login, plan/status and real craft |

Keep the installed client process alive for both to client-only to both, restarting
only the report-owned server between connections if needed. Keep the native client
process alive for server-only to neither. Reuse the corresponding clean world
baseline per cell. Probes and receipts must identify the current connection epoch;
old receipt files cannot satisfy a later connection. The native-client pair needs
no driver endpoint and uses explicit reviewed manual checkpoint receipts.

Before the full matrix, qualify observation mode on one Forge both-installed
connection: prove the hooks see sends, prove custom fixture fluids are not
registered, and prove a missing required receipt fails validation. Then qualify
both-installed observation on each remaining loader before accepting its zero-send
cells. Any observer-caused handshake or registry difference is a harness failure,
not evidence about production compatibility.

For each target, hold Minecraft, loader and AE2 dependencies fixed and vary only
Crafting Time presence. Retain both artifact inventories and hashes. Demonstrate
login, open plan and status screens, exercise relevant controls, and record custom
payload observations in both directions. For server-only, complete a real craft,
restart the disposable server and inspect retained history through a supported
client. For client-only, use the same client process to join a supporting server
again, proving recovery without a restart. Include disconnect/reconnect and late
packet work in the lifecycle check.

Plan launches and a wall-time budget before execution. First prove one target,
then run the remaining selected targets; do not repeat a full campaign for each
diagnostic correction. Final evidence must be a clean run of the current immutable
head. Record scenario, run index, timestamp/time zone, elapsed duration, result,
logs, screenshots and artifact hashes. Cleanly stop owned clients and servers,
then shut down the configured CodexVM and read back its absence from the VM list.

Completion requires all criteria and sixteen connected cells. A missing observer,
profile, runtime, fixture or consent is a named blocker, not a skipped pass.
