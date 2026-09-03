# Provider dispatch statuses

Issue: [#216](https://github.com/cTux/ae2-crafting-time/issues/216).

Status: planning approved on 2026-09-03; implementation has not started.

## Goal

Explain why scheduled work cannot reach a machine with three crafting-status
labels: `NO TARGET`, `INPUT BLOCKED`, and `LOCKED`. Replace a vague waiting or
delayed label only when the server has direct evidence for the new status.

## Player behavior

| ID | Status | Meaning | English tooltip |
| --- | --- | --- | --- |
| PD-01 | NO TARGET | No eligible destination was found for an attempted pattern dispatch. | No usable destination was found for this pattern. / Connect a compatible machine or inventory to an enabled provider side. |
| PD-02 | INPUT BLOCKED | A destination exists, but blocking mode or an observed insertion rejection prevents dispatch. | The destination is not accepting this pattern's inputs. / Check blocking mode, input space, filters, and machine sides. |
| PD-03 | LOCKED | An actual provider crafting lock prevents dispatch. | Pattern Provider crafting locks are preventing the next batch. / Check redstone conditions or return the previous result to its provider. |

- **PD-04:** Require positive scheduled work on the row and evidence from a real
  attempted dispatch. Missing ingredients, no dispatch budget, an inactive
  provider, provider busy state, or a generic failed machine call alone prove
  none of these statuses. Configuring a lock without activating it is not LOCKED.
- **PD-05:** Evaluate alternatives for the exact pattern. Any successful
  provider or destination suppresses that attempt's warning. Any unobserved,
  unsupported, busy, or otherwise unexplained alternative also suppresses a
  specific new warning. All eligible providers must agree on the same new
  status; mixed causes keep the existing fallback display.
- **PD-06:** A row combining several patterns can show a warning for one
  blocked scheduled pattern while other batches are active. Tooltips append
  `This status applies to scheduled batches; active batches may still finish.`
  when both amounts are positive. A successful different pattern with the same
  output must not erase the blocked pattern's evidence.
- **PD-07:** Keep stored-only NO SPACE first. For pending rows use
  `NO PROVIDER > NO POWER > LOCKED > INPUT BLOCKED > NO TARGET`, then the
  existing Waiting, DELAYED, TTC, and No data yet rules. Priority combines
  independently proven patterns in one row; it must not select a cause from
  disagreeing alternatives for one pattern.
- **PD-08:** Render the three labels bold red in the existing compact badge.
  They have no TTC color and sort as unknown time. Keep existing total-TTC
  behavior; this feature does not promise an ETA for a blocked job.
- **PD-09:** Clear an observation on the next successful, unknown, or changed
  evaluation of that pattern. Otherwise expire it after 20 server ticks and
  clear it on the next existing status refresh. Recovery without a new attempt
  therefore takes at most 20 server ticks plus one refresh, not a fixed
  wall-clock promise during server lag. Job replacement, finish, cancellation,
  disable, and runtime reload clear all related state.

## Compatibility and boundaries

- **PD-10:** Cover 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge, and 26.1.2
  NeoForge from the [release matrix](../../scripts/release-matrix.json).
  Support native AE2 CPUs and the existing AdvancedAE CPU integration on its
  three applicable targets. Providers inheriting the observed AE2 method can
  participate only when execution reaches the verified checks. Overridden or
  custom provider/CPU paths without those checks remain unknown, not broken.
- **PD-11:** Keep logical-server ownership, selected-CPU/network isolation,
  bounded packets, existing request authorization/rate limits, and optional
  dependency behavior. Client/server protocol versions advance together;
  stored samples and dependency minimum versions do not change.
- **PD-12:** Add English and Ukrainian together. Ukrainian labels are
  `Немає приймача`, `Вхід заблоковано`, and `Заблоковано`. Keep translations
  semantically equivalent. Runtime smoke is English only and exercises only
  the newest implemented adapter, following the
  [shared smoke policy](../automated-ui-testing/spec.md#smoke-policy).

## Not included

No automatic fixes, extra screens, settings, saved diagnostic history, block
coordinates, per-lock subtype labels, or new statuses beyond the chosen three.
Do not infer machine power, fuel, recipe validity, chunk loading, channels, or
output capacity. NO SPACE keeps its CPU-to-ME-storage meaning. Craft Plan,
Crafting Tree, and ME Requester gain no new status display. New adapters for
NeoEco, LightningTech, or custom provider implementations are outside this
feature; their existing TTC/profiling support must remain unchanged.

## Acceptance criteria

| Check | Observable result | Requirements |
| --- | --- | --- |
| AC-01 | An attempted processing dispatch with no eligible destination shows NO TARGET; attaching a usable destination clears it. A recognized machine rejecting a craft never becomes NO TARGET. | PD-01, PD-04, PD-09 |
| AC-02 | Blocking-mode rejection and simulated zero-input acceptance each show INPUT BLOCKED; clearing the condition removes it. Partial acceptance followed by a successful dispatch is not INPUT BLOCKED. | PD-02, PD-04, PD-09 |
| AC-03 | Active high/low redstone locks, pulse locks, and result-return locks show LOCKED; inactive configured locks do not. Unlocking clears it. | PD-03, PD-09 |
| AC-04 | A healthy alternate side/provider prevents each warning. Busy, unknown, unvisited, and mixed-cause alternatives produce no new warning. Missing ingredients and generic machine rejection do not become any new status. | PD-04, PD-05 |
| AC-05 | Shared-output and mixed active/pending rows retain exact-pattern evidence, show the scheduling qualifier, and follow documented priority and sorting. Existing statuses and total TTC do not regress. | PD-06, PD-07, PD-08 |
| AC-06 | CPU/network switches, late previous-CPU replies, lifecycle cleanup, no learned samples, expiry, and backwards game ticks cannot leak or retain warnings. | PD-09, PD-11 |
| AC-07 | Every target passes changed logic/packet/contract coverage and focused live status/recovery smoke. AdvancedAE cases cover its three targets; unsupported paths remain unchanged. | PD-10, PD-12 |
| AC-08 | Both locales have matching keys/placeholders, the visible English badges/tooltips fit, malformed payloads are rejected, and player JARs exclude the driver. | PD-08, PD-11, PD-12 |

See the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).
