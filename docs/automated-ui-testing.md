# Automated UI Testing

The UI smoke suite launches each development client, exercises the real AE2
screens, records what was rendered, saves screenshots, and closes that exact
client before moving to the next one. A client passing startup alone is not a UI
pass.

This document defines the intended suite. The runner and test driver do not
exist yet.

See [AE2 Crafting Time Test Driver](test-driver.md) for the companion mod and
its first Forge 1.20.1 artifact contract.

## What owns the matrix

Use these files instead of maintaining another dependency list in test code:

- `scripts/release-matrix.json` owns the four published targets.
- `scripts/run-client-versions.json` owns the compatible and latest development
  dependency graphs.
- `DEPENDENCIES.md` owns integrations the project currently claims to support.

The suite must reject a target missing from either matrix. Every top-level
project in the selected run-client profile must be reported as direct coverage,
coexistence coverage, test tooling, excluded with a recorded reason, or not
applicable. Installing an addon is not evidence that AE2 Crafting Time supports
it.

## Clients to run

Run clients sequentially. Parallel Minecraft clients waste memory and make
logs, screenshots, ports, and world state harder to attribute.

| Target | Required compatible client | Diagnostic latest client |
| --- | --- | --- |
| 1.20.1 Forge | `run-1.20.1-forge.bat` | `run-1.20.1-forge-latest.bat` |
| 1.20.1 Fabric | `run-1.20.1-fabric.bat` | `run-1.20.1-fabric-latest.bat` |
| 1.21.1 NeoForge | `run-1.21.1-neoforge.bat` | `run-1.21.1-neoforge-latest.bat` |
| 26.1.2 NeoForge | `run-26.1.2-neoforge.bat` | `run-26.1.2-neoforge-latest.bat` |

The four compatible clients are the release-facing smoke gate. Their complete
graphs are pinned and must pass. Latest clients deliberately resolve current
upstream files. A latest-only resolution or startup failure is a visible
diagnostic result, not proof that the pinned release is broken.

## Common checklist

Every client checks the standard AE2 path:

1. Resolve the selected dependency profile and record every installed file.
2. Copy the target's tracked `ae2-crafting-time` world to a disposable test
   world. Never mutate the checked-in fixture.
3. Launch the client and fail on loader, mixin, resource, or startup errors.
4. Enter the disposable world and open its known AE2 terminal.
5. Open Crafting Plan for a known craftable output.
6. Verify TTC or `Collecting data` on eligible rows, the total TTC, badge
   geometry, longest-first default order, all three sort modes, tooltip content,
   Ctrl-click details, and Ctrl-Alt-click reset.
7. Submit the craft and open Crafting Status.
8. Verify row TTC, header total placement, sorting, tooltip and click targeting,
   and deterministic waiting, running, delayed, and completed states.
9. Confirm the job produces its expected output.
10. Save semantic results, screenshots, and current logs, then request a clean
    client shutdown.

Each UI assertion must come from the actual screen, renderer, widget, input, or
client/server result. Translation keys existing in a language file do not prove
that a screen displayed them.

## Current optional-dependency checklist

The current run-client matrix contains more projects than the published
optional-dependency list. Use these coverage meanings:

- **Direct UI:** check the addon's real screen, TTC content, layout, tooltip,
  details/reset targeting, and any addon-specific total.
- **Direct behavior:** run a real addon CPU or key through the standard AE2 plan
  and status screens and verify profiling, TTC, and completion.
- **Coexistence:** verify startup, absence of mixin errors, and a standard AE2
  craft while the candidate is installed. Do not claim addon support from this.
- **Tooling:** required by the development client but not an AE2 Crafting Time
  integration.

