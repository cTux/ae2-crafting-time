# Recurrent crafting status: implementation plan

Status: ready for implementation after this planning PR merges. The current PR
contains documentation only. Follow the [spec](spec.md) and
[technical design](technical-design.md); leave the feature issue open until the
implementation and its verification are complete.

## 1. Capture recurrence where AE2 rejects recipes

Own pure-Java classification/aggregation under `shared/src/main/java` and thin
AE2 carriers/mixins under `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201`.
Verify the exact buildChildPatterns, request/addMissing, runCraftAttempt, and
CraftingPlan descriptors in the four target artifacts before writing hooks.
Use mc1201/mc2612 overrides only when those signatures differ.

Implement the design's rejected-candidate observation, no-eligible-child rule,
per-attempt reset, immutable plan attachment, and final positive-missing
intersection. Preserve AEKey variant identity. Do not scan the network graph,
change an AE2 return value, or touch profiler persistence.

Write focused coverage under `shared/src/test` for every classification branch:
no candidates, only rejected candidates, an eligible alternative, zero missing,
nonsimulated success, mixed contributions, reset, and independent calculations.
Add AE2-facing boundary tests in the existing shared Minecraft test source set
for self/two/three-node loops, exact variants, storage, emission, substitutes,
and CRAFT_LESS. Assert the original plan quantities/results remain unchanged.

Completion evidence: a simulated plan carries only proven recurrent missing
keys; successful, discarded, and other concurrent plans cannot inherit them.

## 2. Send evidence with its native plan

Own a CraftConfirmMenu mixin and transient summary-entry carrier in mcCommon,
the shared payload codec beside existing `mc1201/net` codecs, and loader glue in
each `versions/*/src/main/java/com/ctux/ae2craftingtime/mc1201/StatsNetwork.java`
and corresponding packet source set. Reuse the installed networking libraries.

Implement summary revisions, clear-on-setPlan, positive-summary intersection,
256-row chunks, and all identity/bounds/bit checks from the design. Associate
flags with original entry objects, not sorted indices or profile ids. Register
the message and update channel/registrar versions together on every target;
check Fabric capability before send. Do not modify AE2 packet bytes.

Extend the nearest packet tests for empty evidence, 1/256/257 rows, the final
partial chunk, duplicate chunks, negative and overflow bounds, wrong counts,
invalid trailing bits, stale/future revisions, wrong or closed menus, new
plans with identical contents, two players/networks, cancellation and disconnect.
Prove native setPlan runs before its mod chunk on each loader's client executor.
Assert no packet-controlled count allocates an arbitrary-sized collection.

Completion evidence: all four targets receive only their current plan's flags;
invalid or unsupported diagnostics leave AE2's plan intact.

## 3. Render and translate the row status

Own `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmTableRendererMixin.java`,
the current TtcText source variants, and shared English/Ukrainian language files.
Add small version adapters only where AE2's Component construction differs.

Replace only the native Missing label component for a flagged positive-missing
row. Keep AE2's amount/unit formatting, add the exact tooltip explanation once,
and handle craftAmount == 0. Preserve other descriptions and tooltip lines.
Confirm that AbstractTableRendererMixin does not treat Recurrent as a TTC badge
or recolor it. Do not edit TTC sorting for this feature.

Cover flag/amount/enabled predicates, exact locale keys and placeholders, red
normal-weight style, and unflagged descriptions. Verify sorted entry identity,
no learned samples, mixed rows, large amounts, narrow UI and Ukrainian text.
Update relevant plan/player docs and `docs/dependencies.md` with the implemented
native-terminal coverage; coordinate status-guide issue #305 without expanding
this change into a guide chapter implementation.

Completion evidence: the row and tooltip follow R1-R7 while existing quantities,
TTC, and other screens retain their behavior.

## 4. Commit, CI, and runtime verification

Use `implement-planned-feature` for the later code task, with development,
test-driver, and prepared-client smoke skills where applicable. Implement and
self-review before creating one conventional feature commit. Follow AGENTS.md:
verify/setup the post-commit hook, let it push and create the PR, then run local
checks. Do not run local tests before PR creation.

After PR creation, run the required shared/boundary tests and JaCoCo; every new
or changed executable branch needs the development skill's 100% line and branch
coverage. Build all four release-matrix targets through the repository build
workflow. Check translations, effective mixin registrations, and diff hygiene.
Read GitHub CI and review findings separately from local results.

Extend the existing standard-AE2 test-driver path with a focused recurrent-plan
scenario on each target. Use real encoded processing patterns and actual plan
menus, not synthetic cached flags. A useful fixture uses inert distinct item
types A/B/C and deliberately circular processing patterns; no machine output is
needed for a failed plan. Create separate seeded and valid-alternative fixtures
and assert that AE2 really finds a successful plan before using them as negative
controls. Run prepared-client smoke in the dedicated VM through the smoke skill.

| Target | Required observed evidence |
| --- | --- |
| 1.20.1 Forge | A/B and self loops, ordinary/alternative controls, row/tooltip screenshots, packet ordering and lifecycle. |
| 1.20.1 Fabric | Same focused scenario and capability fallback, with remapped mixins verified. |
| 1.21.1 NeoForge | Same focused scenario, three-node loop, multiplayer isolation, English/Ukrainian and TTC sort checks. |
| 26.1.2 NeoForge | Same focused scenario on its rendering/packet APIs, row/tooltip and lifecycle screenshots. |

Also exercise a plan larger than one diagnostic chunk and exact item variants.
Test logical-server handling in integrated play and a dedicated-server session;
two players must see only their own current plans. Capture both the red text and
its hover explanation. A successful screenshot alone does not certify negative
controls or packet isolation.

## Completion gate

R1-R7 each have passing automated or actual runtime evidence as mapped in the
design. All four builds and required CI checks are green, review blockers are
resolved, both translations match, and the implementation PR links the issue
and these documents. Record tested artifact versions and evidence locations.
Do not claim unrun smoke coverage or publish a release as part of implementation.

For this documentation-only PR, the gate is narrower: self-review the three
documents together, validate relative/source/issue links and Markdown structure,
run `git diff --check` after the hook opens the PR, inspect the rendered GitHub
documents, wait for green CI and review readiness, then merge and verify all
three issue links resolve on master. No runtime feature or smoke result is
claimed by the planning merge.
