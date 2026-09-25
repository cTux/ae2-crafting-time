# Compact amounts technical design

Lifecycle and behavior: [specification](spec.md).

## Evidence and ownership

The [research](research.md) records inspected upstream versions and limits.
`CraftingStatusTableRenderer#getEntryDescription` returns an `ArrayList` of up
to three translated components. Its separate `getEntryTooltip` uses full amounts.

Keep composition in
`shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingStatusTableRendererMixin.java`.
Both `mc1201` and `mc2612` copies of `AbstractTableRendererMixin` already draw
badges through their version's `TtcBadge`; do not replace the table renderer.
`CraftingRowState.isBadge` is the shared badge-key registry.

## Composition

1. Gate condensing and its legend on the new client-owned
   `OptionFeature.COMPACT_STATUS_AMOUNTS`, independently of `STATUS_ROWS`.
   Read stored, active and pending directly from the menu entry. Keep the
   existing `appendTtc` gate unchanged, including its server disable behavior.
2. Add the pure string formatter to the existing core `CraftingRowState` class
   in `shared/src/main/java`. Pass the three
   raw amounts and their key-formatted `AmountFormat.SLOT` strings; inspect each
   amount independently with `> 0`. Emit the specification's cases without a
   sum, parsing localized text, unit conversion, or a new formatter framework.
   Keep `TtcText` as the Minecraft component wrapper. Cover every formatter
   branch in `CraftingRowStateTest`; component recognition and styling use the
   existing Minecraft boundary tests. Put other Minecraft-free decisions in
   the same covered core source set rather than the excluded adapter package.
3. Use one translatable wrapper key, `text.ae2craftingtime.status.amounts`, with
   value `%s` in both locale files and the composed string as its argument. This
   identifies the badge without adding a custom component type. Prefixes and
   separators are fixed symbols. Add that exact key to `CraftingRowState.isBadge`.
4. Identify native quantity components by the translatable keys produced by
   `GuiText.FromStorage`, `GuiText.Crafting`, and `GuiText.Scheduled`, not by their
   rendered language or list position. Require exactly one matching component
   per positive amount and none for absent amounts. Reject modified components
   with extra siblings or arguments that differ from the expected native
   component. If this contract fails, keep the original list and append TTC as
   before. Do not clear the list.
5. On success, remove only those native components and insert the summary at the
   first removed position. Preserve every foreign component and its relative
   order. All-zero entries get no summary. The supported upstream lists are
   mutable; do not add catch-all exception recovery for unsupported renderers.
6. Run the existing `appendTtc` path once. Capture the component it appends and
   copy its resolved text RGB to the summary, without bold or other warning
   styles. If that component inherits its color, preserve the same inherited
   color. If no component is appended, use `ClientConfig.Color.TOTAL` explicitly.
   This avoids separately reproducing warning precedence or issuing extra stats
   requests. Never identify the TTC component by assuming the last foreign line
   belongs to this mod.
7. Update the `status-row` integration observation at this call site: net list
   growth is no longer evidence of successful rendering because three native
   lines become one. Observe successful summary insertion or TTC append, rather
   than calling `IntegrationLog.growth` with the original native list size.

The return injection stays shared and retains the normal Mixin priority. A mod
running afterward can still replace the result; priority escalation cannot prove
compatibility. Add before/after foreign-line regression fixtures and inspect the
actual prepared-client mod graph during smoke testing.

## Client option

Register `COMPACT_STATUS_AMOUNTS(Owner.CLIENT, Group.DISPLAYS,
"compactStatusAmounts")` in `OptionFeature`. The existing `OptionsScreen`
enumeration renders the toggle; `FeatureOptions` supplies the off default and
reset behavior. `ClientConfigFile` already reads/writes client-owned features
to `ae2craftingtime-client.toml`; a missing key defaults off. Reuse the existing
draft/Done/Cancel flow, with no custom control, config format or server packet.

Read `ClientOptionsRuntime.current().features().enabled(COMPACT_STATUS_AMOUNTS)`
for the quantity gate. Do not use the generic `ClientOptionsRuntime.enabled`
method here: it suppresses features when server profiling is disabled, whereas
these native menu quantities require no profiler data. When row TTC is hidden,
the summary uses the already-defined `TOTAL` color fallback.

Add `config.ae2craftingtime.compactStatusAmounts` in both locale files:

- English: `Compact crafting amounts`
- Ukrainian: `Стислі кількості виготовлення`

Use the existing client-option help tooltip. Keep the toggle available without
operator permission and do not change any sibling setting when it is switched.

## Drawing and width

Reuse the existing rounded badge drawing and shadow in both renderer adapters.
Add the new summary key to the same width-limited branch as recurrent labels,
reusing the 90-font-pixel limit and scale calculation in `CraftingRowState`.
Rename recurrent-specific helper names only where necessary to describe both
callers; retain recurrent label behavior and its tests.

The inspected AE2 table is 67 logical pixels wide. Its icon starts at pixel 48,
and right-aligned half-scale text ends at pixel 46. A 90-font-pixel line occupies
45 logical pixels; badge padding extends one logical pixel on either side and
stays within the cell. Measure `Font.width` after formatting with the active
font. For width `w > 90`, use horizontal scale `90 / w`, preserve vertical scale,
and anchor the right edge at the original `x + w`. Draw the background around
the final width, restore the pose, and preserve the legacy draw return value.
The newer adapter uses `GuiGraphicsExtractor`/matrix operations; the older one
uses `GuiGraphics`/pose operations. No new version-specific quantity hook is
needed by the inspected source contracts.

This is a fit guarantee, not a claim that every arbitrary font is legible at any
width. If the required runtime cases become illegible, the implementation gate
fails and needs a reviewed design revision rather than silently hiding values.

## Tooltips and localization

Leave AE2's full-amount tooltip components untouched. Add the quantity legend
before the existing TTC tooltip append logic, so its early return for no pending
work cannot hide the legend. Gate the legend by the compact-amount option and
at least one positive amount, without a cross-frame cache. Do not call
`getEntryDescription` again or trigger extra TTC requests. The same legend can
explain native quantities when the compact description takes its fallback.

Use `text.ae2craftingtime.status.amounts_legend`:

- English: `A: Available / C: Crafting / S: Scheduled; -: none`
- Ukrainian: `A: Доступно / C: Виготовляється / S: Заплановано; -: немає`

If a foreign mod alters the description contract only, showing the legend for
the native amounts remains harmless; tooltip formatting must never depend on
mutable render-session state. Do not gate the legend on `DETAILED_TOOLTIPS`.

## Data and failure boundaries

The server and native menu continue to own quantities. All new work is client
presentation over existing longs and `AEKey.formatAmount`. Only the new local
boolean is persisted through the existing client config. No packets, retained
profiling state, saved-data version, dependency, or history-reset changes are
added. Existing config files need no migration: the missing key defaults off;
saved `true` and `false` choices keep their values.
Unknown translation/component contracts keep native quantities. Empty entries
avoid formatting and summary creation. Custom key formatters remain responsible
for unit display; do not apply `AeKeyAmounts.normalize`, which serves estimates.

## Verification fixtures

Extend `StandardAe2Scenario`'s `standard-status-controls` leaf and
`StandardCraftFixture`; keep shared transitions in `testDriver1201` and use
the existing 26.1.2 fixture/platform adapters for changed APIs. The
[implementation plan](implementation-plan.md#verification-prerequisites) defines
the quantity, option, font and relaunch checkpoints that must be added before
the campaign. Reuse native menu rendering and final-frame observations; no
second driver or production test switch is needed.

The [implementation plan](implementation-plan.md) covers tests and runtime gates.