| Target | Direct UI | Direct behavior | Coexistence candidates and dependencies | Tooling |
| --- | --- | --- | --- | --- |
| 1.20.1 Forge | AE2 Crafting Tree; ME Requester | Applied Mekanistics chemical key; NeoEco AE C-series CPU; AE2 Lightning Tech time-wheel CPU | AdvancedAE; ExtendedAE; ExtendedAE-Plus; BM Addon; Crazy AE2 Addons; AE2 WCWT; AE2 Wireless Terminals; MEGA Cells; OMNI Cells; ProjectCell; Applied Flux; Modern AE2 Additions; AE2 Import Export Card; AEInfinityBooster; Advanced Peripherals; Expanded AE; ProjectE | GuideME; JEI |
| 1.20.1 Fabric | ME Requester | None beyond standard AE2 | ExtendedAE; AE2 Wireless Terminals; MEGA Cells; AE2 Things | JEI |
| 1.21.1 NeoForge | AE2 Crafting Tree; ME Requester | Applied Mekanistics chemical key; AdvancedAE Quantum Computer | NeoEco AE; AE2 Lightning Tech; ExtendedAE; ExtendedAE-Plus (latest only; excluded from compatible because of its recorded Expanded AE conflict); BM Addon; AE2 WCWT; AE2 Wireless Terminals; MEGA Cells; OMNI Cells; ProjectCell; Applied Flux; AE2 Import Export Card; AEInfinityBooster; Advanced Peripherals; Expanded AE; ProjectE | GuideME; JEI |
| 26.1.2 NeoForge | None; pre-26 optional UI adapters must be absent | AdvancedAE Quantum Computer | AE2 Lightning Tech; ExtendedAE; BM Addon; Neo Vitae; AE2 Wireless Terminals; OMNI Cells; Applied Flux; AE2 Import Export Card; AEInfinityBooster | GuideME; JEI |

AE2 Crafting Tree is a declared 1.20.1 Fabric integration but is not currently a
top-level project in that target's run-client matrix. Report that check as
`MISSING_FIXTURE`, not passed or not applicable, until the development client
contains a compatible test installation.

Candidate key, storage, provider, terminal, and CPU addons should gain a direct
scenario only after the relevant integration is implemented or native AE2
coverage is verified. Until then, their coexistence check catches dependency and
mixin regressions without overstating behavior.

## How UI evidence works

The companion test driver observes the final AE2 screen boundary independently
from the production mod. For each rendered item it records semantic data such
as:

```json
{
  "screen": "CraftConfirmScreen",
  "output": "minecraft:iron_ingot",
  "component": "text.ae2craftingtime.ttc",
  "text": "12s",
  "bounds": [142, 81, 42, 13],
  "badge": true
}
```

The suite checks that required components were observed, remain inside the
screen, and do not overlap owned buttons or item cells. It clicks the real sort
button and rows, then verifies the visible order and resulting server response.
This catches a mixin that no longer applies, a render hook that runs on the
wrong screen, and a sorted row whose click still targets the old index.

Use a fixed resolution, GUI scale, language, fixture, and cursor position for
screenshots. Screenshots are evidence for clipping, spacing, color, and other
visual judgment. Do not make a full-frame pixel comparison the first pass;
animated items and renderer differences create noise. Add a cropped golden
comparison only for a stable region that has repeatedly regressed.

## Results and failure handling

Write one result directory per client and profile:

```text
build/ui-smoke/1.21.1-neoforge/compatible/
  result.json
  resolved-mods.json
  client.log
  craft-plan.png
  craft-plan-tooltip.png
  crafting-status.png
  failure.png
```

Every check returns one of:

- `PASS`: the required behavior was observed.
- `FAIL`: the client ran but the expected behavior was wrong or missing.
- `FAIL_SETUP`: dependencies, fixture, or startup prevented the scenario.
- `MISSING_FIXTURE`: supported behavior has no runnable fixture yet.
- `NOT_APPLICABLE`: the target intentionally does not contain that behavior.
- `DIAGNOSTIC_FAILURE`: a latest-profile upstream graph did not resolve or run.

On failure, record the current screen class, expected and observed semantic
events, screenshot, log, dependency manifest, and exact step. Ask Minecraft to
stop normally. If it times out, terminate only the process tree started for that
client; never kill Java processes broadly.

## Done means

The automated suite is ready when one command can run all four compatible
clients unattended, produce a complete per-target checklist, close each client,
and return non-zero for any required failure. Latest-profile runs may be a
separate switch, but their failures must remain visible and attributable.
