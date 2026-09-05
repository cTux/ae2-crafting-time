# Startup Integration Diagnostics Spec

Status: implemented; see [runtime evidence and limits](../ui-smoke-evidence.md#startup-integration-diagnostics-2026-09-05).
Source: [issue #193](https://github.com/cTux/ae2-crafting-time/issues/193).
Read with the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).

## Goal

A player can attach `latest.log` and show what AE2 Crafting Time registered,
which optional integrations are absent, which hooks were actually observed,
and what failed, without enabling debug logging.

The report describes this process and its observed capabilities. An installed
mod, a supported version, or a past smoke result is not proof of activation in
the current run. A healthy report is not a guarantee that every addon works.

## Requirements

| ID | Required behavior |
| --- | --- |
| SD-01 | Write startup context once per process: actual AE2 Crafting Time, Minecraft, loader, and AE2 versions; build target; physical client or dedicated server. Use an explicit unknown value and reason when metadata is unavailable. |
| SD-02 | Report every integration in the design inventory, with canonical integration ID, actual installed mod ID/version when present, capability, outcome, and reason. Distinguish native AE2 hook reuse, custom adapters, and compatibility-only entries. Development pins alone never create supported integrations. |
| SD-03 | Report confirmed registration or hook activation at INFO, only after the named operation completes successfully. Report evidence at capability level; a render callback cannot confirm tooltip, click, or CPU hooks. Native-hook readiness must explicitly say it does not verify an addon-specific job. |
| SD-04 | Report normal skips at INFO: target not supported, client-only capability on a dedicated server, optional mod absent, or feature disabled by configuration. Do not resolve optional classes to check presence. |
| SD-05 | Leave deferred hooks pending until evidence exists. Report a capability's first confirmed activation once. No timeout converts an unused hook into failure or success. A partly observed integration lists its remaining pending capabilities. |
| SD-06 | Report known recoverable degradation at WARN, naming the disabled capability and retained functionality. Report unrecoverable failure at ERROR when our code owns the failure boundary; preserve the original cause. Unexpected failures include the throwable and stack trace. |
| SD-07 | End available entrypoint checks with one compact integration summary: initialized, skipped, pending, partial, and failed counts. Later observations produce bounded transition messages, not repeated startup summaries. Do not repeat on ticks, frames, screen openings, or world changes. |
| SD-08 | Recover only from the read-only adapter boundaries established in the design. Disable the affected optional UI/lookup capability for the process. Keep original screen behavior, server profiling, and stored history intact. Never turn an unreadable estimate into zero or an incomplete total. |
| SD-09 | Preserve loader dependency checks, required AE2 behavior, injection requirements, and original upstream crashes. Do not catch arbitrary gameplay failures, suppress Mixin errors, add speculative upper version caps, or advertise universal crash prevention. |
| SD-10 | Verify client and dedicated-server behavior on all four supported targets. Keep diagnostics local to each process; no packet, save-format, telemetry, or translation changes. |

## Outcomes and boundaries

- `initialized`: every applicable capability in that integration's declared
  reporting scope is confirmed. State the scope, especially `shared-hooks`.
- `skipped`: no capability applies in this process; state why.
- `pending`: at least one applicable capability is unverified, with no known
  failure. List confirmed and pending capabilities separately.
- `partial`: at least one capability failed or was disabled after a compatibility
  failure, while another is confirmed or still pending. List what remains.
- `failed`: all applicable capabilities are unavailable because of failure,
  or a required capability has an unrecoverable failure. Include
  `action=disabled` or `action=propagate` so a recoverable optional failure is
  not confused with a fatal startup failure.

Expected configuration/side skips do not count as partial failure. The summary
counts integrations once, not each capability or each Crafting Tree package
variant. Core registration/hook diagnostics are separate from optional counts.

An integrated server belongs to a physical client process. Client and logical
server capabilities can be observed in that process, but opening another world
does not reset the report. A dedicated server never loads client diagnostics or
screen classes. Server and client logs are independent; neither claims to know
the other side's activation state.

## Non-goals

- No new integrations, supported targets, dependency versions, or gameplay UI.
- No automatic disabling of whole mods, CPU adapters, or shared AE2 hooks.
- No blanket exception handler, runtime Mixin rollback, or crash-handler plugin.
- No startup class scanning, forced screen/CPU construction, synthetic crafting,
  performance metrics, per-job log stream, or persistent diagnostic history.
- No promise to catch errors before our entrypoint runs or after an upstream
  mod corrupts state. Keep the loader's original diagnostic in these cases.

## Acceptance checks

| Check | Observable result | Requirements |
| --- | --- | --- |
| AC-01 | Each target's client and server log has one context line with runtime versions and one available-check summary; counts reconcile with inventory rows. | SD-01, SD-02, SD-07, SD-10 |
| AC-02 | A core-only installation reports expected optional skips at INFO; no optional target or client class is loaded merely for diagnostics. | SD-02, SD-04, SD-10 |
| AC-03 | With a compatible addon, its actual version is reported, unused hooks stay pending, and exercising each supported path produces its own first confirmation. Presence and a no-data estimate never imply full activation. | SD-03, SD-05 |
| AC-04 | Shared AE2 hook reuse and compatibility-only mods are identified honestly, including Forge AdvancedAE's packaged adapter and unsupported target combinations. | SD-02, SD-03 |
| AC-05 | An incompatible reflective UI fixture disables only our optional additions, logs the missing member/type and one diagnostic, and leaves the host screen and a subsequent normal AE2 craft working. | SD-06, SD-08 |
| AC-06 | Missing AdvancedAE selected-CPU reflection leaves network-level stats available and selected-CPU diagnostics unavailable, without changing samples. | SD-06, SD-08 |
| AC-07 | Unexpected invoked exceptions retain their original cause; fatal errors and CPU/loader/Mixin failures propagate. Failure evidence is preserved even if no summary is reached. | SD-06, SD-09 |
| AC-08 | Repeated renders, callbacks, errors, screen reopening, and world changes do not repeat an already emitted outcome or grow an unbounded report. | SD-05, SD-07 |
| AC-09 | Automated checks cover state transitions, inventory, registration, reflection failures, and absence boundaries; actual smoke logs demonstrate success, absence, deferred activation, recovery, and unrecoverable failure on the required matrix. | SD-01 through SD-10 |
