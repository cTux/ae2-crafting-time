# CodexVM MCP Evaluation Design

The smallest safe result is a documented rejection plus measurements of the
working tools. No runtime integration is needed for the
[specification](spec.md).

## Existing flow

Baseline inspected at `b78cf41c4c3116bff1d5d20eefaf2a93d5c4d3fb`:

1. [`run-ui-smoke.ps1`](../../scripts/run-ui-smoke.ps1) and
   [`run-ui-smoke-matrix.ps1`](../../scripts/run-ui-smoke-matrix.ps1) select and
   build the immutable host bundle.
2. [`invoke-ui-smoke-codexvm.ps1`](../../scripts/invoke-ui-smoke-codexvm.ps1)
   shares the exact worktree, resolves the guest IP with `vmrun`, and dispatches
   encoded PowerShell over key-based OpenSSH. The matrix also calls it for stop;
   invoking it without a bundle delegates back to the matrix.
3. [`run-ui-smoke-codexvm.ps1`](../../scripts/run-ui-smoke-codexvm.ps1) stages a
   guest-local copy and uses the installed native manifest and Java runtime.
   Scheduled interactive Java runs as the existing guest user. The runner
   replaces historical test properties and returns status, logs and images.
4. The test driver performs known actions. VNC supplies visual inspection and
   exceptional GUI input. Cleanup identifies the exact recorded process and
   creation time; it does not stop Java processes broadly.

The password fallback in the dispatcher imports an encrypted credential and
passes its plaintext value to `vmrun -gp`. Do not use that fallback in this
evaluation. One-time [`prepare-codexvm-ui-smoke.ps1`](../../scripts/prepare-codexvm-ui-smoke.ps1)
creates credentials and a temporary provisioning file; it must not be rerun
when existing SSH already works. No transport caller or script needs modification.

## Reviewed sources and rejection

