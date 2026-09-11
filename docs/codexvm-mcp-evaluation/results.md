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
| VM discovery and Tools readiness | Existing `vmrun` commands | Keep current path | Three warm samples pending |
| Guest PowerShell queries | Key-based OpenSSH | Keep current path | Three warm samples pending |
| Copy a disposable fixture in and out | Existing OpenSSH transfer | Keep current path | Three warm samples pending |
| Retrieve a known fixture log | SSH PowerShell | Keep current path | Three warm samples plus one bounded recovery check pending |
| Inspect the exact guest process | SSH PowerShell | Keep current path | Three warm samples pending |
| Capture the guest display | Existing localhost VNC helper | Keep current path | Three warm samples pending |
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

## Pending measurements

No benchmark or smoke has run for this implementation yet. Step 9 will add the
retained artifacts and replace only the pending cells below. It will report
medians and ranges only for successful samples, with failure counts beside
them; it won't substitute estimates for missing data.

| Existing operation | Warm samples | Median | Range | Failures | Evidence |
|---|---:|---:|---:|---:|---|
| Current-state readiness | 0/3 | pending | pending | pending | pending |
| PowerShell query | 0/3 | pending | pending | pending | pending |
| Copy fixture into guest | 0/3 | pending | pending | pending | pending |
| Copy fixture out of guest | 0/3 | pending | pending | pending | pending |
| Retrieve fixture log | 0/3 | pending | pending | pending | pending |
| Process inspection | 0/3 | pending | pending | pending | pending |
| VNC capture | 0/3 | pending | pending | pending | pending |
| Missing-file failure and valid-read recovery | 0/1 | pending | pending | pending | pending |

Each retained raw row will contain UTC start and end times, elapsed
milliseconds, native result, text and artifact byte counts, actual agent/tool
round trips, VMware Tools state, and artifact identity. Transfers will include
SHA-256 verification outside the timed operation. The focused prepared
`1.20.1-forge` `craft-plan` smoke is also pending and will use Java 17 and the
installed native loader after the implementation PR exists.

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
