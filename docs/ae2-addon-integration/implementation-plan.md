# AE2 Addon Integration Implementation Plan

Lifecycle: see the [scope status and evidence](spec.md).

The version-selection work below records the plan implemented in PR #211;
its remaining verification is tracked in the specification. The later sections retain the
earlier native-integration roadmap; they are not evidence that every historical
phase is still outstanding. Complete this feature as one conventional commit
following `AGENTS.md`; run local tests only after the hook creates its PR.

## Versioned adapter selection

Follow [VS-01 through VS-10](spec.md#versioned-adapter-selection) and the
[design](technical-design.md#versioned-adapter-selection). No dependency upgrades,
new Minecraft targets, gameplay changes, or issue #193 implementation are part
of this work.

### 1. Capture contracts and preserve regression artifacts

- Record exact dependency versions, download provenance, hashes, target/loader,
  and relevant bytecode signatures before changing adapters. Use the current
  compatible graph and the historical evidence linked in the design.
- Retain Forge NeoEco 20.3.0, 20.4.0, and 20.4.2 fixtures, plus NeoForge 21.1.1.
  Confirm the accounting-object, long/boolean, and integer-batch descriptors
  and trace normal/FastPath calls. Identify delegate overloads to avoid observing
  the same expected-output insertion twice.
- Use the exact original and Refreshed Crafting Tree fixture pairs in the
  design: CF 7182165 / MR flhDmaU7 for Forge and CF 7182163 / MR 35O4yt0D for
  NeoForge 1.21.1. Inspect widget/node bytecode and retain each artifact's hash.
  No Fabric release was found in the checked catalogues; preserve its existing
  declaration/packaging and test absence and selector fixtures without claiming
  a positive Fabric Tree smoke. Missing retained contract or newest-adapter proof still
  prevents completing this feature.
- Recheck loader bootstrap APIs at the compatible pins and declared floors.
  Use the four target-specific bridges in the design; no shared NeoForge API
  assumption across 1.21.1 and 26.1.2.

Gate: the catalogue has evidence-backed predicates and a retained old/new
contract fixture pair for each real API transition. Artifact retrieval and checks are
verification work; the architecture and compatibility policy are already fixed.

### 2. Add selection and its smallest meaningful tests

- Implement `IntegrationSelection` and `IntegrationSelectionTest` in shared
  pure Java. Select the first compatible candidate from the immutable ordered
  list. Return a decision and bounded reasons; do not mutate the catalogue.
- Cover absent dependency, unsupported target, physical side, old-only,
  new-only, both-compatible, incompatible-new/compatible-old, neither-compatible,
  independent dependencies, and deterministic ordering. Candidate IDs and
  ownership are validated; same-family common hooks are allowed, cross-family
  ownership conflicts are not.
- Add the fixed catalogue and early bootstrap bridge/plugin. Cache a family's
  complete decision atomically across all plugin instances. Test callback order,
  concurrent/repeated queries, two Forge configs, and feature-setting/world
  changes against the same process decision.
- Check real small bytecode fixtures for missing classes/members, wrong return
  kinds, inherited members, and overlapping contracts. Exercise probe I/O and
  malformed-bytecode errors separately from normal incompatibility. Tests must
  assert the selected mixin bundle, not just repeat catalogue constants.
- Use existing JUnit and Mixin/ASM dependencies. Put loader-free probe logic in
  a testable shared seam if it needs branches; do not exclude it from coverage.
  Require 100% line and branch coverage for new or changed executable behavior
  under the repository's existing coverage policy.

### 3. Gate packaged hooks and split only the differing NeoEco dispatch

- Add `plugin` to shared pre-26, Forge AdvancedAE, NeoForge 1.21.1, and NeoForge
  26.1.2 mixin configs. Preserve their side lists and existing core hook behavior.
- Gate the two Tree variants using the fixed catalogue. Retain their distinct
  spacing, tooltip, node access, and click behavior. Do not rewrite working UI
  algorithms as part of this selection change.
- Split NeoEco's three-descriptor redirect into `NeoEcoPendingDispatchMixin`,
  `NeoEcoLongBatchDispatchMixin`, and `NeoEcoIntBatchDispatchMixin` under the
  existing `mc1201` mixin source directory. Put the network-aware dispatch
  callback behind `NeoEcoDispatchObserver`; the shared lifecycle mixin remains
  included in each variant bundle and is gated off when none is selected.
- Gate existing AdvancedAE, Lightning Tech, and Requester singleton adapters
  for the exact target/side inventory in the design. Do not expand declarations
  just because a string-target class is packaged in a shared source set.
- Add one selection/skip log per dependency and a read-only snapshot for future
  diagnostics. Keep selected hooks pending until exercised; no hook rollback,
  broad catch, new activation states in packets, or runtime reselection.

Gate: every addon-owned mixin maps to its selected family bundle; core AE2
mixins still apply. A known no-match vetoes the entire optional bundle before
application. Unexpected probe and post-application failures propagate.

### 4. Verify packaging and the actual runtime matrix

After the implementation commit's hook creates its PR, follow the applicable
development, test-driver, and prepared-client smoke skills. Build production
and driver artifacts on the host; run clients sequentially inside CodexVM.
Use English (`en_us`) for all scenarios. Smoke only the newest implemented
adapter per dependency/target; retained old fixtures receive contract and
packaging tests without extra runtime campaigns. Follow
[SP-01 through SP-04](../automated-ui-testing/spec.md#smoke-policy).

| Target | Required startup and behavior evidence |
| --- | --- |
| 1.20.1 Forge | Core-only client and dedicated server; Refreshed Tree `tree-layout`; NeoEco 20.4.2 `batched-long` normal/FastPath CPU paths; AdvancedAE extra-config absence/presence; Lightning Tech and Requester regressions; two addons together. |
| 1.20.1 Fabric | Core-only client and dedicated server; Requester regression; Tree absence and selector fixtures; Forge-only CPU variants never accepted or class-loaded. No positive Tree runtime claim without a published Fabric artifact. |
| 1.21.1 NeoForge | Core-only client and dedicated server; Refreshed Tree `tree-layout`; NeoEco 21.1.1 `batched-int` CPU; AdvancedAE, Lightning Tech, and Requester regressions; two addons together. |
| 26.1.2 NeoForge | Core-only client and dedicated server; AdvancedAE CPU regression; pre-26 Tree, Requester, NeoEco, and Lightning Tech adapters excluded. Native coverage of an installed addon does not imply a custom adapter is present. |

- Extend existing `crafting-tree-screen`, `neoeco-cpu`, `advancedae-cpu`,
  `lightningtech-cpu`, and `merequester-screen` scenarios only where they cannot assert
  newest-adapter selection, English UI behavior, or duplicate prevention. Reuse the driver
  registry and shared state machine; keep fixture content out of production.
- In CPU cases, assert selected CPU, accepted job, actual expected-output and
  returned-output amounts, a fresh sample, normal/FastPath completion, and TTC
  in the reopened plan. Assert counts/amounts so duplicate sampling cannot pass.
  In UI cases, inspect node badges/spacing, tooltips, details/reset, row totals,
  and the absence of duplicated lines or input handling.
- Use controlled development-only bytecode fixtures for both-eligible,
  incompatible-new/compatible-old, no-match, unexpected probe errors, and
  post-selection failure. Verify first-match behavior and that no fallback
  occurs after application. A synthetic overlap test proves exclusivity only;
  real artifacts must prove that the selected hook reaches the active path.
- Inspect built JAR contents, configs, refmaps, variant membership, singleton
  platform bridge, and absence of bundled addon/driver classes. Test production
  namespaces through native-loader artifact launches, not only dev classpaths.
- Run `scripts/validate-optional-integrations.ps1` and its existing regression
  script, the applicable Gradle tests/coverage and target builds, then the final
  compatible UI suite for all four targets because shared bootstrap changes
  reach all of them. Follow the smoke skill's final-base-refresh rule. Latest
  launches cannot replace the required newest-adapter smoke. Do not run direct
  older-adapter cases even when compatible pins select them; use focused newest
  fixtures instead.
- Archive exact versions/hashes, selected IDs/reasons, startup/server logs,
  per-scenario results, and visually reviewed checkpoints using
  `docs/ui-smoke-evidence.md`. Report third-party failures separately and retain
  failed attempts. Run applicable CPU behavior on dedicated servers as well as
  integrated servers; client-only Tree/Requester variants stay skipped there.

### 5. Reconcile documentation and close the acceptance gate

Update `docs/dependencies.md` and `docs/mod-automation-coverage.md` with verified
variants and artifact evidence, retaining all existing minima and optionality.
Update the startup-diagnostics inventory/snapshot consumer if #193 lands first;
otherwise keep the documented handoff. Mark this planned selection work
implemented only after the following evidence is complete.

| Acceptance | Proof owner |
| --- | --- |
| VS-AC-01 | Steps 1, 3, 4: retained artifact contracts and packaging, separate variants, newest-adapter English smoke. |
| VS-AC-02 | Steps 2, 4: selector and bytecode fixtures, exact winning bundle and no-match skip. |
| VS-AC-03 | Steps 2, 4: independent family decisions plus real multi-addon sample/UI checks. |
| VS-AC-04 | Steps 3, 4: config/packaging checks and core-only physical client/server launches on all targets. |
| VS-AC-05 | Steps 2, 4: cached decisions across config instances, callback order, world/config changes. |
| VS-AC-06 | Steps 2, 3, 4: bounded selection logs and preserved probe/runtime failure evidence. |
| VS-AC-07 | Steps 1 through 5: coverage, packaged artifacts, all retained API contracts, truthful docs and metadata. |

Done means all seven acceptance checks pass, every new executable branch is
covered, older variants retain contract coverage, newest adapters have English
runtime proof, required CI is green,
and the implementation PR contains reviewed artifact/log/screenshot evidence.
Missing required contract fixtures or newest-adapter runtime proof remain
explicit blockers;
neither a compatible pin nor a green compile can replace them.

## Development client matrix

1. Keep one compatible lock and one dynamic latest mode for every supported
   target.
2. Put the shared project list and every compatible version in
   `scripts/run-client-versions.json`; do not duplicate dependency lists in
   wrappers.
3. Reject missing compatible transitive pins and keep latest resolution errors
   visible.
4. Keep latest worlds, configs, and managed mods under `run-latest`.
5. Cover PowerShell and Bash selection, pins, dynamic versions, target
   isolation, cleanup, and invalid profile data with deterministic checks.

Done when all eight wrappers route to the correct profile and both resolver
implementations produce the same target-specific graph.

## Phase 1: Prove existing native coverage

1. For every candidate CPU, inspect whether each hooked
   `CraftingCpuLogic` method is inherited, overridden, redirected, or replaced.
2. Record the exact supported mod version and Minecraft/loader row.
3. Test OmniSequence first. Replace the current competing redirect only if an
   in-game craft proves that its inherited path misses `ProfilerBridge.start`.
4. Close candidates that already work through layer 0 without adding code.

Done when a normal output-producing craft starts, records output, finishes, and
shows TTC without duplicate samples.

## Phase 2: Spike service-level lifecycle observation

1. On one custom CPU, verify whether AE2's `CraftingService.getCpus()` returns
   it and whether submission goes through `submitJob`.
2. If it does, extract a small pure-Java busy-state tracker with tests for new,
   busy, finished, cancelled, removed, and duplicate CPUs.
3. Add thin `CraftingServiceMixin` adapters for the supported AE2 API source
   sets.
4. Do not merge the observer if it sees only vanilla `CraftingCPUCluster`
   instances already covered by layer 0.

Done when the spike proves additional real coverage without double-counting.

## Phase 3: Add only required custom CPU adapters

Work in this order because the source investigations already identify the hook
points:

1. NeoEco (`ECOCraftingCPULogic`).
2. AE2 Lightning Tech (`Ae2LtTimeWheelCraftingCpuLogic`).
3. Any later addon whose execution path still bypasses layers 0 and 1.

For each addon:

- reuse `ProfilerBridge` and the AdvancedAE adapter pattern;
- hook actual pattern dispatch, accepted output, finish, and used capacity;
- keep the mixin optional and absent-mod safe;
- add resource membership checks and the closest boundary tests;
- verify cancellation, partial output, parallel work, and a successful craft.

## Phase 4: Verify key types without addon handlers

1. Exercise each candidate `AEKey` with its real `getAmountPerUnit()` value.
2. Confirm the same normalized amount reaches profiling, estimates, snapshots,
   display, and reset lookup.
3. Add no code when the native contract works.
4. If two types produce the same profile ID, stop and plan a separate saved-data
   and packet format migration before changing `ProfileKey`.

Done when every tested key type either works through `AeKeyAmounts` or has a
specific, reproduced contract gap.

## Phase 5: Reuse AE2 UI seams

1. Check whether each candidate screen uses `CraftConfirmTableRenderer` or
   `CraftingStatusTableRenderer`, or inherits their hooked description and
   tooltip methods.
2. Verify those concrete renderer paths without new code. Do not count reuse of
   `AbstractTableRenderer` alone as TTC support.
3. Look for one stable `AEBaseScreen` method only after the table path is ruled
   out.
4. Add a bespoke `@Pseudo` mixin only for a fully custom screen or API, starting
   with the player-visible crafting screens.
5. Close candidates such as range boosters or visual tools when they have no TTC
   surface.

## Applied Journey investigation

Use the [scoped criteria](spec.md#applied-journey-investigation) and
[design](technical-design.md#applied-journey-investigation). The primary target
is `1.21.1-neoforge`, Applied Journey 0.0.1, NeoForge 21.1.252, in Prism's Codex
group, using the newer supplied archive identified in the investigation guide.
Retain the historical 21.1.249/200-entry export as a separate comparison;
mark it blocked if unavailable. Required verification of other rows follows
the eventual changed code;
this investigation does not require an unrelated four-target full smoke.

1. **AJ-01, preflight:** verify the supplied archive hash, install the exact
   export through Prism if absent, and inventory enabled/nested JAR metadata
   and overrides. Verify Java 21, guest-local staging, source fixture marker,
   and session worktree share. Build on the host after the PR exists, following
   repository ordering. Identify the historical Crafting Time artifact
   separately from the resumed graph's installed 1.2.12 baseline, and hash
   the replacement production/driver pair.
2. **AJ-01/02, reproduce:** capture ordinary Plan and Status behavior in the
   exact pack, including options, channel support, selected/observed hooks,
   logs and screenshots. Compare the same craft and settings in the five
   graphs defined in the investigation guide. Include each addon's required
   dependencies and record graph differences. Keep hypotheses separate from
   findings; report a blocked graph without substituting another version.
3. **AJ-03/05, correct:** update the design with the transformed Plan method
   ordering and its limits before editing behavior. Wrap the shared Plan and
   Status description/tooltip methods: call the original exactly once, copy
   the returned list to a mutable list, apply existing TTC decorations, and
   return the result. Set `require = 0` and `remap = false`. Preserve
   native/addon content and all original return paths; disabled features add
   no lines. Keep the test-driver observation
   outside the production wrapper so it captures the final result, including
   the separate 26.1.2 Status observer. Add focused tests for immutable input,
   content/order preservation, disabled and empty results, and original-call
   count. Do not introduce a new helper abstraction unless these methods cannot
   share the existing decoration code safely.
4. **AJ-02, controlled differential:** the paired removal of AppliedEnhancements
   1.1.0 and required OmniSequence 2.0.7 passed the standard-plan-controls
   case. That comparison alone does not identify which mod caused its result;
   bytecode separately confirms AppliedEnhancements' early Plan return
   cancellation. It does not prove full-pack acceptance. A second case
   disconnected when Thunderbolt's `CPUSelectionListStorageMixin` could not
   inject at `formatStorage` on AE2 19.2.18; case three did not run. Resolve this
   separate target mismatch before repeating affected controls. Retain the
   uncertainty around Thunderbolt's earlier Plan cancellation for specific
   entries. Keep controlled graphs separate from the unchanged full-pack run.
5. **AJ-05, dependency and target check:** the minimum supported 1.21.1 NeoForge
   version 21.1.1 supplies MixinExtras 0.3.5, which lacks `@WrapMethod`; the
   resumed 21.1.252 graph supplies 0.5.3, which supports it. Package
   `io.github.llamalad7:mixinextras-neoforge` 0.5.5 using the existing
   Jar-in-Jar pattern and verify the nested artifact, metadata, minimum loader
   requirement, and selected runtime version. Use the NeoForge flavor, not the
   similarly versioned Forge artifact. Check the existing 0.5.5 copies on
   `1.20.1-forge`/`1.20.1-fabric` and the 0.5.4 copy on `26.1.2-neoforge`
   26.1.2.71. Compile/remap the production target and start all four clients.
   Compilation alone does not prove that the runtime selected a compatible
   MixinExtras copy.
6. **AJ-03/04, full smoke:** expand `standard-ae2` from
   `scripts/ui-smoke-groups.json`; select applicable installed integrations and
   general status cases from `scripts/ui-smoke-neoforge-suite.json`. Retain the
   case list and absent/unsupported reasons. Use one schema-2 suite per graph
   with pristine state between cases. Restore the exact full graph for a clean
   final run on the reviewed source; verify real crafts and review screenshots.
7. **AJ-05, close evidence:** verify the production and exported driver observe
   the final Plan/Status descriptions and tooltips in the resumed graph. Run
   checks required by the actual correction, separately report current-head
   GitHub CI, and record artifact, revision and graph identities. Update
   dependency claims only where demonstrated. Do not mark the scope finished
   while required graphs, full-pack cases or visual evidence are missing.

Before runtime work, record planned launches, known cold-start cost and a
wall-time budget. Use bounded lack-of-progress detection. Diagnostic resumes
remain diagnostic; final acceptance requires a clean run on the final graph
and source. Collect timings and failures per actual run, close only the tested
client, and cleanly shut down CodexVM after immediate visual follow-up. Verify
its VMX is absent from `vmrun -T ws list`, including after a failed campaign.

## Applied Journey continuation gates

The renderer correction is merged in PR #669. Preserve its existing verification;
do not repeat all four client targets for dependency-only investigation.
Original AJ-04 remains blocked until its exact graph's prerequisites are
officially resolved; qualified-graph results cannot replace it or close #534.

1. **AJ-01/02:** acquire official Data 3.3.4 only for disposable qualification,
   verify its advertised SHA-256
   `69441bd856d427354f33ee16ca8ee7a1a4c15cd1691419e3c907bc1b4893f6cf`,
   actual mod/nested metadata and all present dependency ranges. Rehash the
   retained one-class Thunderbolt DEV artifact against its receipt. Keep the
   managed 196-JAR graph and historical export unchanged.
2. **AJ-02/05:** run the standalone Architectury diagnostic described in the
   design against original 13.0.11, then any exact reviewed candidate. Prove
   callback registration/removal/clear, cached and single-listener behavior,
   latch-controlled mutation, non-null concurrent invokers and dispatch outside
   the lock. Identify source/dependency closure before compiling; use no foreign
   Gradle build or new runner framework. Independently review evidence and the
   exact DEV class delta before guest installation. Retain unknown writer identity.
3. **AJ-02/03:** use existing controlled-case fixtures and launch/watch helpers
   for Core settings persistence, Data Status, Omni Status and Both Status.
   Pass `statusRelaunch=true` for both persistence phases. Verify ordinary and
   addon finite/infinite CPU row/tooltip values, final TTC results, and reviewed
   images. A new Data version invalidates affected prior graph evidence.
4. **AJ-03/04:** only after prerequisite and controlled gates pass, select the
   existing 29-case full suite on the explicitly identified qualification graph,
   preserving all unrelated mods. Start from clean fixture state and record all
   assertions, screenshots, timings and failures. This remains additional
   evidence, not an original AJ-04 pass. Keep historical unavailability explicit.
5. **AJ-01/05:** reconcile graph/artifact identities and remaining blocked
   criteria, preserve originals, and verify CodexVM shutdown. No completion
   claim while original AJ-04 or another mandatory gate remains unsatisfied.

Plan approximately 90 minutes for remaining qualification, subject to measured
progress: successful cold starts previously cost roughly 1.5–5 minutes, and
eight controlled Status/persistence launches alone can cost 20–40 minutes.
Record a separate full-suite wall budget after selection. Use one bounded
startup diagnostic with a stop-on-unchanged-failure rule; do not debug by repeated
full-pack launches. The budget is an estimate, not permission to omit acceptance.

## Final compatibility sweep

- Run required CI for every changed supported row.
- Launch each named modpack only through its matching Prism test workflow.
- Test with each optional addon present and absent.
- Check that samples are recorded once, persisted, requested, reset, and shown.
- Update `docs/dependencies.md`, loader metadata, and candidate status only for code
  and versions that were actually verified.
