# Exact ingredient mismatch diagnostics: implementation plan

Status: ready for implementation after this planning PR merges. The current PR
contains documentation only. Follow the [specification](spec.md) and
[technical design](technical-design.md); leave issue #327 open until the feature
and its verification are complete.

## 1. Classify exact-key near-matches

Own `PlanStoredVariantDetector` and focused tests in the shared Minecraft source
sets. Verify `CraftingPlanSummary`, `CraftConfirmMenu`, `AEItemKey`,
`getAvailableStacks`, and `getPrimaryKey` descriptors in AE2 15.0.10, 19.0.24,
and 26.1.10-beta before writing hooks.

Implement one available-stack snapshot, positive-item filtering, exact-key
inequality, same-primary-item matching, and original summary-row indices. Reuse
an implemented #320 plan-diagnostic carrier if present; otherwise add only the
transient carrier required by this feature. Do not serialize item data to text,
inspect mod fields, extract storage, or alter the plan.

Cover same item with different NBT/components, exact equality, different item
with the same display name, positive/zero availability, missing/zero-missing,
multiple variants, unrelated rows, fluids, and empty storage. Assert inputs are
unchanged and the scan occurs once.

Completion evidence: only positive missing item rows with a current stored
same-item/different-key candidate return their original row index.

## 2. Bind evidence to the current native plan

Own the `CraftConfirmMenu` and `CraftingPlanSummaryEntry` carriers in mcCommon,
the shared chunk validator/codec, `PlanStoredVariantsS2C` wrappers, and
`StatsNetwork` registration in all four version modules. Add version adapters
only where inspected signatures differ.

Implement server/client summary revisions, clear-on-native-summary behavior,
256-row bit-mask chunks, per-player delivery, and every identity/bounds/trailing
bit check in the design. Increment Forge and NeoForge protocol/registrar
versions; add the Fabric receiver-support guard. Keep AE2 packets unchanged.

Extend the nearest packet and lifecycle tests for 1/256/257 rows, final partial
chunks, duplicate chunks, wrong container/revision/count, negative/overflow
bounds, invalid trailing bits, early/late packets, identical replacement plans,
closed menus, disconnect, two players, and two networks. Prove native summary
installation precedes the diagnostic handler on every loader.

Completion evidence: only the intended player's current plan entries receive
flags, and malformed/unsupported data leaves the native plan intact.

## 3. Render the warning and guidance

Own `CraftConfirmTableRendererMixin`, the existing `TtcText` source variants,
and both shared language files. Append the warning line and two exact tooltip
sentences before TTC's missing-only early return. Keep AE2's missing text,
quantities, row identity, sort comparators, and TTC formatting unchanged.

Cover flag/key/amount/enabled predicates, exact locale keys, gold normal-weight
style, one warning for multiple variants, missing-only rows, all sort modes,
no-sample plans, and coexistence with #320 when that feature is present. Verify
narrow layouts and both languages through the existing UI observation driver.

Completion evidence: V1-V6 render on native craft-confirm screens without
changing the plan or other UI surfaces.

## 4. Commit, CI, and runtime verification

Use `implement-planned-feature` for the later code task and the test-driver and
prepared-client smoke skills for UI evidence. Implement and self-review before
one conventional feature commit. Follow `AGENTS.md`: let the post-commit hook
push and create the implementation PR before local tests.

Run focused detector/codec/rendering tests and JaCoCo with 100% line and branch
coverage for new executable branches. Build all four release-matrix targets,
check translations and effective mixin registrations, and read GitHub CI
separately.

Add a standard-AE2 test-driver scenario using two stacks of one item with
distinct exact data. Encode a processing chain for the expected key, store only
the other key, open the real confirmation screen, and capture the warning and
tooltip. Negative controls must cover the exact key, different base item,
ordinary shortage, and successful plan. Repeat the boundary on Forge/Fabric
1.20.1 and both NeoForge targets; include multiplayer isolation, replan,
sorting, disconnect, and English/Ukrainian checks. A screenshot alone does not
prove negative controls or packet isolation.

## Completion gate

V1-V7 each have passing automated or runtime evidence, all four builds and
required CI checks are green, review blockers are resolved, and the
implementation PR links issue #327 and these documents. Record tested artifact
versions and evidence locations. Do not publish a release in that task.

For this documentation-only PR, self-review the three documents together,
validate relative/source/issue links and Markdown structure, run
`git diff --check` after the hook opens the PR, inspect the rendered GitHub
documents, wait for green CI and review readiness, then merge and verify all
three issue links resolve on the default branch.
