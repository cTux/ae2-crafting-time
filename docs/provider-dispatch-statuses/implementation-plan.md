# Provider dispatch statuses: implementation plan

Implement only after approval of the [spec](spec.md) and
[technical design](technical-design.md). This plan does not authorize a release
or changes to upstream crafting behavior.

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
- Test AC-01 through AC-06 in the existing test setup: all agreeing alternatives,
  successful alternate provider/side, mixed causes, unvisited/busy/unknown
  alternatives, missing inputs, budget/power short-circuits, nested calls,
  exceptions, shared outputs, repeated batches, tick rollback, and cleanup.
  Add no test framework and do not weaken JaCoCo requirements.

## 3. Extend the existing packet and UI

- Append the three enum values; keep old enum positions stable. Advance all
  four wire boundaries as documented. Reuse StatsPacketCodec, snapshot wrappers,
  request handling, and CPU-scoped cache replacement.
- Extend packet/cache tests for each new reason, empty/max/oversized collections,
  unknown enum, invalid/unrequested keys, context mismatch, no samples, omitted
  values, and old/new peer boundaries (AC-06, AC-08).
- Add English/Ukrainian labels and two-line tooltips, plus the mixed-row
  qualifier. Extend badge recognition and test priority, stored-only exclusion,
  sorting as unknown, neutral TTC color handling, and unchanged total TTC
  behavior (AC-05, AC-08). Keep the plan and addon-only UI surfaces unchanged.

## 4. Prepare real focused smoke scenarios

- Extend the existing DispatchStatusFixture/NoProviderScenario/NoPowerScenario
  patterns in the shared 1.20.1-era and 26.1.2 driver boundaries. Add scenario
  names `no-target`, `input-blocked`, and `locked` to runner selection and the
  applicable coverage matrix. Use actual crafting requests and server state,
  not injected status maps or hand-drawn labels.
- NO TARGET: disconnect the only usable target, observe the scheduled label,
  restore it, and prove dispatch/completion. Cover an alternate usable side.
- INPUT BLOCKED: exercise both blocking mode and zero-acceptance rejection;
  clear each condition and prove recovery. Include a partial-acceptance success
  case that queues leftovers without showing INPUT BLOCKED.
- LOCKED: exercise active high/low redstone, pulse, and result-return locks;
  unlock each through normal provider behavior and prove recovery. Include a
  configured-but-inactive lock and a healthy alternate provider.
- For each new label, prove no-data rendering, tooltip visibility, bounds,
  sorting, and mixed active/pending explanation. Check CPU switch isolation and
  regress NO PROVIDER, NO POWER, NO SPACE, Waiting, DELAYED, and numeric TTC.
- Native CPU scenarios run on all four targets; AdvancedAE direct behavior
  runs on Forge 1.20.1 and both NeoForge targets. Check optional absence and
  custom-path fallback without claiming new support for those paths (AC-07).

## 5. Deliver and verify

- Self-review source, tests, docs, and coverage changes together. Keep one
  conventional implementation commit. Run no local tests before the hook has
  created the PR; GitHub CI and local runtime evidence are separate gates.
- After PR creation, run only checks required by the applicable skills. Cover
  all changed core branches and packet/contract boundaries across all four
  modules. Preserve every retained adapter with contract/packaging checks.
- Run the focused scenarios above using the prepared-client workflow, building
  on the host and running clients sequentially in CodexVM. Exercise the newest
  implemented adapters only, in English. Keep each target/profile/scenario's
  result.json, screenshots, logs, dependency manifest, and adapter identity.
  Review screenshots for clipping, overlap, style, and tooltip readability.
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
