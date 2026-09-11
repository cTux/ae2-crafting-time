# CodexVM MCP Evaluation Spec

Keep the existing OpenSSH, test-driver and localhost VNC workflow. The reviewed
MCP candidates fail the credential safety gate, so this evaluation records a
no-adoption result without installing or executing them. Tracking issue:
[#383](https://github.com/cTux/ae2-crafting-time/issues/383).

## Scope

Measure the existing workflow and document where each control path belongs.
Rejecting a candidate before execution is a valid safety outcome, not a successful
MCP benchmark. No comparative speed claim follows from that rejection.

This work changes documentation and a short prepared-client skill reference.
It does not change Minecraft code, loaders, smoke execution, credentials, VM
configuration or installed dependencies. Do not create a replacement MCP server,
third-party patch, test shim or benchmark framework. Do not reboot or stop the VM
for measurements, or disrupt an active client.

## Acceptance criteria

- **VM-01:** Explain why a vCenter REST server cannot control this local
  Workstation VM. Identify exact reviewed source commits, dependencies, license
  evidence and maintenance observations; distinguish source findings from tests.
- **VM-02:** Never execute, install or register either reviewed candidate.
  Never provide credentials to MCP arguments or password-bearing process argv.
  Keep the existing key-based SSH path and localhost-only VNC endpoint. An
  encrypted credential store does not eliminate subsequent argv exposure.
- **VM-03:** Record three warm repetitions of each eligible existing operation:
  current-state readiness, PowerShell query, file copy into and out of a
  disposable directory, log retrieval, process inspection and VNC capture.
  Preserve raw timestamps, elapsed time, success/failure, byte counts, agent/tool
  round trips and artifacts. Report median and range only for successful samples,
  with failure counts alongside them.
- **VM-04:** Label cold boot and pre-Tools measurements `NOT_MEASURED`. Label
  credentialed MCP operations `BLOCKED_BY_REVIEW`; unexecuted credential-free MCP
  operations are `NOT_MEASURED`. Do not substitute analytical estimates or fake
  backend timings. The existing VNC path and direct scripts overlap: do not count
  the same measurement as independent implementations.
- **VM-05:** Publish a decision table, prerequisite/readiness checks, limitations,
  fallback and rollback guidance. A future configuration example may be disabled
  and use placeholders, but cannot imply the reviewed candidate is safe to enable.
  Do not claim a live MCP configuration was verified.
- **VM-06:** After the documentation PR exists, complete one clean prepared
  `1.20.1-forge`, `compatible`, `craft-plan` smoke using Java 17 and the installed
  native loader. Retain required screenshots, sidecars and visual review, bind
  evidence to the tested SHA, and confirm the exact client exited. VNC stays the
  visual evidence path; an automation response is not visual proof.

## Issue deliverable mapping

| Issue deliverable | Required outcome |
|---|---|
| Review candidate before execution | VM-01/02: pinned source review rejects credentialed use; no execution follows |
| Confirm Workstation 26 and exact VM | Verify actual host/VM and existing transport; candidate compatibility is only source-assessed, not certified |
| Optional pinned STDIO configuration | VM-05: disabled example only; live installation/registration is not applicable to no-adoption |
| Keep credentials out of Git, logs and arguments | VM-02: SSH key auth; no candidate credentials or vmrun password fallback |
| Benchmark VNC/skill, direct and MCP paths | VM-03/04: measure eligible existing paths; retain explicit blocked/unmeasured cells for MCP and cold boot |
| Timings, round trips, size, recovery and pre-Tools behavior | Raw existing-path observations; bounded failure/readback check; pre-Tools observation explicitly unmeasured |
| Adopt only with measured speed or safety benefit | No adoption; no measured MCP benefit is established and source review fails safety |
| Update usage docs/skills and rollback | VM-05: links and decision guidance; existing execution route remains in use |
| Real prepared-client or Prism verification | VM-06: one focused prepared Forge smoke, no Prism installation |

Benchmarks and smoke are pending until their actual artifacts exist. A source
review alone does not complete this issue. See the
[design](technical-design.md) and [implementation plan](implementation-plan.md).
