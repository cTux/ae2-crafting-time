# Conformance audit evidence

Lifecycle and audited revision: [specification](spec.md).

## Cause and correction

Newer feature requirements and implementation replaced older behavior, but
several reference pages retained the older descriptions. The correction follows
the newer requirements and inspected source; it does not change requirements to
bless a defect. Source links below are pinned to the audited revision. Line
numbers in the reference column describe that revision, before this correction.

| Criteria | Stale reference locations | Authority and implementation evidence |
| --- | --- | --- |
| D1 | `working-with-project.md:70-78`, `server-client-stats.md:69,219-224`, development `references/code-map.md:24,60`, `player-controls-and-integrations/spec.md:29` | [#114 requirements](../profiling-and-diagnostics/spec.md#planned-normalized-sample-details-114) supersede whole-window collection; [CraftProfiler](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/main/java/com/ctux/ae2craftingtime/core/CraftProfiler.java#L194) accepts returns and finalizes intervals at the tick flush. |
| D2 | development `references/code-map.md:73` | [Current total semantics](../time-to-craft-plan.md) and [CraftingJobEstimate](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/main/java/com/ctux/ae2craftingtime/core/CraftingJobEstimate.java#L73) use remaining dependency paths, including incomplete-graph confidence. |
| D3 | `architecture.md:67-85,134`, development `references/code-map.md:84`, `world-save-persistence.md:183-213,229-240` | [Remembered statuses](../world-save-persistence.md#remembered-statuses), [SavedSampleHistory](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/main/java/com/ctux/ae2craftingtime/core/SavedSampleHistory.java), and [ProfilerBridge flush](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/ProfilerBridge.java#L219). The mc2612 bridge and all four SavedData adapters retain the same sample/provider/status division. |
| D4 | `server-client-stats.md:238-249` | [CPU sorting requirements](../ttc-sorting/cpu-list/spec.md) supersede visible-only refresh in TTC modes; [CpuTtcCache](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/main/java/com/ctux/ae2craftingtime/core/CpuTtcCache.java#L85) implements priority/round-robin requests, entry merging and bounded expiry. |
| D5 | `server-client-stats.md:158-160,340-341` | [Millisecond wire design](../ttc-milliseconds-instant/technical-design.md), [Forge networking](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/versions/1.20.1-forge/src/main/java/com/ctux/ae2craftingtime/mc1201/StatsNetwork.java#L33), [Fabric channels](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/versions/1.20.1-fabric/src/main/java/com/ctux/ae2craftingtime/mc1201/StatsNetwork.java#L27); both NeoForge StatsNetwork registrars declare 28. |
| D6 | `server-client-stats.md:392-396` | [Architecture UI surfaces](../architecture.md#ui-surfaces) and [optional connection contract](../optional-connection/spec.md) keep native screens available with supporting peers. |
| D7 | `player-controls-and-integrations/spec.md:60-63`, `configuration-screen/spec.md:54-56,76-79,107-116`, development `SKILL.md:43-45` | [Configuration behavior](../configuration-screen/spec.md), [suspension default](../crafting-suspension/spec.md), [FeatureOptions](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/main/java/com/ctux/ae2craftingtime/core/FeatureOptions.java#L31), and [save-before-apply](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/ServerOptionsRuntime.java#L56). |
| D8 | development `references/code-map.md:16,78`, architecture and working-with-project source-layer lists | [Dependency targets](../dependencies.md#startup-adapter-selection), [IntegrationCatalog](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/main/java/com/ctux/ae2craftingtime/core/IntegrationCatalog.java#L26), and [Forge shared source set](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/versions/1.20.1-forge/build.gradle#L76). |
| D9 | `dependencies.md:67-71`, `test-driver/spec.md:368-369` | [Production mode contract](../provider-locate/resource-icons/technical-design.md#production-acceptance-on-the-existing-fixtures) and [ResourceFixtureClient](https://github.com/cTux/ae2-crafting-time/blob/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9/shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/ResourceFixtureClient.java#L354) distinguish fixture-only, typed-key and production lifecycle checks. Visual review remains separate. |
| D10 | `working-with-project.md:51-53` | [Repository workflow](../../AGENTS.md) and [development testing workflow](../../.codex/skills/ae2-crafting-time-dev/references/testing-and-change-workflow.md) require PR-first ordering. |

## Other inspected boundaries

The native CPU mixins call the API-specific bridges; pure core code owns samples,
accuracy, state and estimates. `StatsRequestHandler` resolves live menu/grid/job
context before collection. `PacketLimits`, `PlayerRequestRateLimit`,
`StatsRequestQueue` and `RowStatsSession` provide bounded identifiers, traffic,
queues and response correlation. `CpuTtcRequestHandler` intersects the current
menu serial map with live grid CPUs. Existing core and packet tests cover these
boundaries; they were inspected, not executed in this audit.

`TimeEstimate`, `TtcEstimate`, `CraftingRowState` and `TtcText` implement current
millisecond/confidence and status presentation rules. Server option permission
adapters and `ServerOptionsRuntime` validate authority and revision before save
and live application. `IntegrationSelection` chooses a fixed compatible optional
contract without initializing addon classes; loader send paths check peer
capability. No new behavioral mismatch was established in these inspected paths.

The four release-matrix rows match Gradle modules, Java 17/21/25 toolchains and
loader-explicit dist names. Build scripts derive tasks from that matrix. Deploy
fingerprints use production inputs and retain dry-run/scoped-changelog paths.
CI runs builds and tests/JaCoCo with required Codecov upload; shared line/branch
thresholds remain 1.0. Driver source sets/JARs are separate from production.
Prepared launchers validate native loader manifests and stop identified clients.

These source observations are not new CI, runtime, upload or rendering passes.
The draft CPU-specific stats/Ponder proposals, planned Vortex work, and explicit
unfinished feature verification remain outside this correction.
