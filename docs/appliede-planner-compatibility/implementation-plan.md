# AppliedE correction and verification plan

Lifecycle: see the [scope status and evidence](spec.md).

Deliver the [spec](spec.md) from the evidence and remaining limits in the
[design](technical-design.md). The startup prerequisites are already implemented
at base 4d3d818602a98922c00af7d75fc9490133e8e6a3. Do not repeat them or change
startup deadlines. This amendment does not authorize Crafting Time production
code or an external repository mutation.

## Current evidence gate

Both controls reached a usable world. The enabled result fails with an empty
plan and zero craft counters; the disabled result passes all seven existing
checks. Its plan uses eight stored cobblestone, so it proves native furnace
crafting, not EMC transmutation.

The successful raw debugger observations have been independently reviewed at the
session and line references in the design; AP-03 establishes the ordering/cache
mechanism with the documented identity qualification. The campaign's early JDB
logs instead contain failed attachment reads. Retain a redacted successful raw
excerpt with the campaign before cleanup or handoff. The upstream correction
boundary and AP-05 documentation were accepted and merged in
[PR #436](https://github.com/cTux/ae2-crafting-time/pull/436); the correction
itself has not been implemented. The instrumented enabled timeout is not clean
functional proof.

## Ordered changes and checks

| Order | Work and check | Criteria |
|---|---|---|
| 1 | Archive the reviewed successful raw debugger excerpt, control/reset provenance and the exact limits of the disabled pass. Preserve the existing failed and successful results. | AP-01 through AP-05, AP-07 |
| 2 | Completed: the reviewed three-document amendment merged in PR #436. Keep the issue open; obtain separately scoped authority before editing the Applied Enhancements repository. | AP-05 |
| 3 | At the exact upstream source revision matching the reported artifact, inspect all candidate-discovery and child-build callers. Add the smallest optional AppliedE classifier and pre-build fallback described below. | AP-05, AP-06 |
| 4 | Add upstream behavioral regression coverage for each build entry, nested candidates, fallback propagation and unaffected ordinary patterns. Follow that repository's applicable build/test rules. | AP-06 |
| 5 | Repair the development-only AppliedE fixture so the final EMC assertion cannot pass using a conventional recipe. Update its existing test/result boundaries and dependency coverage documentation truthfully. | AP-06, AP-07 |
| 6 | After the relevant implementation PRs exist, run targeted tests/coverage and compile affected driver consumers. Then perform the focused corrected runtime checks below. | AP-06, AP-07 |
| 7 | Review actual corrected artifacts, CI and runtime evidence. Report upstream delivery state separately; the disabled baseline does not close #420. | AP-05 through AP-07 |

The observed mechanism selects the upstream correction boundary; it does not
validate an unimplemented correction or establish successful EMC accounting.

## Upstream change boundary

Before any AELIS compiler call to molecularmanipulator$buildChildPatterns, query
the existing crafting service for the occurrence's raw candidate patterns. A
candidate whose runtime class name is exactly
gripe._90.appliede.me.misc.TransmutationPattern selects the stable
request_ordered_pattern fallback before that occurrence's children are built.
Do not initialize an AppliedE child merely to discover its pattern type.

Cover all three paths in the inspected compiler: inspect,
isTerminalQuantityFeedbackOccurrence and validateOccurrence. Propagate the
existing typed fallback to the session boundary; the terminal-feedback helper's
RuntimeException catch must not consume it and resume compilation. Preserve
emitter handling and ordinary-pattern behavior. Nested occurrences need the
same guard, including when their ordinary parent has already been inspected.

Use an exact class-name comparison without a required AppliedE class reference,
class loading, dependency range change or generic extension framework.
Installations without AppliedE must continue to work. Reuse the upstream fallback type
and session/native routing instead of adding a parallel planner or rewriting
third-party bytecode in Crafting Time.

Tests must execute the decision at the real compiler boundary: request-ordered
candidates never invoke their build bridge, keep their child list uninitialized
and yield the stable fallback; an ordinary or absent AppliedE candidate retains
normal compilation. Cover mixed ordinary/AppliedE candidate lists, a nested
request-ordered child, all three guarded entries and fallback propagation.
A classifier-only test does not prove the fix. Keep native positive-output
validation unchanged.

## Unambiguous EMC proof

Read AppliedE's actual extraction/accounting path before choosing the assertion.
The current fixture grants furnace knowledge and EMC but leaves the ordinary
furnace recipe available. Preserve the original mixed-recipe case as a planning
regression, and create an isolated EMC-only state for the accounting check.

Use the existing disposable fixture APIs to remove the competing furnace pattern
and conventional input supply from that isolated state. Verify that the
Transmutation Module is active, owns the intended player's knowledge and
advertises the requested furnace. Sample its real authoritative EMC balance
after setup and immediately before submission; compute the expected positive
charge from the installed ProjectE value for one furnace. After completion,
require exactly that net debit and exactly one returned furnace. Check that
neither a stored furnace nor another crafting provider could satisfy the order.

Reuse the current dispatch, completion, profile and TTC checks, adding the
accounting assertion at the existing test-driver result/validation boundary.
Do not call a generic craft counter an EMC observation. Keep decisions in covered
shared logic and preserve server-thread ownership. Inspect AppliedEFixture's
Forge 1.20.1 and NeoForge 1.21.1 consumers and their API differences; compile both
if the common fixture changes. Use a Forge-only adapter if the APIs require it,
without inventing a NeoForge compatibility claim.

After the driver implementation PR exists, compile
`:mc_1_20_1_forge:compileTestDriverJava` and
`:mc_1_21_1_neoforge:compileTestDriverJava` when the shared fixture changes.
Run the focused Forge tests for the added pure accounting/validation decisions,
with unchanged line/branch coverage requirements; name those exact test methods
in the executable change before running them. Neither this document review nor
the earlier result JSON substitutes for those checks.

## Focused runtime proof

Use Project Infinity 0.0.52.0, Minecraft 1.20.1, Forge 47.4.20 and Java 17.
Stage a new marked guest-local Codex-group copy through the existing named-pack
workflow. Compare every non-Crafting-Time JAR with the archived graph, allowing
only the explicitly identified upstream correction artifact. Record its source
SHA and hash, plus current Crafting Time production/driver identities.

Keep automatic AELIS, long-range crafting and native safety enabled. Preserve
8192 MiB heap, guiScale:0, pauseOnLostFocus:false and validated absolute
forward-slash driver paths. Run the original mixed-recipe scenario and the
isolated EMC-only case sequentially with pristine resets. They may share one
launch if the existing fixture lifecycle supports it. Require the fallback
reason, valid requested/advertised/rebuilt amounts, native acceptance and the
AP-06 output/accounting/profile assertions.

Read-only breakpoint evidence may establish ordering, but final functional
proof runs cleanly without debugger suspension. Retain the earlier enabled
failure and disabled baseline; do not relabel them as corrected runs. Budget
one corrected launch initially and 45 minutes for staging, startup, focused
checks and evidence, then use measured phase times to assess any retry.
The campaign already reached usable worlds; it is not permission to extend
driver deadlines or replay full matrices.

## Review and completion

For these documents, inspect links, facts, criterion coverage and whitespace;
no repository test is needed. Executable checks follow the applicable
repository's hook-created PR ordering. Apply unchanged coverage rules to new
logic and report excluded Minecraft adapters separately.

Follow the [archive contract](../ui-smoke-evidence.md): preserve distinct control
configuration and inventory receipts, pristine-reset provenance, raw debugger
output, results, screenshots/sidecars, exact-client exit and timings. Do not use
the overwritten top-level stage-readback file as the enabled receipt. Review
required images and keep private paths/account details out of published reports.

AP-06 remains open until corrected AELIS-enabled EMC proof passes. AP-07 requires
the complete retained evidence and actual exit/visual review, not just result
JSON. An upstream patch's publication, local diagnostic build and Crafting Time
PR are separate states; report each truthfully before deciding issue closure.
