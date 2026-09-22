# AppliedE ordering failure and correction boundary

Lifecycle: see the [scope status and evidence](spec.md).

Current evidence base: 4d3d818602a98922c00af7d75fc9490133e8e6a3. Read the
[spec](spec.md) and [plan](implementation-plan.md) together. The observed
pre-request child mutation belongs to the Applied Enhancements/AppliedE planner
interaction. The correction is assigned to AELIS's early inspection, with no
Crafting Time production workaround.

## Original failure and dependency identity

The original archive is
20260914T101445Z-f5bb/archives/20260914T130501504Z-b8bbd4af under the
project-infinity-0.0.52 modpack archive. Its forge-exact-fixed-retry graph is the
reported Project Infinity 0.0.52.0 release. At 13:03:16 UTC on 2026-09-14, the
uninstrumented log reports furnace amount=1, aggregatedRequest=1 and
invalid_pattern_output, followed by a positive-output rejection of matching
total zero in both AELIS's native boundary and fallback. The later PLAN_STABLE
timeout and process exit zero do not change the failure.

| Retained file in that graph | SHA-256 |
|---|---|
| run/evidence/latest.log | 2f714811c2d4988ab404a917e591ddbf23752e3e1fb83cd93fb7e8a2c7559b22 |
| run/evidence/appliede-cpu/result.json | a911c1f4993d15df0693943d0bef9266ac47322e0f37ed2aef77d7ba4fd5f980 |

| Exact dependency | SHA-256 |
|---|---|
| AppliedE 0.14.7-fix2 | 832e8b3872ca90c3d1a917bdcbb3c09d7c809b2edeeec6295227f4f287f5d29e |
| Applied Enhancements 1.0.7-forge | 37b52c6938b9ebd8d4b8c5780c0c89d485ab58abb58beae8a5e7c96f36fbf5a9 |
| OmniSequence 2.0.3-fix | 7c0e12290ed104401719efc174be2867006fb557104978fed577a5b87007a875 |

## Source flow and ownership

