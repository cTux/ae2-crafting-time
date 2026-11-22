# NeoEco Status item TTC technical design

[Scope and acceptance](spec.md) · [Implementation plan](implementation-plan.md)

## Source contract

The tagged 20.5.9 and 20.5.10 sources have byte-identical `ComputationCpuStatusPanel`,
`ComputationCpuItemEntry` and `ComputationCards`. NeoEco's server snapshots
select one CPU, add its header ETA, and serialize bounded item and task pages.
Each item record carries its full AE key and exact quantities. The client accepts
the page with its selection and scroll state and may copy a row to add processing
detail before rendering it. The item grid and task list use different renderers.
[Status snapshot and pages][snapshot] · [Item record][item] · [Item grid][render]

AE2's existing Status rows estimate active plus scheduled output with
`AeKeyAmounts`, `TimeEstimate` and the network's learned stats. Reuse that amount
conversion and the common client TTC formatter. Only numeric learned TTC and its
existing display preferences are in scope. NeoEco owns its waiting and processing
labels. [AE2 Status renderer](../../../../shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingStatusTableRendererMixin.java)

## Server and wire

Extend only the item-page serialization inside the native snapshot. Intercept
its first `page` call, call NeoEco's original page builder, and enrich those
already bounded row compounds in order. Use the source record's AE key so
NeoEco-specific key decoding and normalization remain intact. Match the same
clamped source range used by the native page and skip enrichment if the lengths
differ. Do not serialize estimates for the parallel task page.

Require an enabled, tracked and unpaused external job on the actual selected
CPU. Use its grid and Crafting Time's existing key-type gate. Bound the sum of
active plus scheduled before converting NeoEco's BigInteger quantities to a
positive long. Look up network-scoped stats and return a finite duration plus
its reliability bit. The server omits the optional field when any gate fails.
The existing CPU ETA and six-method public API stay independent.

Use a namespaced compound inside the native item row. The client accepts only
the expected typed fields and a finite, nonnegative duration. A missing or
invalid field means no TTC. Native clients can ignore the extra field and
older servers send the unmodified row. The estimate lives on the decoded row
and follows NeoEco's native processing copy. A new snapshot or page replaces
the row and its estimate together. There is no separate result cache, request,
persisted value, or client-supplied network identity.

## Client and layout

Use the existing `STATUS_ROWS` switch and the shared TTC formatter, including
its local display preferences. Append the component to `itemDescription` only
when it fits the native 67-by-22 cell's text area, before the icon, using the
actual font width and line height at NeoEco's half-scale. Add the full
component to the native hover card through the existing footer hints. Preserve
the item name, mod label and all native tooltip entries. The hover card is also
the fallback for a crowded or unusually wide cell.
[Native cell text placement][render] · [Item card construction][card]

Keep the optional mixins on Forge. The existing external CPU selector limits
the feature to its supported execution path. Check the native presentation
descriptors independently so an absent or changed UI can fall back without
disabling the header API. Presentation injections use `require = 0` and are
tested against the remapped add-on artifacts. The dedicated server hooks avoid
client classes.

[snapshot]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/gui/computation/ComputationCpuStatusPanel.java#L1396-L1477
[item]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/gui/computation/ComputationCpuItemEntry.java
[render]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/gui/computation/ComputationCpuStatusPanel.java#L903-L937
[card]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/gui/computation/ComputationCards.java#L67-L121
