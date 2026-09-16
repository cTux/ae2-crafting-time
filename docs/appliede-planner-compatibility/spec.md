# AppliedE planner compatibility

[Issue #420](https://github.com/cTux/ae2-crafting-time/issues/420) records a valid
one-furnace EMC request rejected before CPU submission. Restore that request, or
explain a proven unsupported recipe accurately. The current evidence proves the
rejection but does not yet identify a safe production correction.

This first slice establishes the exact reproduction and resolves its startup
prerequisite. Read the [design](technical-design.md) and
[plan](implementation-plan.md) together. Merge these documents before executable
changes. No production fix is selected until controlled evidence proves causation;
then amend and merge all three documents before implementing that correction.

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

AP-01 through AP-05 establish readiness for a documented correction. Issue closure
also requires AP-06 and AP-07; none currently has a new passing runtime result.

## Current investigation gate

The 2026-09-16 preflight reproduced the startup prerequisite without selecting a
production correction. The managed Codex-group source contains 363 mods. Compared
with the retained exact non-Crafting-Time graph, it differs only by adding Applied
Enhancements `1.0.7-forge`, replacing OmniSequence `1.3.9` with `2.0.3-fix`, and
leaving every other mod name and hash unchanged. Hash-matching retained copies of
both missing artifacts are available. AppliedE already matches the design hash.

The disabled-AELIS attempt is still a prerequisite failure. Its driver reached
`STARTING` during the initial loading overlay, exhausted the ten-minute scenario
deadline on `ReceivingLevelScreen`, and never entered the request. A same-graph
diagnostic retry reported game startup taking 781.308 seconds, with initial
resource loading still active after ten minutes. The shared driver currently starts the deadline before it
skips loading-overlay frames.

The prerequisite correction is therefore limited to the development driver:
while `STARTING` has an uninitialized clock and the initial loading overlay,
do not initialize its deadline. Start the unchanged ten-minute deadline on the first later observable
frame. Keep every post-start deadline, production class and the independent
26.1.2 driver unchanged. AP-02 through AP-05 remain open until that prerequisite
is reviewed, merged and used for a clean exact-pack enabled/disabled campaign.
