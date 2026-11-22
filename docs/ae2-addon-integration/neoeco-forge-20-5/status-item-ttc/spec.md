# Per-item TTC in NeoEco Status

Status: finished

Scope: The native Status item grid on NeoEco 20.5.9 and 20.5.10 with Forge
1.20.1. The [selected CPU header ETA](../spec.md) already shipped separately.

Issue: [#766](https://github.com/cTux/ae2-crafting-time/issues/766)

Implementation: [#768](https://github.com/cTux/ae2-crafting-time/pull/768)

Verification: [Gradle tests](https://github.com/cTux/ae2-crafting-time/actions/runs/45014029719)
and [production JAR builds](https://github.com/cTux/ae2-crafting-time/actions/runs/45014029843)
passed on the implementation head `f7430ca0`. The [PR verification record](https://github.com/cTux/ae2-crafting-time/pull/768)
includes native Forge 47.4.26 and AE2 15.4.10 runs with NeoEco 20.5.9 and
20.5.10. They show separate timings, the native hover card, a wide estimate
that fits only in the card, the display option and the fallback after pause
and restart or when the server omits the row data. NeoEco 20.3.0, NeoEco
20.4.2 and Forge without NeoEco passed focused crafting cases. NeoForge
1.21.1 retained its existing adapter in the production JAR.

Planning: [Technical design](technical-design.md) and
[implementation plan](implementation-plan.md).

When an ECO CPU has work left on an item, show its learned time to craft
alongside NeoEco's quantity labels. Use the active and scheduled amounts for
that item on the selected CPU. The header continues to show the whole job's
ETA. Stored items remain visible and contribute no work to the estimate.

| ID | Acceptance |
| --- | --- |
| N766-1 | Active and scheduled items with learned timing on the selected network show their own TTC. Different items can show different times. Rows with only stored items or unknown timing keep their normal labels. |
| N766-2 | The existing Crafting Time Status rows option hides or shows the added TTC. Keep the native quantities, icons and processing display. Use the native hover card for the full estimate, including when the inline estimate would overlap another line or the icon. |
| N766-3 | Keep the estimate with the correct row when scrolling, when changing CPUs, and when a new job starts. A paused, cancelled, restored or exact order shows the native row presentation while Crafting Time has no eligible live estimate. |
| N766-4 | A missing compatible server hook, an absent add-on, and unsupported add-on versions preserve normal startup and native presentations. The older Forge adapters, AE2 Status, and NeoForge selection remain compatible. |

The item grid is the only new surface. NeoEco's task list, planning UI, quantity
format and processing warnings retain their current behavior. The separate
[NeoForge migration](https://github.com/cTux/ae2-crafting-time/issues/457)
owns that loader's NeoEco support.
