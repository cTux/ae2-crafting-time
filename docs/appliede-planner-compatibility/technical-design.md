# AppliedE failure and reproduction boundaries

Investigation base: `7262a78efb72ca8f7a0ec1c875ae1c2f3fdc4aa2`, inspected on
2026-09-16. See the [spec](spec.md) for scope and [plan](implementation-plan.md)
for the prerequisite gates. No Crafting Time production mutation is established.

## Verified runtime evidence

The retained archive is identified by
`20260914T101445Z-f5bb/archives/20260914T130501504Z-b8bbd4af`, under the
`project-infinity-0.0.52` modpack archive. Its `forge-exact-fixed-retry` graph is
the reported 0.0.52.0 release with the updated dependencies. Archive locations
follow [the evidence contract](../ui-smoke-evidence.md).

In `run/evidence/latest.log`, lines 4056-4063 show the AppliedE fixture reaching
`PLAN_STABLE`. At 13:03:16 UTC, AELIS reports furnace `amount=1`,
`aggregatedRequest=1` and `invalid_pattern_output`. Both its native boundary and
native fallback throw from Applied Enhancements' positive-output check with a
matching total of zero. Player chat calls this native 64-bit overflow. The driver
fails 30 seconds later; process exit zero does not make the scenario pass.

| Retained file | SHA-256 |
|---|---|
| `run/evidence/latest.log` | `2f714811c2d4988ab404a917e591ddbf23752e3e1fb83cd93fb7e8a2c7559b22` |
| `run/evidence/appliede-cpu/result.json` | `a911c1f4993d15df0693943d0bef9266ac47322e0f37ed2aef77d7ba4fd5f980` |

The archived `mod-hashes-exact-fixed-retry.json` identifies:

| Dependency | SHA-256 |
|---|---|
| AppliedE 0.14.7-fix2 | `832e8b3872ca90c3d1a917bdcbb3c09d7c809b2edeeec6295227f4f287f5d29e` |
| Applied Enhancements 1.0.7-forge | `37b52c6938b9ebd8d4b8c5780c0c89d485ab58abb58beae8a5e7c96f36fbf5a9` |
| OmniSequence 2.0.3-fix | `7c0e12290ed104401719efc174be2867006fb557104978fed577a5b87007a875` |

The later retained `aelis-disabled/result.json` has SHA-256
`e7f39eec82ec518a55079042d8f80f3bc51d766c8825536963237e4dd83336ea`.
It records `STARTING` timeout on `ReceivingLevelScreen`, all craft checks false;
the failure capture is dated 2026-09-15 19:36:23 UTC. Its staging script referenced
a locally built Applied Enhancements JAR, so its bytes must not be assumed to
match the original release. The [issue discussion](https://github.com/cTux/ae2-crafting-time/issues/420#issuecomment-5684243048)
keeps the missing causal evidence explicit.

## Source flow and hypothesis

1. AppliedE's `EMCModulePart.getAvailablePatterns` delegates to
   [KnowledgeService](https://github.com/62832/AppliedE/blob/b57292fdd2478d5115a74158d5c319456b91a6b1/src/main/java/gripe/_90/appliede/me/service/KnowledgeService.java),
   which advertises each known item as `TransmutationPattern(item, 1)`.
2. Its [node mixin](https://github.com/62832/AppliedE/blob/b57292fdd2478d5115a74158d5c319456b91a6b1/src/main/java/gripe/_90/appliede/mixin/crafting/CraftingTreeNodeMixin.java)
   captures quantity at `request` HEAD, then rebuilds transmutation patterns in
   `buildChildPatterns` using that field, whose default is zero.
3. [AELIS's calculation wrapper](https://github.com/AyaYumi/AppliedEnhancements/blob/b2c4dbd9d29bcafb527e85a08c0e07879d7ce2d8/src/main/java/com/appliedenhancements/mixin/AelisCraftingCalculationMixin.java)
   tries AELIS before the original tree request. The
   [compiler](https://github.com/AyaYumi/AppliedEnhancements/blob/b2c4dbd9d29bcafb527e85a08c0e07879d7ce2d8/src/main/java/com/github/appliedenhancements/crafting/aelis/AelisPlanner.java)
   calls `buildChildPatterns` during canonical compilation, terminal-quantity
   inspection and contextual-occurrence validation.
4. [AE2 15.4.10](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/forge/v15.4.10/src/main/java/appeng/crafting/CraftingTreeNode.java)
   initializes children only when `nodes == null`. Its request divides demand
   by `CraftingTreeProcess.getOutputCount`.
5. [Native safety](https://github.com/AyaYumi/AppliedEnhancements/blob/b2c4dbd9d29bcafb527e85a08c0e07879d7ce2d8/src/main/java/com/appliedenhancements/mixin/CraftingTreeProcessLongSafetyMixin.java)
   unconditionally requires a positive matching total. AELIS fallback reuses the
   same tree. This does not prove a fresh AELIS-disabled calculation fails.

The leading hypothesis is early child construction before AppliedE's quantity
capture, followed by cached zero-output children. The observed run did not dump
those fields or that ordering. Another wrapper or different matching provider
state remains possible. OmniSequence and Thunderbolt appear in the stack; their
presence alone is not causation. Match inspected source against runtime artifacts
and transformed methods before relying on it for a correction.

Crafting Time's shared
[`CraftingTreeNodeMixin`](../../shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingTreeNodeMixin.java)
preserves `notRecursive`'s return value and delegates `addMissing` unchanged while
recording recurrence metadata. It does not rebuild outputs or set AppliedE's
quantity. All four release targets register this hook. The null-plan reset from
[merged PR #419](https://github.com/cTux/ae2-crafting-time/pull/419) is separate;
[dependency documentation](../dependencies.md) already records it.

## Startup seam and available infrastructure

Shared [`CraftPlanScenario`](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/CraftPlanScenario.java)
has a 10-minute startup deadline, checked before loading-overlay handling.
`start()` waits for a usable world and validates its disposable marker.
[`TestDriverRuntime`](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/TestDriverRuntime.java)
constructs scenarios initially and through both suite-transition paths. The clock
starts on first elapsed-time evaluation and resets on state transition. Several
standard/status scenarios also use the startup constant outside `STARTING`;
raising it globally would change those deadlines too. The separate 26.1.2 driver
uses two minutes. Existing `TestDriverCoreTest` covers `startTime` initialization,
not a complete slow-start policy.

Read-only preflight found CodexVM and key-based SSH available, guest Java 17
installed, Prism running, and no Minecraft process. Prepared Forge uses 47.4.10;
another manifest uses 47.4.23. Neither is the exact pack target. The Codex-group
managed pack has Forge 47.4.20, AE2 and AppliedE at the reported versions, but
ships OmniSequence 1.3.9 without Applied Enhancements. The previous disposable
instance and world are gone. Existing host-build, Prism staging,
`prepare-ui-smoke-suite.ps1`, `AppliedEFixture`, `SuitePlan` and `FixtureMarker`
provide the provisioning path. Recheck this inventory before execution.

No existing runtime trace captures AP-03. Use bounded read-only debugger
inspection if available. New diagnostic instrumentation or a startup-policy
change needs a concrete design amendment first; do not silently introduce a
runner, profiler, third-party patch or broad timeout increase.
