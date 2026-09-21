# AppliedE planner compatibility

[Issue #420](https://github.com/cTux/ae2-crafting-time/issues/420) records a valid
one-furnace EMC request rejected before CPU submission. Restore that request, or
explain a proven unsupported recipe accurately. The controlled exact-pack
campaign and reviewed raw debugger observations establish an upstream ordering
defect: AELIS initializes an AppliedE child before its requested amount is captured.
No Crafting Time production correction is selected. Corrected EMC behavior still
requires verification.

The reproduction and startup prerequisites are complete. The causal documentation
amendment was merged in [PR #436](https://github.com/cTux/ae2-crafting-time/pull/436).
Read the [design](technical-design.md) and [plan](implementation-plan.md) together.
No upstream correction or final EMC verification has been completed. A selected
correction design is not a verified fix.

## Reported environment

| Component | Version |
|---|---|
| Project Infinity | 0.0.52.0 |
| Minecraft / Forge / Java | 1.20.1 / 47.4.20 / 17 |
| AE2 | 15.4.10 |
| AppliedE TPS Fix | 0.14.7-fix2 |
| Applied Enhancements | 1.0.7-forge |
| OmniSequence | 2.0.3-fix, CurseForge file 8878052 |
| Original Crafting Time test build | 1.2.6, commit `0426f9469d3e9a810e6dd1037242855b9a311e70` |

Automatic AELIS and long-range crafting were enabled. A powered, player-owned
Transmutation Module advertised the furnace as craftable, with furnace knowledge
and sufficient EMC. AELIS's native boundary and subsequent native fallback both
rejected a matching output total of zero. The later `PLAN_STABLE` timeout is a
consequence. A separate disabled-AELIS attempt timed out in `STARTING` before a
usable world; it provides no comparison result.

## Boundaries

- Keep all native arithmetic checks, including positive output counts. Disabling
  the guard would leave invalid input for AE2's division by the output count.
- Keep ordinary prepared profiles and the managed Prism source unchanged. Use a
  newly marked disposable copy of the exact pack in CodexVM, with Java 17 and
  the existing host-build, Prism and fixture workflows.
- Keep production packets, saved data, dependency ranges and optional-mod loading
  unchanged during investigation. Do not patch a third-party JAR under this plan.
- The reproduced compatibility scope is Forge 1.20.1 only. AppliedE's NeoForge
  1.21.1 integration is not proven affected; Fabric and NeoForge 26.1.2 are not
  additional pack targets. A shared driver change must still consider its consumers.
- An investigation result or startup-only fix does not resolve the player bug.
  An upstream correction or workaround requires its own evidence and scope decision.

## Acceptance criteria

| ID | Required evidence |
|---|---|
| AP-01 | A disposable reproduction matches the reported Minecraft/loader, non-Crafting-Time mod hashes and gameplay configuration. Record current production/driver SHAs and test-harness settings separately; verify the three critical dependency hashes in the design. Only the automatic-AELIS setting differs between controls. |
| AP-02 | The client reaches a usable marked world under bounded, observable startup. Record loading, world-entry and scenario timing separately. A failed startup remains a prerequisite failure, never an AppliedE verdict. |
| AP-03 | Capture advertised and rebuilt pattern outputs, requested quantity, first build/request order, node/process identity and cached-child reuse for the furnace. Distinguish observed values from inferred causation. |
| AP-04 | Compare fresh calculations with automatic AELIS enabled and disabled, changing only that setting. Keep native safety and long-range crafting enabled. Report each result independently, including failure before request entry. |
| AP-05 | Establish a causal control and correction ownership before selecting a production fix. Update and merge this spec, design and plan with the proven mechanism, exact changes and regression checks. |
| AP-06 | For the final correction, a valid one-furnace request plans, is accepted, returns exactly one furnace, charges the expected EMC and finishes. Existing `appliede-cpu` checks also prove a profile sample and TTC afterward. If the documented correction instead establishes an unsupported recipe boundary, verify its accurate rejection without starting a craft and preserve supported AppliedE behavior. |
| AP-07 | Retain immutable logs, configuration, inventories, hashes, scenario results, reviewed screenshots and exact-client exit evidence. Bind checks to the tested head; preserve failed and unrun cases. |

AP-01 through AP-05 establish readiness for a documented correction. The
campaign below supplies graph, control and independently reviewed AP-03 debugger
observations; the AP-05 documentation amendment is merged. Issue closure still
requires the corrected enabled run in AP-06 and its AP-07 evidence.

## Earlier startup prerequisites

This section records superseded prerequisites. The usable-world correction is
present at `4d3d818602a98922c00af7d75fc9490133e8e6a3`; do not implement it again.

The 2026-09-16 preflight reproduced the startup prerequisite without selecting a
production correction. The managed Codex-group source contains 363 mods. Compared
with the retained exact non-Crafting-Time graph, it differs only by adding Applied
Enhancements `1.0.7-forge`, replacing OmniSequence `1.3.9` with `2.0.3-fix`, and
leaving every other mod name and hash unchanged. Hash-matching retained copies of
both missing artifacts are available. AppliedE already matches the design hash.

The merged initial-overlay prerequisite was exercised at its exact merged head
`aa50d4328c005cbf6856bba44e91c5a71b88e265`. The current driver survived the
initial overlay and reached the title screen after ModernFix reported
`958.782` seconds of process startup. The marked world then spent another
`5.835` minutes starting JEI. The unchanged ten-minute `STARTING` deadline
expired on `ReceivingLevelScreen` before the first crafting request; all seven
scenario checks and every dispatch counter remained zero. The disabled control
was therefore not run, and this result is not an AppliedE verdict.

The second prerequisite was limited to the development driver. While
`STARTING` has an uninitialized clock, defer that clock until the first frame
whose overlay and screen are clear and whose client/player/game mode and correct
local or dedicated server connection are all usable by `start()`. Then preserve
the unchanged ten-minute deadline, including during any later language reload.
Every post-start deadline, production class and the independent 26.1.2
driver stayed unchanged. The subsequent campaign reached usable worlds.

## Current campaign and remaining gates

The fresh campaign ran from merged prerequisite head
`4d3d818602a98922c00af7d75fc9490133e8e6a3`. It staged the managed 363-JAR
source plus the two hash-pinned graph changes above, and verified 364
non-Crafting-Time JARs with no missing, extra or mismatched entries. The current
Crafting Time production and driver SHA-256 values were respectively
`f4a8b5a80b07dd0bd15364db1708136f3407fafb182268cabea9c92d72cd637a` and
`02430b5c9c9fb9fa0bf5df37fcb5be81cca8f693060c8c3a24931b6c2a5eed37`.
Both controls used Forge `47.4.20`, Java `17.0.20.101`, an 8192 MiB heap,
long-range crafting and unchanged native arithmetic safety. The retained receipts
have identical mod hashes and critical harness settings; automatic AELIS is the
changed gameplay setting. Keep the separate world-reset and full configuration
provenance with the control evidence rather than inferring it from matching JARs.

With automatic AELIS enabled, the scenario failed before submission because the
confirm plan had no rows. The reviewed raw debugger output shows the AELIS compiler
calling `CraftingTreeNode.buildChildPatterns` before native `request`: the
furnace request was one, the advertised AppliedE `TransmutationPattern` output
was one, but AELIS triggered AppliedE's rebuild and cached output zero while
AppliedE's requested-amount field was still zero. Native `request` then captured
one while retaining the same calculation, child list and cached processes, too
late to repair that child. The design qualifies the node-identity evidence. With
automatic AELIS disabled, the native path passed: the plan contained furnace and
cobblestone rows, and dispatched, returned, started and finished exactly one
furnace with every existing scenario check true. However, its plan explicitly
uses eight stored cobblestone. This proves generic native crafting succeeds;
it does not prove EMC transmutation or the correct EMC charge. The raw evidence
proves the ordering and cache mechanism. Debugger suspension can consume the
enabled scenario deadline, so that timeout is diagnostic, not a clean functional
control; the earlier uninstrumented safety exception remains separate evidence.

The selected correction belongs in Applied Enhancements. Before AELIS calls its
child-build bridge for each inspected occurrence, it must query the existing
crafting service for candidate patterns. If an AppliedE
`gripe._90.appliede.me.misc.TransmutationPattern` is present, compilation must
return a stable `request_ordered_pattern` fallback before initializing the
node's child list. The normal wrapper can then invoke AE2's untouched native
request path, allowing AppliedE to capture the requested amount before building
children. An optional dependency-safe class-name classifier avoids adding a
required AppliedE dependency and applies at every inspected node, including
nested occurrences.

Cover canonical inspection, terminal-quantity-feedback inspection and contextual
occurrence validation; a guard in `Compiler.inspect` alone is incomplete. The
fallback must escape local error-handling and abort compilation before that
occurrence's child cache is initialized.

The upstream regression must prove that a request-ordered candidate selects the
fallback before the build hook and leaves the child cache uninitialized, while
ordinary AELIS candidates still compile. The corrected exact-pack run must keep
AELIS enabled, record the fallback reason, requested/advertised/rebuilt amount
one, and pass AP-06. The fixture must prevent the ordinary cobblestone recipe
from satisfying the request and verify a positive expected EMC debit through
the real provider, with exactly one furnace returned. Both the original mixed
recipe conflict and the isolated EMC-only path need coverage. No Crafting Time
production change is authorized by this result, and an upstream source change
requires separately scoped authority in its owning repository.
