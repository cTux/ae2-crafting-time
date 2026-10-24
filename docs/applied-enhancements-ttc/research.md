# TTC in Applied Enhancements AELIS plan review

Research for [#756](https://github.com/cTux/ae2-crafting-time/issues/756), October 24, 2026.
Implementation is deferred. Native coexistence verification is tracked in
[#754](https://github.com/cTux/ae2-crafting-time/issues/754).

## Result

Yes, a TTC column is technically feasible. Applied Enhancements 1.1.4-forge
has no public material-column registration API. Its custom screen needs its own
optional presentation adapter; our native AE2 table hooks do not render this table.
This is a source-based feasibility result, not a runtime qualification of AELIS.

## Verified source

The inspected upstream Forge revision is
[`ab7fb661`](https://github.com/AyaYumi/AppliedEnhancements/commit/ab7fb661d2a5d07303e0f7bd343e61a491418281).
The official beta artifact is
[CurseForge file 9301183](https://www.curseforge.com/minecraft/mc-mods/applied-enhancements/files/9301183).
The published JAR's metadata identifies 1.1.4-forge, requiring AE2 [15.4.10,16)
and Forge 47.4.10+. Source revision and published binary are separate evidence;
no reproducible-build equivalence is claimed.

- [Screen drawing, lines 548-604](https://github.com/AyaYumi/AppliedEnhancements/blob/ab7fb661d2a5d07303e0f7bd343e61a491418281/src/main/java/com/appliedenhancements/client/plan/AelisCraftConfirmScreen.java#L548-L604):
  the screen extends CraftConfirmScreen but draws material rows directly. It does
  not call CraftConfirmTableRenderer for them. Tooltip drawing is separate too.
- [Row model](https://github.com/AyaYumi/AppliedEnhancements/blob/ab7fb661d2a5d07303e0f7bd343e61a491418281/src/main/java/com/appliedenhancements/client/plan/AelisPlanRows.java#L30-L38):
  each row has its AEKey and BigInteger stored, craft, missing and cyclic amounts.
  Demand is their stored/craft/missing sum; it is not the quantity to estimate.
- [Layout](https://github.com/AyaYumi/AppliedEnhancements/blob/ab7fb661d2a5d07303e0f7bd343e61a491418281/src/main/java/com/appliedenhancements/client/plan/AelisPlanLayout.java#L10-L52):
  the panel caps at 360 scaled pixels, requires at least 300, and uses fixed
  numeric column spacing. Cyclic plans add another column. Another column needs
  coordinated layout and hitbox changes, not an overlay on existing text.
- [Screen selection](https://github.com/AyaYumi/AppliedEnhancements/blob/ab7fb661d2a5d07303e0f7bd343e61a491418281/src/main/java/com/appliedenhancements/client/plan/AelisPlanScreenEvents.java#L26-L68):
  the custom view replaces native confirmation while calculating or on the AELIS
  planner path, when enabled and large enough. The AE2 view remains available.
- [Public API boundary](https://github.com/AyaYumi/AppliedEnhancements/blob/ab7fb661d2a5d07303e0f7bd343e61a491418281/docs/API_INTEGRATION.md#L31-L50):
  only the public api packages are supported addon entry points. The documented
  client extension APIs cover pattern terminals and item context menus, not
  AELIS material columns. Internal graph packets are not a stable graph API.

## Smallest useful design

Start with one TTC column for each row's **To craft** quantity. Put the existing
approximate plan total in the summary area, with the same uncertainty semantics
as native Crafting Time. Stored-only rows have no remaining work. Missing history
uses the existing collecting-data/unknown behavior; missing and cyclic work must
not gain a false completion promise.

Reuse ClientStats, ClientStatsRequests, ProfilerBridge.key, AeKeyAmounts,
TimeEstimate, TtcText and RowTextColor. Keep profiling and retained history on
the server, and requests bounded and scoped to the current network and screen.
Do not add planner simulation or a second timing database.

TimeEstimate.milliseconds currently accepts long; AELIS rows use BigInteger.
Never silently use longValue() or clamp an oversized amount and display the
result as its full TTC. Either support the wider quantity in shared covered
logic, or explicitly show an unavailable estimate above the supported range.
Keep fluid normalization and output identity consistent with the native path.

A public upstream column provider is the preferred stable solution. It would
need visible row keys/quantities, column measurement/render/tooltip callbacks,
layout reservation and screen/menu lifecycle context. No upstream request was
sent. If we implement against the current release, use a string-target optional
client adapter selected by an exact class/member contract through the existing
IntegrationCatalog/IntegrationSelection mechanism. Keep it absent on unsupported
versions and dedicated servers. Presentation injections use require=0; verify
both the remapped production target and a real client startup.

Treat rendering, tooltip bounds, row selection, search/filter order, scrolling,
cyclic columns, CPU controls, AE2-view switching and narrow-screen fallback as one
layout change. Keep TTC sort/details/reset controls out of the first version
unless separately requested. An extra rate column is unnecessary for the first
TTC implementation. Craft Tree support is a separate presentation decision.

## Acceptance before calling it supported

Verify the exact published beta with AELIS enabled, trained and empty histories,
ordinary item/fluid rows, stored-only/missing/cyclic rows, oversized quantities,
calculation/replan/failure transitions, search and every filter, scrolling, tree
switching, AE2 view, narrow windows and maximum GUI scale. Check reconnect and
network identity isolation, option-off behavior, an older addon API, and addon
absence. Review screenshots of column widths, badges, colors and tooltips. Cover
new shared arithmetic and layout decisions completely. Add a dedicated AELIS
fixture; native standard-plan-controls does not prove custom-screen support.
