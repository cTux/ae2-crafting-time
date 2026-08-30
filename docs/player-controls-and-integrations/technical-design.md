# Player Controls And Integrations Technical Design

## Presentation

`CraftConfirmTableRendererMixin` and `CraftingStatusTableRendererMixin` append
translatable TTC components to AE2's existing row descriptions and tooltips.
They do not replace AE2's table renderer.

`AbstractTableRendererMixin` scans the current plan or status list once, builds
a render-scoped `ProfileKey -> RGB` map with `TtcColor`, draws TTC backgrounds
through `TtcBadge`, and enables text shadow. It clears the context after the
render. The same path covers normal TTC and `DELAYED` rows without adding a
custom widget tree.

The screen mixins own totals and sort state:

- `CraftConfirmScreenMixin` sums known plan-row estimates and draws the total
  below CPU details.
- `CraftingCPUScreenMixin` calculates running total from AE2 elapsed progress
  and appends it beside the title only when the measured text fits.
- both screens start with sort mode `2` and use `TtcSort.copySorted(...)` on a
  copied list; mode `0` returns AE2 order, `1` is shortest first, and `2` is
  longest first.

Minecraft 26.1.2 uses the same behavior through `mc2612` screen copies that
adapt the upstream input and rendering API types.

## Details And Reset Flow

The registered `show_ttc_details` mouse key defaults to left-click.
`TtcDetailsKeyMapping` combines it with the current modifier state:

```text
Ctrl + bound mouse button       -> SHOW
Ctrl + Alt + bound mouse button -> RESET
```

The screen finds the entry using its table geometry, scrollbar, and displayed
sort order, with AE2's hovered stack as a fallback. `StatsChatMessages` sends a
`StatsChatC2S(outputId, amount, action)` record; the client never supplies chat
text or a trusted network id.

`StatsChatServer` resolves the player's current grid, derives its network id,
validates the output id, and applies `PlayerMessageRateLimit`. SHOW formats the
server's aggregate throughput and optional accuracy. RESET clears the
authoritative `ProfileKey`, updates persistence through `ProfilerBridge`, and
removes the local cache entry immediately.

Messages use a bound player chat type and are sent to every connected player.
`showChatMessages = false` suppresses SHOW and reset notices, but it does not
block the reset mutation.

## Configuration

Forge and NeoForge register `Ae2CraftingTimeConfig.SPEC` as a common config.
Fabric loads the same filename with a small line-based `key=value` reader,
ignores unknown or malformed lines, and clamps numeric ranges.

`ProfilerBridge` creates `CraftProfiler` and `TtcAccuracyTracker` from
`maxSamples` and `outlierMultiplier`. Runtime event paths read `enabled`, and UI
or message paths read `showInTree` and `showChatMessages` where needed.

No custom config screen, sync packet, watcher, or hot-reload layer exists.

## Optional UI Adapters

### AE2: Crafting Tree

The pre-26 mixin list contains two string-target `@Pseudo` adapters for old and
new Crafting Tree widget packages. `CraftingTreeTtc` contains the shared
reflection, recursive estimate, color, and badge helpers. Identity maps prevent
two equal-looking nodes from collapsing into one result, and the traversal
cache prevents repeated recursive calculation.

Both adapters:

- request stats for node outputs;
- add known self plus child estimates recursively;
- compute tree-relative colors;
- increase vertical spacing while `showInTree` is enabled;
- add tooltip/control hints; and
- route details/reset clicks through the common server action.

Missing reflected fields, methods, or failed reflective calls return `null` and
omit the affected optional UI result.

### ME Requester

`MERequesterScreenMixin` is a pre-26 string-target `@Pseudo` mixin. It reads the
request key and wanted amount reflectively, requests both aggregate stats and
current stored network amounts, then estimates only the positive shortfall.
It draws half-scale row badges and a header total using the shared TTC, color,
and badge helpers.

## Other Addon Boundaries

Applied Mekanistics needs no UI-specific class reference. `AeKeyAmounts` uses
the AE key's amount-per-unit contract, so chemical keys follow the same
millibucket normalization as fluids where the addon is supported.

AdvancedAE uses a NeoForge-only string-target execution mixin. It mirrors the
standard AE2 CPU dispatch, completion, job-accuracy, finish, and recent-capacity
events into `ProfilerBridge`. `StatsRequestContext` can resolve the selected
AdvancedAE CPU for status diagnostics. Missing AdvancedAE classes leave the
adapter inactive.

## Localization

`TtcText` and `StatsChatServer` build Minecraft translatable components instead
of hard-coded player text. English and Ukrainian JSON files live together under
`shared/src/main/resources/assets/ae2craftingtime/lang`, so every loader packages
the same keys. Placeholders and key sets must stay matched.

## Registration Boundaries

- Server execution mixins stay under `mixins`.
- AE2 and optional renderer/input mixins stay under `client`.
- 1.20.1 and 1.21.1 include Crafting Tree and ME Requester adapters.
- both NeoForge targets include the AdvancedAE adapter.
- 26.1.2 omits the pre-26 optional UI adapters.
- loader metadata keeps optional dependencies optional and agrees with
  `DEPENDENCIES.md`.