| Candidate | Exact evidence and outcome |
|---|---|
| `havu0/vmware-mcp` | [Commit 128abd9](https://github.com/havu0/vmware-mcp/tree/128abd9c37b80fed94edf697116a6d3bf660f1b8), package 0.2.0, MIT; rejected for credentialed use |
| `omichelbraga/vmware-mcp` | [Commit 6d7105b](https://github.com/omichelbraga/vmware-mcp/tree/6d7105bf843a1d5e6b454c98ec3f9483e43ef8ce), package 0.2.0; rejected because guest tool arguments require passwords |
| `giuliolibrando/vmware-vsphere-mcp-server` | [Prerequisites](https://github.com/giuliolibrando/vmware-vsphere-mcp-server#prerequisites) require Docker and vCenter REST access; wrong control plane for local Workstation |

The primary's [`VmrunClient`](https://github.com/havu0/vmware-mcp/blob/128abd9c37b80fed94edf697116a6d3bf660f1b8/src/vmrun.ts)
adds `-gp`/`-vp` in shared auth flags, passes them to `spawn`, and includes
`args.join(' ')` in timeout errors. All authenticated guest, filesystem, process,
screen, lifecycle, snapshot and variable paths share this wrapper. PasswordVault
storage does not prevent these process arguments or error disclosure. This is
source-established behavior; no real credential was used to reproduce it.

The same source and its guest/file/screen tools use `/tmp` on the Windows host.
Automatic command output retrieval can swallow copy failures and return empty
output. An explicit screenshot destination bypasses one temporary-path problem,
but does not fix credential handling. The configured default VM is not a target
allowlist: `resolveVm` also accepts arbitrary `.vmx` paths. Tool allowlists cannot
repair these implementation boundaries.

The Windows default binary path uses `Program Files (x86)` whereas the inspected
installation uses `Program Files`; an explicit path resolves that mismatch.
Although auth flags hardcode `-T fusion`, the installed Workstation CLI returned
`running`, exit 0, for a read-only `checkToolsState` query with that flag. Do not
report this as demonstrated Workstation incompatibility. No full candidate
compatibility claim follows from that one direct CLI query.

The primary lockfile pins MCP SDK 1.27.1, Zod 3.25.76, TypeScript 5.9.3 and
Vitest 4.1.0. Package runtime requires Node 18+; Vitest requires Node 20/22/24+.
At the 2026-09-11 review the repository was not archived, had no open issues,
and last pushed on 2026-04-18. Tag `v0.2.0` existed; no GitHub releases were
returned. These are dated maintenance observations, not quality guarantees.
Upstream test-count claims are not local test results.

The secondary's pinned [`tools_vmrun.py`](https://github.com/omichelbraga/vmware-mcp/blob/6d7105bf843a1d5e6b454c98ec3f9483e43ef8ce/src/vmware_mcp/tools_vmrun.py)
exposes `user`/`password` tool parameters; its wrapper forwards `-gp` to the
process. Its manifest requires Python 3.10+, `mcp>=1.2.0`, `httpx>=0.27.0` and
`pydantic-settings>=2.2.0`, with no dependency lock in the inspected tree.
Its README requires vmrest for REST features and describes vmcli's interactive
credential limitation. The inspected tree has no LICENSE file: a reusable
license grant was not established, and none is needed to install this rejected
candidate because it will not be installed.

## Feasibility and boundaries

Read-only inspection on 2026-09-11 confirmed Workstation 26.0.0
build-25388281, the exact configured VM running, Tools running, existing
key-based SSH with strict host-key checking, sshd running and VNC listening only
on localhost port 5905. No Java client was observed. The sandbox initially showed
zero VMs and denied the SSH key existence check; the actual host user context
corrected both. Recheck live state rather than starting a duplicate VM.

Host and guest Java 17/21/25 locations exist. Guest manifests exist for all four
prepared targets plus one version-specific Forge installation. The representative
Forge manifest uses Java major 17 and loader 47.4.10, matching the compatible
profile. The host Forge fixture has `level.dat` and its disposable-fixture marker.
Native libraries/assets still require the existing launcher's preflight; their
existence is not inferred solely from a manifest. No new infrastructure is needed.

Use only portable placeholders in published commands. Resolve the actual VMX,
CLI, SSH key, account and VNC helper from the existing local VM skill; never
publish credentials or private guest addresses. Do not change firewall rules,
SSH exposure, VNC binding, VM shares outside normal smoke dispatch, or services.

## Measurement contract

Each raw row records operation/path, repetition, UTC start/end, elapsed
milliseconds, exit code/result, returned text bytes, artifact bytes, agent/tool
round trips, VMware Tools state and artifact identity. Text size and PNG/file
size are separate; command output is not the image payload. Count agent calls
from the actual execution record, not from subprocess count. Separate one-time
setup from three warm repetitions and report total task time separately.

Use existing `vmrun` for discovery/readiness, SSH PowerShell for queries/processes,
existing OpenSSH file transfer for a unique nonsecret fixture, and the VM skill's
VNC helper for capture. Verify transfer hashes. Retrieve a known fixture log so
private application logs are not exposed. A nonexistent path inside the same
disposable directory is a bounded failure check; retain its failure then verify
the next valid read succeeds. Do not provoke credential or service failures.

Current-state readiness is not boot time. Cold boot and pre-Tools state remain
`NOT_MEASURED`. A working VNC endpoint is independent of guest Tools by mechanism,
but pre-Tools screenshot capture is not an observed result here. VNC/skill flow
already includes the direct transport and has no separate equivalent for every
query or file operation. Record `NOT_APPLICABLE` for those GUI-only cells rather
than inventing manual GUI tasks. No MCP runtime performance is measured.

Follow [dev-client.md](../dev-client.md), the prepared-client skill and
[evidence contract](../ui-smoke-evidence.md) for the clean Forge smoke. The
evaluation adds documentation and the bounded driver prerequisite below; no
production behavior or version/loader matrix changes are required. The driver-only readiness correction is defined below.

## Proven readiness blocker and bounded correction

At verification SHA `2c8b196fa3b3c8d56bb6f52c932abe98b03bca33`, both retained
Forge logs place the joining player near `(1.9,84,42)`. The fixture marker points
to terminal `(-13,-59,5)`, face `SOUTH`. Read-only Anvil/NBT inspection of the
tracked Forge source region `r.-1.0.mca`, chunk `(-1,0)`, confirms that exact
block is `ae2:cable_bus`; its block entity contains a `south` part with
`id=ae2:crafting_terminal` and an `ae2:fluix_smart_cable`. The terminal is not
merely an assumed marker coordinate. Saved NBT does not prove the copied live
grid is active; the post-fix GUI smoke must establish that boundary.

Shared `CraftPlanScenario.start()` verifies the disposable world and marker,
but Forge `DriverPlatform.baseFixture()` returns null. No fixture setup moves
the player for `craft-plan`. `openTerminal()` sends `useItemOn` against the
faraway terminal and permanently sets `terminalOpenRequested`. Subsequent ticks
cannot retry before the 30-second `WORLD_READY` timeout. This establishes the
missing reach prerequisite; it does not prove positioning alone cures every
possible grid or menu problem.

Reuse the server-submit/retained-future pattern in shared `AddonCpuFixture.setup`
for the plain block-terminal path. Prefer a small reusable readiness operation
with covered pending/completed/failed transitions over duplicated loader fixes.
Position beside the terminal using its interaction face, not a fixed offset
that puts a SOUTH terminal behind its cable. Wait for the client to observe a
reachable position and loaded terminal before committing the one-shot use.
Missing player/terminal or failed positioning must produce an actionable bounded
failure. Preserve the normal menu-open observation as the success condition.

Place this gate only on the shared block-terminal interaction route; specialized
standard/status flows, requester/analyser routes, wireless fixtures that
intentionally move beyond normal range, and connected-dedicated setup must keep
their current ownership. Existing base/addon positioning should not be repeated
or overridden when the player already satisfies reach. Do not rebuild the grid,
change its stored player data or move a real user's player outside the validated
disposable test world.

The shared `CraftPlanScenario` is consumed by Forge 1.20.1, Fabric 1.20.1 and
NeoForge 1.21.1. The 26.1.2 module excludes that source file and supplies its own
scenario with native base-fixture positioning. Do not port an unproven fix to
that separate implementation. If a new common helper is compiled there, verify
compilation without changing the native behavior. The behavioral change is
confined to development-driver artifacts, never production JARs.
