# Colored TTC Research

Date: 2026-06-21

## Request

Color the `TTC` text in the AE2 crafting plan from bright green to bright red:

- fastest craft in the visible crafting plan list: bright green
- middle of the range: bright yellow
- slowest craft in the visible crafting plan list: bright red
- entries between them: interpolated color

## Feasibility

This is possible.

AE2 `CraftConfirmTableRenderer` returns visible cell lines as `List<Component>` from `getEntryDescription(...)`. `AbstractTableRenderer.render(...)` draws those lines with:

```text
GuiGraphics.drawString(Font, Component, x, y, defaultColor, false)
```

Because the renderer passes a Minecraft `Component`, not a plain `String`, a `Component.literal("TTC: ...").withStyle(...)` can carry text color. The current mod already appends `TTC` by adding a `Component` in `CraftConfirmTableRendererMixin`, so the feature can stay in the same UI path.

## Design Constraint

The existing `getEntryDescription(...)` mixin sees only one `CraftingPlanSummaryEntry` at a time.

To color by "fastest and slowest in the crafting plan list", the code must know the full list being rendered. That list is available in AE2 `AbstractTableRenderer.render(..., List<T> entries, int scrollOffset)`, not in the per-entry description method.

## Proposed Design

Use a render-scope color context.

1. Inject into `AbstractTableRenderer.render(...)` at method head.
2. If `this` is a `CraftConfirmTableRenderer`, scan the passed `entries`.
3. For each `CraftingPlanSummaryEntry` with `craftAmount > 0`, look up server-synced stats in `ClientStats.CACHE`.
4. Convert stats and craft amount into estimated seconds.
5. Compute `minSeconds` and `maxSeconds` for the current render list.
6. Store `ProfileKey -> color` in a short-lived client-side context.
7. Existing `CraftConfirmTableRendererMixin` reads the color for the entry and appends:

```java
Component.literal("TTC: " + eta).withStyle(style -> style.withColor(color))
```

8. Clear the context at render return.

No server changes are needed. The server already sends the aggregate stats required to calculate the estimate. The color is purely client display logic.

## Color Scale

Use three fixed RGB stops:

```text
fastest: #55FF55 green
middle: #FFFF55 yellow
slowest: #FF5555 red
```

For an entry:

```text
ratio = (seconds - minSeconds) / (maxSeconds - minSeconds)
```

Then linearly interpolate RGB from green to yellow for the first half and from
yellow to red for the second half.

```text
channel = from.channel + ratio * (to.channel - from.channel)
```

If `minSeconds == maxSeconds`, use green for every `TTC` line. There is no useful relative spread when every known entry has the same estimate.

## Missing Stats

If an entry has no cached server stats yet:

- keep requesting stats as today
- render no `TTC` line until stats exist
- exclude it from min/max color calculation

This avoids lying with a default color.

## Helper Change

`TimeEstimate` currently returns only formatted text. Coloring needs the raw estimated seconds too.

Smallest change:

```text
TimeEstimate.seconds(amount, stats) -> OptionalLong
TimeEstimate.format(amount, stats) -> uses seconds(...)
```

This avoids parsing strings like `~1:07` back into seconds.

## Risks

- AE2 `AbstractTableRenderer` is shared by multiple craft-plan tables. The render-scope mixin must guard on `this instanceof CraftConfirmTableRenderer`.
- The color range is based on the currently rendered list passed by AE2. If the list includes off-screen rows, colors represent the whole current plan list. If AE2 passes only visible rows after scrolling, colors represent visible rows. Local bytecode shows `render(..., List<T>, scrollOffset)` receives the full list and applies `scrollOffset` internally, so the expected behavior is whole current plan list.
- Bright TTC colors use Minecraft's native dark text shadow for contrast without a heavy outline.

## Tests

Add small shared tests for:

- `TimeEstimate.seconds(...)` returns one second for nonzero sub-second work.
- color interpolation returns dark green at min, dark red at max, and a middle color between them.
- equal min/max returns dark green.

Version-side tests can stay cheap:

- assert the craft plan mixin still hooks `getEntryDescription`
- assert the render-scope mixin is client-only in `ae2craftingtime.mixins.json`

## Recommendation

Implement it with a tiny shared color helper and one client-only render-scope mixin. Do not add server packets, config, or a new UI setting for the first pass.
