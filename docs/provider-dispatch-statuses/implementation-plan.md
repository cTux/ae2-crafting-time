# Provider dispatch statuses: implementation plan

Lifecycle: see the [scope status and evidence](spec.md).

Implement only after approval of the [spec](spec.md) and
[technical design](technical-design.md). This plan does not authorize a release
or changes to upstream crafting behavior.

Refreshed 2026-09-06 against `51edf0f8f531c8f12fe4e192f934669359011390`.
The [research findings](technical-design.md#repository-changes-since-the-original-plan)
identify the changed code. This remains future implementation work; this
documentation refresh does not claim feature tests or runtime smoke passed.

## 1. Verify contracts and establish the branch

- Refresh the implementation base, read AGENTS and the development, planned
  feature, test-driver, and prepared-client smoke skills. Follow the branch and
  single conventional commit workflow; initialize the repository hook through
  `scripts/setup-git.ps1` when not already configured.
- Inspect compiled native and AdvancedAE executeCrafting contracts and all
  four pinned provider implementations. Record exact method descriptors,
  pattern-local capture, provider iteration, all exits, and API-specific target
  lookup signatures before adding hooks. Inspect callers and optional mixin
  interactions, especially lookup replacement and push overrides.
- Start from the existing `Iterable.iterator()` redirect in the native
  `mcCommon` mixin and the AdvancedAE `neoforge` mixin (also included by Forge's
  build). Preserve `observeProviders` and `ProviderStartTracker.noteDispatch`.
  Prove full traversal by observing normal iterator exhaustion; do not
  materialize or re-enumerate addon iterables to discover alternatives.
- Contract mismatch must be resolved through the existing versioned adapter
  policy, not by guessing local ordinals or weakening required injection counts.
  Keep the four release targets and supported dependency minima unchanged.

## 2. Implement observation and aggregation

- Extend `CraftingCpuLogicMixin` and `AdvancedCraftingCpuLogicMixin` with exact
  pattern evaluation boundaries, candidate accounting, busy-result observation,
  and a try/finally-scoped provider invocation context. Call original methods
  exactly once and preserve exceptions, arguments, results, and order.
- Add provider observation mixins at actual lock/target/blocking/simulation
  seams, split only where version contracts require it. Register them in each
  relevant target configuration. Do not implement a second dispatch algorithm.
- Add `ProviderDispatchTracker` in shared core and wire both ProfilerBridge
  variants plus CraftProfiler. Track by CPU identity and exact pattern, clear
  on success/unknown/lifecycle changes, expire after 20 ticks, and merge with
  existing statuses using explicit precedence.
- In both bridges, whitelist only NO PROVIDER and NO POWER in the current
  `rememberStatus` conversion. Never map a new reason to NO PROVIDER and never
  extend `StatusKind`/saved tags for these transient observations. Preserve
  `rememberedReasons(scope)` and the recovery fix from #284. Add regressions
  for each new reason through snapshot, notifier read, save, and restore;
  assert that it neither survives nor creates a remembered alias (AC-06).
- Keep diagnostic cleanup separate from throughput `start`/`complete` and
  sample resets. Regress live interval learning from #287, provider-start
  records, and delayed plate recovery with active and scheduled work together
  (AC-05). Unknown or expired dispatch evidence must not erase those systems.
- Test AC-01 through AC-06 in the existing test setup: all agreeing alternatives,
  successful alternate provider/side, mixed causes, unvisited/busy/unknown
  alternatives, missing inputs, budget/power short-circuits, nested calls,
  exceptions, shared outputs, repeated batches, tick rollback, and cleanup.
  Add no test framework and do not weaken JaCoCo requirements.

## 3. Extend the existing packet and UI

- Append the three enum values; keep old enum positions stable. Advance all
  four wire boundaries as documented. Reuse StatsPacketCodec, snapshot wrappers,
  request handling, and CPU-scoped cache replacement.
- At this baseline, use Forge 16, Fabric `stats_snapshot_v9`, and NeoForge 15
  on both targets. Preserve sample amounts and server total TTC in the current
  snapshot layout. Recheck the base's identifiers immediately before editing.
- Extend packet/cache tests for each new reason, empty/max/oversized collections,
  unknown enum, invalid/unrequested keys, context mismatch, no samples, omitted
  values, and old/new peer boundaries (AC-06, AC-08).
- Add English/Ukrainian labels and two-line tooltips, plus the mixed-row
  qualifier. Extend badge recognition and test priority, stored-only exclusion,
  sorting as unknown, neutral TTC color handling, and unchanged total TTC
  behavior (AC-05, AC-08). Keep the plan and addon-only UI surfaces unchanged.
- Keep compact tooltips as updated in #288: no accuracy rows return. Existing
  locate clicks remain available; the new statuses add no chat, locate records,
  or red-plate transitions. Check the shared renderer's existing click/tooltip
  ordering rather than replacing it.

## 4. Prepare real focused smoke scenarios

- Extend the existing DispatchStatusFixture/NoProviderScenario/NoPowerScenario
  patterns in the shared 1.20.1-era and 26.1.2 driver boundaries. Add scenario
  leaf names `no-target-status`, `input-blocked-status`, and `locked-status`,
  matching the current `no-provider-status`/`no-power-status` naming. Register
  them in `SuitePlan`, both `TestDriverRuntime` variants, runner selection,
  coverage and evidence requirements, and `scripts/ui-smoke-groups.json` and
  `scripts/ui-smoke-impact.json`. Update planner/coverage tests so shared status
  changes select these leaves on all four targets. Use actual crafting requests
  and server state, not injected status maps or hand-drawn labels.
- NO TARGET: disconnect the only usable target, observe the scheduled label,
  restore it, and prove dispatch/completion. Cover an alternate usable side.
- INPUT BLOCKED: exercise both blocking mode and zero-acceptance rejection;
  clear each condition and prove recovery. Include a partial-acceptance success
  case that queues leftovers without showing INPUT BLOCKED.
- LOCKED: exercise active high/low redstone, pulse, and result-return locks;
  unlock each through normal provider behavior and prove recovery. Include a
  configured-but-inactive lock and a healthy alternate provider.
- Pulse fixtures use AE2 15's PULSE behavior on 1.20.1 and the high-to-low-to-high
  sequence when initially powered on AE2 19/26. Result locks must return the
  result through the provider's normal return path, not just inject it into ME
  storage or directly reset the lock field (AC-03).
- For each new label, prove no-data rendering, tooltip visibility, bounds,
  sorting, and mixed active/pending explanation. Check CPU switch isolation and
  regress NO PROVIDER, NO POWER, NO SPACE, Waiting, DELAYED, and numeric TTC.
- Native CPU scenarios run on all four targets; AdvancedAE direct behavior
  runs on Forge 1.20.1 and both NeoForge targets. Check optional absence and
  custom-path fallback without claiming new support for those paths (AC-07).
- Make the three new fixtures accept native/AdvancedAE CPU setup through the
  existing addon fixture support, and record direct new-status assertions for
  each of the three AdvancedAE targets. `advancedae-cpu` startup/throughput
  evidence alone does not prove these statuses. Keep suite terminal-screen
  waits and world teardown/reentry handling from #278/#281 intact.

## 5. Deliver and verify

- Self-review source, tests, docs, and coverage changes together. Keep one
  conventional implementation commit. Run no local tests before the hook has
  created the PR; GitHub CI and local runtime evidence are separate gates.
- After PR creation, run only checks required by the applicable skills. Cover
  all changed core branches and packet/contract boundaries across all four
  modules. Preserve every retained adapter with contract/packaging checks.
- Review `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`,
  then run the required changed-scope selection and archive `selection.json`.
  Ensure the explicit four-native/three-AdvancedAE status/recovery matrix above
  is covered; a planner-selected subset or generic addon pass does not replace
  that acceptance gate. New leaves must be registered before selection runs.
- Run the focused scenarios above using the prepared-client workflow, building
  on the host and running clients sequentially in CodexVM. Exercise the newest
  implemented adapters only, in English. Keep each target/profile/scenario's
  result.json, screenshots, logs, dependency manifest, and adapter identity.
  Review screenshots for clipping, overlap, style, and tooltip readability.
- Include save/reopen proof that new transient reasons do not reappear as
  remembered NO PROVIDER, plus existing provider recovery, live-learning, total
  TTC, compact tooltip, and locate/plate regressions. Do not run a full modpack
  merely because those shared boundaries changed.
- Keep Ukrainian verification static (keys, placeholders, meaning), not a
  second runtime campaign. No unrelated full-modpack smoke campaign is required.
- Confirm production JARs exclude driver classes/resources; confirm core-only
  and dedicated-server startup do not load optional/client classes incorrectly.
- Run documentation/link checks and git diff --check. Read back PR/CI state;
  report actual results and any blocked gate without calling it a pass.

## Completion gate

AC-01 through AC-08 pass; all four native target runs and the three applicable
AdvancedAE runs have direct new-status and recovery evidence; newest-adapter
identity and English screenshots are recorded; older variants retain contract
coverage; translations and protocol boundaries agree; required CI is green;
and no known repository-owned warning or false-positive classification remains.
Do not merge or publish a release without the corresponding user authorization.

## Warning-tooltip controls correction (#437)

This is the bounded follow-up for [#437](spec.md#planned-warning-tooltip-controls-correction-437).
The original feature steps above are historical context, not work to repeat.
Implement only after this correction's three documents are merged.

1. Correct final tooltip assembly in the shared status renderer. Append the
   locate/details/reset section once after every warning body, preserving
   non-warning behavior and existing action eligibility (W437-1, W437-2, W437-3).
2. Add final-composition regression checks in the existing shared Minecraft
   test source set, with covered core tests if new pure decisions are needed.
   Assert exact hint suffix/order, unchanged body, no duplicates, mixed-row
   qualifier placement, no-sample warnings, all six block reasons, NO SPACE,
   DELAYED, and ordinary/empty-row exclusions. Keep both locale key checks and
   every existing coverage gate (W437-1 through W437-4, W437-6).
3. Extend the existing input-blocked and no-space scenario assertions and the
   native NO SPACE counterpart to observe the final hints. Reuse existing
   driver input actions on an eligible warning row to verify all three controls
   against its output, preserving the stored-only NO SPACE rejection. Keep
   captures before actions that close the screen or reset samples. Cover row
   targeting after sorting/scrolling through existing controls fixtures
   (W437-4, W437-5).
4. After the hook creates the implementation PR, run the focused regression
   checks, shared coverage, and affected target compilation. Review
   `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly` before
   building or launching. The baseline renderer rule selects
   `standard-status-controls`, `waiting-status`, `running-status`,
   `delayed-status`, `no-space-status`, `no-provider-status`, `no-power-status`,
   `no-channel-status`, `no-target-status`, `input-blocked-status`,
   `locked-status`, and `craft-lifecycle` on all four targets. Editing `TtcText`
   additionally selects `recurrent-plan` and `standard-plan-controls`; review
   the actual plan rather than assuming this list is exhaustive (W437-6).
5. First prove INPUT BLOCKED and NO SPACE on prepared compatible Fabric 1.20.1
   with Java 17, then run the complete required changed selection sequentially
   in CodexVM. Use the newest implemented adapters and English runtime text;
   validate Ukrainian statically. Archive screenshots/sidecars, result checks,
   dependency/artifact identities and timings at the tested PR SHA. Review the
   complete tooltips for fit and all required captures before accepting them.
   A focused pair alone does not satisfy the selected matrix (W437-5, W437-6).

### Verification prerequisites and completion

The investigation verified host JDKs 17/21/25, the Gradle 8.12 wrapper, a running CodexVM,
working SSH, matching guest JDKs, all four compatible native launch manifests,
loader entry classes and asset directories. The tracked source-fixture markers,
disposable-copy/reset flow and existing launch/evidence/cleanup scripts exist.
No new verification framework is needed. Recheck the exact worktree share,
manifest/dependency match and fixture marker before staging; build only on the
host and launch only guest-local disposable clients. Three manifests reference
absent version-named wrapper JARs while their actual loader entry classes are
present elsewhere on the classpath; this is not a proven launch failure and
must be assessed by normal preflight, not treated as a recorded smoke pass.

Budget launches and wall time from the reviewed selection. Record measured
startup costs, use existing progress deadlines, and stop on an owned failure
before expanding the campaign. Final proof must use a clean run of the current
head; diagnostic retries do not replace it. No client was launched during this
investigation, so runtime costs and success remain unmeasured.

Complete when W437-1 through W437-6 have current-head evidence, required CI
passes, and the reviewed diff preserves the stated compatibility boundaries.
The original dispatch implementation's dedicated-server, persistence and
AdvancedAE dispatch campaign is not a new requirement for this tooltip-only
correction; any cases required by current changed-selection policy still run.
