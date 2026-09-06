# In-game Configuration Screen Technical Design

This design implements the [specification](spec.md) for
[#117](https://github.com/cTux/ae2-crafting-time/issues/117).

## Decisions

Use Minecraft's existing `Screen`, list, button, slider, and text-field widgets.
Do not add Cloth Config or another runtime dependency. Keep two explicit config
models:

- `ClientConfig`: local display, appearance, and default-sort values;
- `ServerConfig`: profiling, retention, filtering, notification/chat, and delay
  thresholds.

The models expose typed values, defaults, validation, reset, load, and save.
Shared UI code reads those models rather than loader config objects. The current
static `Ae2CraftingTimeConfig` accessors become the server-model facade so
profiling call sites do not gain loader branches.

## Storage and migration

| Owner | Forge/NeoForge | Fabric |
| --- | --- | --- |
| Client | loader client config, `ae2craftingtime-client.toml` | `config/ae2craftingtime-client.toml` through the existing bounded parser/writer |
| Server | loader server config, `ae2craftingtime-server.toml` in world server config | world-scoped `ae2craftingtime-server.toml` through the same typed parser/writer |

On first load after upgrade, read the existing
`ae2craftingtime-common.toml`. Copy only known keys that are absent from their new
owner file, validate them, then mark migration complete by the presence of the
new files. Do not delete or rewrite the legacy file. `showInTree` maps to the
client tree toggle; the other current keys map to `ServerConfig`.

Client writes use a temporary sibling file followed by replace so an interrupted
save cannot truncate the last valid config. Server writes remain on the logical
server and use loader save/config events where available. Fabric uses the same
replace rule and keeps parsing/writing code in its version module.

## Screen structure

`ConfigScreen` owns a working copy of both models. Section descriptors are fixed
data: translation key, control kind, default, range/choices, owner, apply timing,
and availability predicate. The screen creates native widgets directly from
those descriptors. There is no plugin/factory layer.

- Boolean rows use cycle buttons.
- Bounded integral values and opacity use sliders plus their exact numeric value.
- Doubles use text fields so values such as `4.0` are not rounded by a slider.
- RGB values use a `#RRGGBB` text field and a live color swatch.
- Sort modes use the existing numeric meanings: `0` AE2 order, `1` shortest,
  `2` longest.

Reset changes the working copy only. Done validates all visible fields, saves
the values the current player may own, and returns to the parent screen. Cancel
returns without saving. Disabled rows retain their stored values.

Minecraft 1.20.1 and 1.21.1 share the existing `mc1201` client source seam;
26.1.2 uses `mc2612` only where screen/widget signatures differ. The screen's
state and setting descriptors stay in Minecraft-free shared code.

## Loader entry points

- Forge 1.20.1 registers the loader's config-screen extension from client setup.
- Both NeoForge targets register the matching `ConfigScreenFactory` extension on
  the client distribution.
- Fabric adds a `modmenu` entrypoint class that implements Mod Menu's config
  screen factory. Metadata uses `suggests`, and compile-time use is isolated so
  the class is never loaded without Mod Menu.

Every entry point passes the loader's parent screen to `ConfigScreen`. No client
class is referenced from the common mod initializer or dedicated-server path.

## Applying settings

UI mixins replace hard-coded visibility, sort defaults, `TtcColor` constants,
status/total colors, and `TtcBadge.BACKGROUND` reads with `ClientConfig` getters.
TTC calculation and server snapshots remain unchanged.

Server-owned settings are read only on the logical server. Delay thresholds
replace the current 10-second and 2x constants at the single classification
seam. Retention/filter changes apply to new calculations without discarding
valid retained samples; shrinking `maxSamples` trims each queue once.

Add a bounded server-to-client config snapshot to the existing stats protocol.
It contains the effective values and an `editable` flag. Refresh it on login and
after a server config reload. A missing or invalid snapshot leaves the last valid
values/defaults and logs the rejection.

Add one matching client-to-server update packet containing the complete typed
server model plus the snapshot revision it edits. Accept it only from the
integrated-server owner or a player with permission level 4, on the server game
thread, with the expected protocol/context and current revision. Revalidate every
field, write through the server config backend, apply once, increment the
revision, and broadcast the new snapshot. Reject unauthorized, stale, malformed,
or oversized updates without partial application. This packet edits only the
fixed server-setting set; it is not a general remote file/config API.

## Validation and failure behavior

- Validate at parse and before save. Reject NaN/infinity, invalid RGB, and ranges
  outside the spec.
- If a save fails, keep the working copy on screen, show one actionable error,
  and leave the last file intact.
- If Mod Menu or an optional UI integration is absent, disable only its row or
  entry point. Startup remains clean.
- Unknown sort values fall back to longest first. Invalid colors fall back per
  field, not for the whole appearance section.
- Translation checks require identical English/Ukrainian key and placeholder
  sets.

## Verification map

| Acceptance area | Proof |
| --- | --- |
| Ownership and packet boundary | unit tests for models/codecs plus authorized integrated/operator edits and forged, oversized, stale-revision/context, and unprivileged-write checks |
| Migration and persistence | parser tests for old/new/malformed files and reload tests on each loader |
| UI behavior | prepared-client scenarios for entry, save/cancel/reset, validation, colors, toggles, sorting, and restart messaging |
| Optional integrations | Fabric with/without Mod Menu and each optional display mod present/absent |
| Server safety | dedicated startup on all targets and two-client authority check |
| Packaging | release-matrix builds and JAR audit for client-only classes/dependencies |
