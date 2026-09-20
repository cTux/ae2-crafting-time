# Connected resource fixture implementation plan

Tracks [#482](https://github.com/cTux/ae2-crafting-time/issues/482).
Follow the [spec](spec.md) and [design](technical-design.md). This prerequisite
must merge with fixture-only qualification before #376 resumes its icon fix.

1. **Bounded control (RF3).** Add resource command/state DTOs, strict bounded
   properties parsing and pure transition checks beside existing driver control
   classes. Reuse directory/epoch, source-marker and atomic move utilities.
   Cover every action/state, identity mismatch, duplicate replay/conflict,
   sequencing overflow/gaps, malformed/oversized file, link/path refusal,
   in-flight retries and reset acknowledgement revision. No production packet.
2. **Server fixtures (RF1/RF5).** Add resource setup/hold/return/cancel/reset
   operations using StandardCraftFixture and ServerDriverPlatform seams; delegate
   from shared DedicatedCpuScenario. Keep 26.1.2 API conversions in its target
   adapter; there is no separate DedicatedCpuScenario variant to copy. Build
   optional key/cell factories only for Forge and NeoForge 1.21.1. Register the
   named Forge-only bucketless test fluid in driver initialization. Verify
   native processing dispatch, warmup, exact units, partial insertion, overlap
   and cleanup. Do not add classes/resources to production source sets.
3. **Client lifecycle (RF1/RF2/RF6).** Add one shared resource scenario for
   integrated and connected modes, with the existing 26.1.2 entrypoint adapter.
   Drive the fixed case list, native locate input, release and fresh cancel
   jobs; reconnect only after the armed server acknowledgement. Observe normal
   packet-owned plates with menus closed and chat disabled. Capture held,
   overlap, winner-promoted, rejoined, completed and cancelled states. Keep
   server facts separate from client snapshots; never populate client caches.
4. **Runner and evidence (RF3/RF4/RF6).** Extend run-connected-dedicated-ui-smoke,
   run-ui-smoke, prepare-ui-smoke-launch and existing host/guest dispatch seams
   for the exact leaves and explicit ResourceFixtureOnly option. Update
   DriverOptions, supported cases, SuitePlan, DriverResult and selection/result
   validators coherently. Preserve CPU-list/recurrence behavior. Use the same
   manifest validation, source/disposable markers, identity binding, logs,
   progress watchdogs and cleanup. Validate fixture-only evidence before a
   prerequisite pass; ordinary suites must never interpret it as icon coverage.
5. **Review and commit before tests.** Follow AGENTS.md and the driver/development
   skills. Check the whole diff, optional-class isolation and driver packaging;
   one conventional implementation commit lets the hook create the PR. Then
   run the existing driver/core/runner/launch/lifecycle checks extended above,
   shared changed-behavior coverage and all four production/driver builds.
6. **Runtime qualification (RF1–RF6).** Preflight and run the matrix below on
   the immutable PR head, sequentially. First prove integrated Forge native,
   then connected Forge native, then the remaining applicable rows. Only after
   cheap checks and representative execution pass expand the matrix. Keep
   GitHub CI distinct from local proof; resolve invalidated evidence on new SHAs.

## Runtime matrix

| Target | Java / compatible graph at planning baseline | Required runs |
| --- | --- | --- |
| Forge 1.20.1 | 17; Forge 47.4.23, AE2 15.4.10 | Integrated + connected native including bucketless; integrated + connected AppMek 1.4.3 |
| Fabric 1.20.1 | 17; Fabric 0.19.5, AE2 15.1.0 | Integrated + connected native |
| NeoForge 1.21.1 | 21; NeoForge 21.1.251, AE2 19.2.17 | Integrated + connected native; integrated + connected AppMek 1.6.3 |
| NeoForge 26.1.2 | 25; NeoForge 26.1.2.109, AE2 26.1.10-beta | Integrated + connected native |

Use explicit base-only graphs for native runs; AppMek runs use its focused graph
with transitive Mekanism dependencies, not every optional addon. Each connected
source marker must match that exact graph and launch bundle. Re-read pins from
scripts/run-client-versions.json and retain resolved artifact hashes. Do not
upgrade dependencies as part of this prerequisite.

Preflight host Java 17/21/25 (Gradle on 17/21), matching guest Java/native client
manifests and dedicated source markers, source dependency hashes, disposable
world setup, driver isolation and available memory. Missing source installs
are named provisioning blockers; no unmarked server or guest Gradle substitute.
Use existing setup paths, document exact blocked target if preparation is absent,
and do not silently create a general provisioner. Record launch count, cold-start
cost when measured and a wall-time budget before starting the matrix.

## Evidence and completion

Every target records a complete fixed case list: native item/water/lava/overlap,
plus Forge bucketless, or chemical oxygen/hydrogen/overlap. Every single-output
case warms up, holds until delayed, reconnects in connected mode, releases to
completion, resets, then submits a fresh job for cancellation. Overlap releases
slot 0 and observes slot 1 before finishing it. Require server and client evidence
to agree on output identity, provider position and lifecycle; capture the actual
unfixed icon baseline without treating it as #376 success.

RF3/RF4 negative tests include wrong player/epoch/fixture/revision, invalid case
or slot, early reconnect, command replay, timeout during mutation, failed
cleanup, unsupported chemical targets, missing integration and misplaced flag.
Run existing CPU-list and recurrence runner contract tests as regression checks;
broaden their runtime only if shared behavior changes invalidate prior coverage.

## Executable coverage map

| Boundary | Executable regression | Runtime gate and retained artifact |
| --- | --- | --- |
| Identity, transition, replay/conflict, reset revision and final client acknowledgement | `ResourceFixtureControlTest` | exact receipt/state comparison in both runner validators; JUnit XML |
| Multi-tick warmup return, partial insertion and original-failure-preserving cleanup | `ResourceFixtureControlTest.multiSlotWarmupReturnPollsEveryPartialSlotUntilComplete`, `partialInsertionRetainsTheUninsertedRemainder`, `connectedAbortBindsOriginalFailureAndTerminalRevision` and `cleanupFailureKeepsTheOriginalFailureVisible` | authoritative `resourceCleanup`, abort acknowledgement and server job timing snapshots; `resource-fixture-evidence.json` |
| Focused case catalogue and graph selection on all supported targets | `scripts/test-ui-smoke-plan.ps1` | `get-ui-smoke-plan.ps1` rejects ordinary-suite inclusion and selects one compatible graph; plan JSON |
| Explicit fixture-only authorization and unsupported chemical targets | `scripts/test-ui-smoke-matrix.ps1` | `run-ui-smoke-matrix.ps1` rejects absent/misplaced flags before launch; matrix plan JSON |
| Setup deadline and active progress watchdog | resource cases in `scripts/test-ui-smoke-fast-path.ps1` | 300-second setup and 60-second active limits; progress/status JSON |
| Launch wrapper forwarding | `scripts/test-run-ui-smoke-wrapper.ps1` | executes the public wrapper against a parameter-validating matrix fixture and requires `ResourceFixtureOnly`; captured invocation result |
| Fixed captures, hashes, delayed timing, frame facts, plate/rainbow semantics, server/client agreement and mode-specific receipt traces | `scripts/test-resource-fixture-contract.ps1` exercises the validator; integrated and connected resource rows provide runtime coverage after PR creation | source-defined positive/negative validator fixtures cover identities, cardinalities, job states, receipt agreement and sidecar binding; real PNGs, sidecars, client evidence and server results remain post-PR runtime evidence |

The post-PR test command must execute every regression named above; source-text
matching is not accepted as coverage. A runtime row passes only after its mode's
validator emits the combined evidence artifact from the exact expected trace.

Ready to unblock #376 means: current-head tests/builds/CI pass, all matrix
fixture checks and cleanup pass, artifacts/captures are reviewed and archived,
and every result explicitly says fixture-only with icon acceptance NOT_RUN.
Update parent driver/dependency docs from planned to implemented fixture status
only with that evidence. Leave #376 open for its production implementation,
payload/persistence boundary tests and complete visual acceptance campaign.
