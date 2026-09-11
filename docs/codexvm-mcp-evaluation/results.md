# CodexVM VMware MCP Evaluation

We aren't adopting a VMware MCP server for CodexVM. The reviewed candidates
put guest passwords in process arguments, and the primary candidate can repeat
those arguments in timeout errors. An encrypted credential store protects the
password at rest, but it doesn't remove that later exposure. Keep using the
existing key-based SSH path and localhost-only VNC endpoint.

This is a source-review decision. Neither candidate was installed, registered,
or executed, and no live MCP configuration was verified. The detailed scope,
existing flow, and verification contract are in the [specification](spec.md),
[design](technical-design.md), and [implementation plan](implementation-plan.md).

## What was reviewed

| Candidate | Pinned source | Decision |
|---|---|---|
| `havu0/vmware-mcp` 0.2.0 | [Commit `128abd9`](https://github.com/havu0/vmware-mcp/tree/128abd9c37b80fed94edf697116a6d3bf660f1b8) and pinned [`VmrunClient`](https://github.com/havu0/vmware-mcp/blob/128abd9c37b80fed94edf697116a6d3bf660f1b8/src/vmrun.ts) | Rejected for credentialed use. Its shared client adds `-gp` and `-vp` to spawned process arguments and includes joined arguments in timeout errors. |
| `omichelbraga/vmware-mcp` 0.2.0 | [Commit `6d7105b`](https://github.com/omichelbraga/vmware-mcp/tree/6d7105bf843a1d5e6b454c98ec3f9483e43ef8ce) and pinned [`tools_vmrun.py`](https://github.com/omichelbraga/vmware-mcp/blob/6d7105bf843a1d5e6b454c98ec3f9483e43ef8ce/src/vmware_mcp/tools_vmrun.py) | Rejected. Its tools accept guest passwords and forward `-gp` to `vmrun`; the reviewed tree also has no license file. |
| `giuliolibrando/vmware-vsphere-mcp-server` | [Prerequisites](https://github.com/giuliolibrando/vmware-vsphere-mcp-server#prerequisites) | Not applicable. It requires vCenter REST access, which cannot control this local Workstation VM. |

The primary's pinned lockfile uses MCP SDK 1.27.1, Zod 3.25.76,
TypeScript 5.9.3, and Vitest 4.1.0. Its package requires Node 18+, while its
test dependency requires Node 20, 22, or 24+. At the 2026-09-11 review the
repository wasn't archived, was last pushed on 2026-04-18, had tag `v0.2.0`, no GitHub
releases, and no open issues. These are dated maintenance observations, not
local test results or quality guarantees. The secondary requires Python 3.10+
and uses unpinned minimum dependency ranges.

The source review also found Windows temporary-path assumptions, swallowed
copy failures, and unrestricted `.vmx` resolution in the primary candidate.
Those findings reinforce the rejection, but password-bearing process arguments
are the blocking issue. See the [design](technical-design.md#reviewed-sources-and-rejection)
for the exact source paths and full boundary analysis.

## Operation decisions

| Operation | Current path | MCP decision | Verification state |
|---|---|---|---|
| VM discovery and Tools readiness | Existing `vmrun` commands | Keep current path | 3/3 warm samples passed; Tools was running |
| Guest PowerShell queries | Key-based OpenSSH | Keep current path | 3/3 warm samples passed |
| Copy a disposable fixture in and out | Existing OpenSSH transfer | Keep current path | 3/3 warm samples passed in each direction; all SHA-256 hashes matched |
| Retrieve a known fixture log | SSH PowerShell | Keep current path | 3/3 warm samples and the bounded failure/recovery check passed |
| Inspect the exact guest process | SSH PowerShell | Keep current path | 3/3 targeted supplemental samples passed |
| Capture the guest display | Existing localhost VNC helper | Keep current path | 3/3 warm samples passed |
| Credentialed MCP operations | Rejected candidates | Do not run | `BLOCKED_BY_REVIEW` |
| Credential-free MCP operations | No candidate installed | Do not run | `NOT_MEASURED` |
| Cold boot | Existing VM lifecycle path | Do not disrupt the running VM | `NOT_MEASURED` |
| Pre-Tools behavior | Existing VM lifecycle/VNC paths | Do not disrupt the running VM | `NOT_MEASURED` |
| GUI equivalents for query and file operations | None | No meaningful separate path | `NOT_APPLICABLE` |

The current Workstation, exact VM, SSH, VNC, Java, prepared manifest, native
loader, and disposable fixture prerequisites must be rechecked immediately
before measurement or smoke execution. Current-state readiness is not boot
time. VNC already overlaps the direct transport, so it isn't an independent
implementation of every command or file operation.

## Measured existing-path baseline

The baseline was measured at `2c8b196fa3b3c8d56bb6f52c932abe98b03bca33` on
2026-09-11. The 23 retained benchmark rows ran inside one outer agent/tool call,
so their per-row round-trip fields share that attribution and do not represent
23 independent calls. Durations below describe native operations only; they are
not an MCP comparison because no candidate was executed.

| Existing operation | Warm samples | Median | Range | Failures | Evidence |
|---|---:|---:|---:|---:|---|
| Current-state readiness | 3/3 | 1017.50 ms | 782.74–1027.09 ms | 0 | `benchmark.json` rows 1, 8, 15 |
| PowerShell query | 3/3 | 456.64 ms | 456.12–468.71 ms | 0 | `benchmark.json` rows 2, 9, 16 |
| Copy fixture into guest | 3/3 | 327.48 ms | 317.49–329.14 ms | 0 | 32 bytes each; `transfer-hashes.json` |
| Copy fixture out of guest | 3/3 | 319.36 ms | 309.75–324.45 ms | 0 | 32 bytes each; all hashes matched |
| Retrieve fixture log | 3/3 | 501.55 ms | 488.41–528.28 ms | 0 | 32 returned bytes each |
| Process inspection | 3/3 | 545.13 ms | 542.66–556.73 ms | 0 | Targeted `Get-Process -Id $PID` supplement |
| VNC capture | 3/3 | 209.46 ms | 197.42–211.40 ms | 0 | 574,362-byte PNG each |
| Missing-file failure and valid-read recovery | 1/1 | 545.62 ms failure; 487.67 ms recovery | single sample | 0 unexpected | Expected exit 1, then valid read PASS |

Raw rows, UTC timestamps, byte counts, native process counts, batch attribution,
transfer hashes, the three VNC PNGs, and the targeted process supplement are in
`step9-benchmark-20260911T191319271Z`. The missing-file read failed as expected
and the immediately following valid read recovered. The guest fixture directory
was removed.

The first two Forge smoke attempts exposed a test-driver readiness defect: the
one-shot interaction ran before the client was within native reach of the loaded
terminal. After adding bounded server positioning and client reach/readiness
gates, a third Forge attempt reached the GUI but exposed an observer defect: the
assertion treated AE2's documented missing-first rows as part of TTC ordering.
The observer now compares only craftable rows while separately enforcing the
missing-first invariant, including complete plans with no missing rows.

At final implementation commit `1295c9907242e2ad2094177a1facb4ea034ec180`,
the prepared Java 17 native-loader `craft-plan` smoke passed on both Forge and
Fabric 1.20.1. Every semantic check passed, the exact clients exited with code
0, fixture hashes stayed unchanged, disposable worlds were removed, and archive
and cleanup gates passed. Automatic visual comparison reported
`REVIEW_REQUIRED` only because these render environments had no qualified
baseline; manual review of all five captures per target passed for maximization,
GUI containment, TTC text, all three sort states, and tooltip readability.

## Timing and retained evidence

These intervals come from retained timestamps. Client runtimes overlap their
status intervals; do not add both. Gate/archive durations are separate measured
work, not the complete review or cleanup time. No sum below represents total
task time. The first two failures used the baseline SHA above; the third used
`bf326fd10f90588c2281e76e30fee1520304ec66`. They remain failed evidence.

| Part of the smoke UI testing task | Time | Why it took that long |
|---|---:|---|
| VM setup and prerequisite inspection | not measured | Running VM reused; no complete phase receipt |
| Host builds and client staging | not measured | No complete combined phase receipt |
| First Forge failure, status interval | 223.042 s | `WORLD_READY` timeout; retained `startedAt`/`updatedAt` |
| Second Forge diagnostic failure, status interval | 208.784 s | Same timeout at unchanged source; retained status timestamps |
| Third Forge failure, exact client | 223.881 s | GUI opened, then obsolete sort observer rejected missing-first rows |
| Final Forge loading and assertions, exact client | 155.227 s | All semantic checks passed; status interval was 158.269 s |
| Final Fabric loading and assertions, exact client | 180.453 s | Native base fixture and all semantic checks passed; status interval was 183.433 s |
| Final Forge gate and archive | 0.612 s | 492 ms validation + 120 ms archive |
| Final Fabric gate and archive | 0.675 s | 449 ms validation + 226 ms archive |
| Failure diagnosis and visual review | not measured | Failed evidence and all ten final captures reviewed |
| Cleanup as a separate phase | not measured | Exact exits and removed disposable worlds verified, without separate timing |
| Total task time | not measured | No complete outer-task start/end receipt |

Evidence below is retained in the issue #383 evidence bundle; these identifiers
and SHA-256 hashes identify exact files without publishing private host paths.
The baseline directory is `step9-benchmark-20260911T191319271Z`:

| Baseline file | SHA-256 |
|---|---|
| `benchmark.json` | `f6bcbedbfa692c88672ce059c49dda10da5a201bf0c95f01e90d8b9600eb665a` |
| `raw-rows.json` | `c38608d997ce79e3ffeb012bec88542b6862f87ee1e747ac76572fe478891432` |
| `batch-attribution.json` | `e54c0f1f29ee3491992594c7eafbff9b70e671dfe5985b0b9b13ab4e3bc8bde0` |
| `process-inspection-supplement.json` | `7167c8295fa6cbb8f9f4cdbb0bd315dec685a90a4bd0104732e67adc5b255de9` |

The first two failures are retained as `step9-smoke-attempt1-guest` and
`step9-smoke-attempt2-guest`, including status and result files. The third is
`step10-runtime-forge-bf326fd1/20260911T201524046Z-72f4ae59`.
Final archives include gate, status, semantic result, images and sidecars:

| Target | Archive identifier | Root `result.json` SHA-256 |
|---|---|---|
| Forge | `step10-runtime-forge-1295c990/20260911T204132511Z-0d385899` | `0fc9300ef238d20ba9f0a005ae910b781db7d9f09d609eb82e38863156e88e11` |
| Fabric | `step10-runtime-fabric-1295c990/20260911T204624793Z-62f2bc96` | `9face440467d72af5a5f4d73c3fe8999cb3ec1b44b33dd619a1a7f4b5865c93f` |

Numeric readiness log lines were not retained after guest cleanup. Successful
GUI transitions prove the readiness boundary passed but cannot reconstruct
those numbers. This results-only documentation update follows the tested source
commit; it does not claim a new runtime run. GitHub CI is verified separately.

## Future configuration shape

This disabled example documents the review boundary. It does not authorize
installing or enabling either rejected candidate.

```toml
[mcp_servers.vmware_review_only]
command = '<absolute-node-path>'
args = ['<reviewed-pinned-build-path>/dist/index.js']
enabled = false
required = false
enabled_tools = ['vm_list', 'vm_status', 'vm_get_ip', 'vm_check_tools']
default_tools_approval_mode = 'writes'
```

Before a future trial, review a new pinned source revision and its credential
flow, make a new adoption decision, and back up the configuration. Keep all
credentials out of arguments, logs, examples, and Git. Windows Credential
Locker may store a secret at rest; it doesn't fix a program that later places
the secret in process arguments. See the official [MCP configuration
reference](https://learn.chatgpt.com/docs/extend/mcp?surface=cli) for the shape
of a disabled server entry.

There is nothing from this evaluation to roll back because it installed and
registered nothing. Preserve any independently owned VMware MCP configuration.
For a future authorized trial, disable only its recorded stanza, stop only its
identified process, and remove only its recorded files. Preserve the existing
SSH setup, credentials, and localhost VNC binding.
