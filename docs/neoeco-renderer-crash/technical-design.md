# NeoEco renderer quarantine design

See the [scope, status and acceptance criteria](spec.md).
Repository investigation used base `44ec1394d658c12ab9ee9d2b1d2649eeb3aa9056`.

## Cause and evidence

NeoEco's [21.1.1 callback](https://github.com/DancingSnow0517/NeoECOAEExtension/blob/3b955f8b5d25a9dd58892ebbc1517ca7819695cf/src/main/java/cn/dancingsnow/neoecoae/client/NeoECOAEClient.java#L65-L68)
captures the event and reads `event.getSectionOrigin()` when its renderer runs.
Its [renderer](https://github.com/DancingSnow0517/NeoECOAEExtension/blob/3b955f8b5d25a9dd58892ebbc1517ca7819695cf/src/main/java/cn/dancingsnow/neoecoae/api/rendering/FixedBlockEntityRenderers.java#L22-L39)
scans the section and calls `region.getBlockEntity` before filtering entity types.
It registers for every section, including sections without NeoEco entities.

The NeoForge 21.1.251 source artifact's `AddSectionGeometryEvent` stores the
supplied origin reference. Its contract says callbacks normally run on a rebuild
thread and unsafe state must be captured during the main-thread event.
`ClientHooks.gatherAdditionalRenderers` forwards that origin. Minecraft's render
section reuses a mutable origin, while the region retains its original chunk
array and minimum chunk coordinates. Reading a moved origin against that old
region can index outside its nine chunks. No particular block entity is necessary
to trigger the failing lookup.

Official artifact: [NeoEco 21.1.1, Modrinth version gWMhHBje](https://modrinth.com/mod/neoecoae/version/gWMhHBje).
The downloaded JAR matched its published SHA-512:

```text
7787072aa99851779324af6f556afb17dac41d36db02bc6c46707b5b429b28598b1524f1f91c18cdf5fcf9c33f0801f739a3a5c531ef694fc132ce577fd05852
```

`javap -c -p` confirmed that the released callback captures the event and invokes
`getSectionOrigin` inside the lambda, immediately before the renderer call.
[Upstream commit e9c4031](https://github.com/DancingSnow0517/NeoECOAEExtension/commit/e9c4031eee32ea1d3a0dcedd431e7f80ff60418f)
copies `event.getSectionOrigin().immutable()` before registering the callback and
explicitly identifies mutable render-section reuse. It is in the later 21.2.6
lineage. Moving to that lineage requires the separate #457 adapter work.

The [original report](https://github.com/cTux/ae2-crafting-time/issues/627) and
[second reproduction](https://github.com/cTux/ae2-crafting-time/issues/627#issuecomment-6128845981)
retain runtime failures. The latter also appears in
[the millisecond TTC verification report](../ttc-milliseconds-instant/verification.md).
Exact historical section coordinates were not captured; do not invent them.

## Fixture flow

`TestDriverRuntime` intercepts `RESULT_WRITTEN`, submits cleanup and
`SuiteFixture.restore` on the integrated server thread, awaits completion plus
two server ticks, then clears observations and starts the next case.
`SuiteFixture` restores blocks, entities, profiles and inventory and teleports the
player to the original position. Standard fixtures move the player away from that
position. Movement and resulting section rebuilds can expose the upstream race;
the evidence does not identify faulty server-thread reset ordering.

Keep that lifecycle intact. A sleep or renderer exception suppression would hide
the defect without fixing the mutable-reference ownership.

## Small repository change

1. Mark the 1.21.1 NeoEco project incompatible in `scripts/run-client-versions.json`,
   with `reason` and `issue_url`. Retain its pin and latest inventory.
   `run-client.ps1` already excludes such projects only from compatible selection
   and rejects an explicit compatible request for one.
2. Remove `neoeco-cpu` from `scripts/ui-smoke-neoforge-suite.json` and register it
   in the existing target-specific `focusedCases` in `scripts/ui-smoke-groups.json`.
   Keep its direct-behavior declaration in `scripts/ui-smoke-coverage.json`.
3. Extend `get-ui-smoke-coverage.ps1` fixture-membership validation to include
   explicitly registered focused cases, validating their real catalogue identity.
   Today it checks ordinary suite membership only, so removing the case without
   this change would make latest coverage fail `MISSING_FIXTURE`.
4. Update dependency inventory and verification evidence. Preserve historical
   failure reports and distinguish a compatible exclusion from latest qualification.

Do not set NeoEco to `FOCUSED_BEHAVIOR`: the planner treats that disposition as an
instruction to add a separate graph during full compatible verification. Its newer
API is outside this scope. The existing explicit latest-project/leaf path is enough.

The boundary is development dependency selection and fixture coverage validation.
No production classes, metadata, packets, saves, mixins or dependency floors change.

## Failure cases and verification prerequisites

Check exclusion from the resolved compatible graph, rejection of explicit
compatible selection, latest retention, explicit diagnostic case expansion,
missing/unknown focused fixture rejection, and unchanged other-target declarations.
A declared fixture alone proves no runtime compatibility.

Use the existing prepared native client, disposable marked world and host-built
JAR workflow. The loader-specific `1.21.1-neoforge/21.1.251/launch.json` and Java 21
were found during preflight. The older root manifest is not the requested loader.
VM control recovery remains a prerequisite; use the existing launcher/control path
once operational. Do not introduce a new runner or weaken campaign provenance.
