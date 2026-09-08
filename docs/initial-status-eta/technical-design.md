# Initial Crafting Status ETA Technical Design

## Root cause

AE2 15.4.10's `CraftingCPUScreen.updateBeforeRender()` estimates remaining time
as `elapsed / max(1, start - remaining) * remaining`. Before the first completed
unit, `start` and `remaining` are both `Integer.MAX_VALUE`, so about 0.957 seconds
of real elapsed time becomes `570992:20:20`. The issue's captured value matches
that calculation.

The fixture submits a real AE2 job and does not write these counters. The
single-world reset destroys the old grid and creates a fresh scenario. None of
the installed Project Infinity integrations targets AE2's title or elapsed-time
tracker.

## Change

Keep the decision in the Minecraft-free `TimeEstimate` helper: progress exists
only when `remainingItems < startItems`. In the existing mc1201
`CraftingCPUScreenMixin` title hook, rebuild the base Crafting Status title when
that condition is false, then preserve AE2's can't-store warning. The existing
total TTC badge is drawn separately and remains unchanged.

This uses the shared mc1201 source set because the affected AE2 title flow is
shared by 1.20.1 Forge/Fabric and 1.21.1 NeoForge. The 26.1.2 implementation is
not changed because its newer AE2 status API is a separate compatibility path.

## Failure boundaries

Null status keeps the current title. Completed progress keeps AE2's title.
Equal or reversed progress counters hide only the unsupported native ETA; they
do not suppress the separate TTC badge or status warnings.