1. AppliedE's EMCModulePart delegates to
   [KnowledgeService](https://github.com/62832/AppliedE/blob/b57292fdd2478d5115a74158d5c319456b91a6b1/src/main/java/gripe/_90/appliede/me/service/KnowledgeService.java),
   which advertises known items as TransmutationPattern(item, 1).
2. Its [node mixin](https://github.com/62832/AppliedE/blob/b57292fdd2478d5115a74158d5c319456b91a6b1/src/main/java/gripe/_90/appliede/mixin/crafting/CraftingTreeNodeMixin.java)
   captures quantity at request HEAD and rebuilds transmutation patterns during
   buildChildPatterns with that field, whose default is zero.
3. [AELIS's calculation wrapper](https://github.com/AyaYumi/AppliedEnhancements/blob/b2c4dbd9d29bcafb527e85a08c0e07879d7ce2d8/src/main/java/com/appliedenhancements/mixin/AelisCraftingCalculationMixin.java)
   tries AELIS before the native request. Its
   [compiler](https://github.com/AyaYumi/AppliedEnhancements/blob/b2c4dbd9d29bcafb527e85a08c0e07879d7ce2d8/src/main/java/com/github/appliedenhancements/crafting/aelis/AelisPlanner.java)
   builds children at three sites: inspect, isTerminalQuantityFeedbackOccurrence
   and validateOccurrence.
4. [AE2 15.4.10](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/forge/v15.4.10/src/main/java/appeng/crafting/CraftingTreeNode.java)
   initializes children only when nodes is null, retaining the same processes on
   later calls. Its request divides demand by the matching output count.
5. [Applied Enhancements' safety check](https://github.com/AyaYumi/AppliedEnhancements/blob/b2c4dbd9d29bcafb527e85a08c0e07879d7ce2d8/src/main/java/com/appliedenhancements/mixin/CraftingTreeProcessLongSafetyMixin.java)
   unconditionally requires that count to be positive. Preserve this check.

Crafting Time's shared
[CraftingTreeNodeMixin](../../shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingTreeNodeMixin.java)
preserves the native notRecursive result and delegates addMissing unchanged
while recording recurrence metadata. It does not rebuild outputs or set
AppliedE's quantity. All four release targets register it. The null-plan reset
from [PR #419](https://github.com/cTux/ae2-crafting-time/pull/419) is separate.

The selected upstream correction prevents AELIS from entering the request-ordered
provider lifecycle early. This ownership decision is based on the field/order
observations below, not merely on absent Crafting Time frames in an exception.

## Current control evidence

Canonical files are under build/issue420-campaign-4d3 in the retained campaign.
Do not publish private paths or whole agent transcripts. Retain a redacted raw
debugger excerpt with the [archive](../ui-smoke-evidence.md) before cleanup.

| File relative to campaign | SHA-256 |
|---|---|
| enabled-evidence/stage-readback-v2.json | b562063b77135f488aa4af3e3a8137223ec258d86bc3ff0de54b060250accd5b |
| guest-stage-readback-disabled.json | eda388b95baa0e602d56a1ad2e1043f5e747983365ce5a4f45cf4c8f878d2158 |
| enabled-control/result.json | 5a461cc3768fe0a6152b6134e803d09873729805b3af5387b5d2daf25f7474d4 |
| disabled-control/result.json | 59f9461edaf14f89534a5749cda5080a40bd1d3d6ed88d97f51868394a42af78 |
| disabled-control/appliede-profiled-plan.json | 02a6f78cc1fccbd364d71e03184046841cbafc1476010b34c298d085e69610e3 |
| ap03-jdb-trace.txt (summary, not raw output) | 5fe3ec13e0798d7a0dfb731ff2379f739725438ded8f211e6064a241b35fbe97 |

The preserved enabled receipt and disabled receipt contain identical 366-entry
mod inventories: 364 non-Crafting-Time JARs and the current production/driver
pair, with no missing, extra or mismatched expected dependencies. Both record
8192 MiB, Java 17, long-range crafting and diagnostics enabled; automatic AELIS
changes from true to false. The output directory necessarily differs. The
top-level guest-stage-readback-v2.json was overwritten with the disabled receipt;
it is not the enabled provenance. Keep full configuration and pristine-reset
records separate from the verified inventory/settings comparison.

Current production SHA-256 is
f4a8b5a80b07dd0bd15364db1708136f3407fafb182268cabea9c92d72cd637a;
driver SHA-256 is
02430b5c9c9fb9fa0bf5df37fcb5be81cca8f693060c8c3a24931b6c2a5eed37.

The instrumented enabled run failed PLAN_STABLE with empty CraftConfirmScreen
rows, zero counters and all checks false. Debugger suspension can consume the
scenario's wall-clock deadline: this result is diagnostic, not an independent
clean proof of timeout causation. The original uninstrumented exception above
remains the independent player-failure evidence.

The disabled run passed every existing check with expected/dispatched/returned/
starts/finishes all one and 57 ticks. Its retained plan explicitly shows one
furnace crafted from eight stored cobblestone. It proves native crafting with
AELIS disabled succeeds in this mixed-provider fixture. It does not prove
AppliedE performed the transmutation or charged EMC. AP-06 remains open.

## Primary debugger observations

Successful raw JDB output is retained in Codex session
01a0a90f-d305-7c92-b95f-716b81d20272, PTY 30569. The table uses one-based
JSONL line numbers in that session's rollout; rows reference tool outputs,
not agent reasoning or the written trace summary. The campaign's
jdb-enabled.stdout.log and jdb-interactive.log belong to earlier unsuccessful
attachment attempts and cannot replace these outputs.

| Raw output lines | Observation |
|---|---|
| 5519, 5525 | First child-build breakpoint; stack enters from AelisPlanner.Compiler.inspect. |
| 5531-5549 | Furnace key, node amount one, calculation JDI 139258, nodes null, AppliedE requested field zero. |
| 5657-5669 | Advertised AppliedE TransmutationPattern has output key JDI 139257 and amount one. |
| 5711-5741 | Compiler occurrence/bridge JDI 139256; calculation 139258; child-list JDI 139302; AppliedE field zero; rebuilt pattern JDI 139385 has the same output key and amount zero. |
| 5627, 5729 | Process identity-hash values 1064056970 and 1090790940, corresponding to rendered process identifiers 3f6c388a and 4104261c. |
| 5753-5771 | Native request breakpoint; requestedAmount one, AppliedE field one, calculation 139258 and child-list 139302 retained. |
| 5789-5819 | Both process strings and zero AppliedE output persist; next buildChildPatterns entry still sees child-list 139302 and captured quantity one. |

The request dump does not separately print its this JDI ID as 139256. Continuity
is supported by the shared calculation, exact child-list identity and retained
processes; do not claim that missing direct node-ID observation. Early concurrent
expression errors are excluded from the evidence.

These values prove an advertised quantity-one AppliedE pattern is rebuilt with
the uncaptured zero quantity during AELIS inspection, and native request later
reuses that cached zero-output process after capturing one. The disabled control
supports the AELIS boundary, but its conventional recipe cannot establish
EMC correctness. The future correction still needs a clean enabled functional run.

## Startup prerequisite already delivered

The initial overlay-only prerequisite reached the title screen but still timed
out during world entry. The merged usable-world prerequisite at the current base
now defers the initial STARTING clock until the readiness predicate shared with
start() succeeds. It preserves the existing ten-minute observed deadline and
later-language-reload behavior. Shared Forge/Fabric 1.20.1 and NeoForge 1.21.1
consumers use it; the separate 26.1.2 implementation is unchanged. Both current
controls reached usable worlds, so do not reimplement these prerequisites.

## Selected upstream correction and regression boundary

Query raw candidate patterns through the existing crafting service before every
AELIS child-build bridge call. An exact class-name match for
gripe._90.appliede.me.misc.TransmutationPattern yields the stable
request_ordered_pattern fallback before that occurrence's child list is
initialized. The existing session/native fallback then lets AppliedE capture
request quantity before constructing that child's outputs.

Guard inspect, terminal-quantity-feedback inspection and contextual validation,
including nested nodes. Propagate the existing typed fallback to the session;
the terminal helper's RuntimeException catch must not consume it. Keep emitters,
ordinary candidates, absent AppliedE, arithmetic safety and optional dependency
loading unchanged. No required AppliedE class reference or dependency cap is
needed. A generic extension contract is outside this minimal correction.

Upstream tests must prove all three pre-build paths, nested and mixed candidates,
untouched requested child caches, stable fallback propagation and unaffected
ordinary compilation. A class-name predicate test alone is insufficient.

The current AppliedEFixture leaves the ordinary furnace recipe available and
does not measure the authoritative EMC balance. Preserve the mixed-provider
planning regression, then isolate an EMC-only request by removing competing
recipes/supply through existing disposable-fixture APIs. Read the real provider
accounting path, sample after fixture funding and before submission, compute a
positive one-furnace charge from the installed ProjectE value, and verify that
exact net debit plus one returned furnace. Keep profile/TTC checks. This is
development-driver work with Forge/NeoForge fixture consumers; it does not justify
a Crafting Time production workaround or claim NeoForge has the same planner bug.
