# Connected resource lifecycle fixtures

Planning draft for [#482](https://github.com/cTux/ae2-crafting-time/issues/482),
the test-driver prerequisite for [#376](https://github.com/cTux/ae2-crafting-time/issues/376).
No fixture implementation or runtime qualification is claimed here.

## Goal and boundary

Provide repeatable real AE2 item, fluid and chemical processing jobs that can
be held, released, cancelled and observed through a client reconnect. Reuse the
existing prepared clients and disposable connected server. This changes only
development tooling; player behavior, profiling, highlight packets, persistence
and production rendering remain unchanged.

The parent [resource-icon contract](../spec.md#delayed-resource-icon-scenarios-planned)
still owns #376's visible-icon acceptance. #482 establishes fixture readiness
without requiring the missing production icon fix. Its explicit fixture-only
runs record `productionIconAcceptance: NOT_RUN`, even when all fixture checks
pass. They cannot satisfy or close #376. Missing icons are retained in captures,
never painted by the driver, hidden, or called a rendering pass.

## Required cases

| Scenario | Targets | Fixture cases |
| --- | --- | --- |
| `delayed-resource-icons` | Forge/Fabric 1.20.1, NeoForge 1.21.1 and 26.1.2 | Item, water, lava, shared-provider water/lava overlap; bucketless control on Forge 1.20.1 |
| `appmek-resource-icons` | Forge 1.20.1 and NeoForge 1.21.1 with AppMek | Oxygen, hydrogen, shared-provider oxygen/hydrogen overlap |

Use real encoded processing patterns, native CPUs and actual provider input
acceptance. The driver acts as a deterministic processing machine: output is
released only after its matching input was dispatched. Never seed production
delayed state, invoke highlight sends or mutate client plates. Keep every case
inside one marked disposable grid, with at most two active jobs and two outputs.

Observe automatic red plates with chat disabled and all menus closed. Exercise
release to completion, cancellation, one held-job reconnect, and promotion of
the surviving shared-provider output. A manual locate before reconnect proves
rainbow state does not return. Run a fresh cancellation job after completion;
do not interpret a command acknowledgement as completion or cancellation.

## Acceptance criteria

- **RF1:** Both scenarios can drive the named real jobs in integrated and
  connected modes on every applicable target. Capture dispatch, held DELAYED,
  release/completion and cancellation, with matching authoritative key/amount.
- **RF2:** The same client disconnects and reconnects while the dedicated job
  remains held. Server state survives, normal login resync restores its red
  plate, and rainbow state stays absent. Overlap promotes the surviving output.
- **RF3:** Commands are bounded and tied to one campaign, scenario, player,
  fixture and revision. Wrong identities, stale/duplicate/conflicting commands,
  invalid phases and oversized/malformed files cannot repeat a mutation.
- **RF4:** Sources remain immutable; only marked disposable copies change.
  Success and failure clean fixture jobs/state and owned processes. Unknown
  process ownership or failed cleanup is a failure, not a reason to kill broadly.
- **RF5:** Base item/fluid runs load without AppMek. Chemical cases require the
  exact supported integration and fail if it is missing. Driver dependencies,
  test resources and optional classes never enter production artifacts.
- **RF6:** Evidence binds server facts, actual client observations, reviewed
  captures, artifacts and dependency graph to one tested SHA. Fixture readiness
  and #376 visual acceptance remain separately reported.

## Not included

No production fix, new production key type, general remote-command API, arbitrary
server support, simultaneous clients, loader upgrades or new server provisioner.
Resource reload, provider removal/unload, malformed production payloads and final
icon/tint correctness remain #376 checks using these reusable fixtures. No new
translations or player settings. Existing CPU-list, recurrence and `appmek-cpu`
contracts remain unchanged.
