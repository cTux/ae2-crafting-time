# In-game Configuration Screen Specification

Issue: [#117](https://github.com/cTux/ae2-crafting-time/issues/117)

## Goal

Let players and server owners configure every useful AE2 Crafting Time option in
game. The screen must make ownership clear: local display choices belong to the
client, while profiling and shared behavior remain server-authoritative.

## Entry points and layout

- Forge and NeoForge expose **Configure** from this mod's loader details page.
- Fabric exposes the same screen through optional Mod Menu integration. The mod
  still loads and its config files still work when Mod Menu is absent.
- The screen has General, Displays, Appearance, Diagnostics, and Advanced
  sections. Every row shows its current value, default, valid range or choices,
  and a short English or Ukrainian explanation.
- **Done** saves valid changes. **Cancel** discards unsaved changes. Each section
  has **Reset section**, and the root screen has **Reset all**; reset changes are
  reviewable before **Done** writes them.
- Client-only changes apply as soon as **Done** is pressed. Server-owned changes
  made by an authorized local/server operator apply on the next supported config
  reload; otherwise the screen explains that a reconnect, world reload, or
  server restart is required.

## Settings

### Server-owned

| Setting | Default | Valid values | Behavior |
| --- | --- | --- | --- |
| Profiling and TTC (`enabled`) | on | on/off | Enables server profiling and the data used by TTC surfaces. |
| Retained samples (`maxSamples`) | `10` | `1`-`100` | Limits recent throughput and accuracy samples per output. |
| Outlier multiplier (`outlierMultiplier`) | `4.0` | `1.0`-`1000.0` | Sets the median-relative throughput filter. |
| Delayed notification (`notifyOnDelayed`) | on | on/off | Controls private delayed/blocking notifications. |
| Chat details (`showChatMessages`) | on | on/off | Controls server-sent TTC details and reset notices. |
| Minimum no-progress time | `10 s` | `1`-`3600 s` | Earliest time at which an output can become delayed. |
| Typical-duration multiplier | `2.0` | `1.0`-`1000.0` | Required learned-duration multiple before an output becomes delayed. |

The server sends the effective values needed for display and explanations after
login and when they change. Only the integrated-server owner or a player with
server operator permission level 4 can submit changed server values. The server
revalidates and saves them; dedicated-server files and permissions remain
authoritative.

### Client-owned

| Group | Settings and defaults |
| --- | --- |
| Displays | Independent on/off values for Crafting Plan rows, Crafting Plan total, Crafting Status rows, Crafting Status total, AE2: Crafting Tree, ME Requester, detailed tooltips, control hints, status indicators, and accuracy details. All default on where supported. |
| Sorting | Separate Crafting Plan and Crafting Status defaults. Both start at longest first, matching the current `2` mode. Choices are AE2 order, shortest first, and longest first. |
| TTC scale | Fast `#55FF55`, middle `#FFFF55`, slow `#FF5555`. |
| Status text | Waiting `#E0E0E0`, delayed/blocked `#FF5555`, collecting-data `#E0E0E0`, and total TTC `#E0E0E0`. |
| Badges | Background `#000000` with `176/255` opacity, matching `0xB0000000`. |

`showInTree` migrates to the AE2: Crafting Tree display toggle. Options for a
surface that is unavailable on the current target stay visible but disabled and
explain the missing mod or unsupported target.

## Validation and recovery

- Text fields reject non-numeric, non-finite, and out-of-range values before
  saving. Color values use six-digit RGB; opacity uses `0`-`255`.
- A missing key uses its default. Existing valid values survive the upgrade.
  Existing `enabled`, `showInTree`, `showChatMessages`, `notifyOnDelayed`,
  `maxSamples`, and `outlierMultiplier` values migrate without changing meaning.
- A malformed client value falls back only that value. A malformed server value
  follows the loader's config correction path and is logged without crashing a
  client or dedicated server.
- Unknown keys are preserved when the backing loader supports preservation and
  otherwise ignored; they never appear as invented UI options.

## Compatibility

The screen and matching behavior cover all four release-matrix targets:
Minecraft 1.20.1 Forge, Minecraft 1.20.1 Fabric, Minecraft 1.21.1 NeoForge, and
Minecraft 26.1.2 NeoForge. English and Ukrainian keys, defaults, validation, and
ownership remain aligned.

## Non-goals

- A new configuration library, web UI, command-only editor, or custom widget
  framework.
- Client authority over server profiling, persistence, chat, or diagnostics.
- Exposing packet limits, protocol constants, cache sizes, or other
  implementation-only values.
- Duplicating the TTC-details mouse binding from Minecraft's Controls screen.
- Changing crafting behavior, learned samples, or existing defaults merely by
  opening the screen.

## Acceptance criteria

- Every current config key and every setting above can be viewed and changed at
  its correct authority boundary on all supported targets.
- Loader and Mod Menu entry points open the same categorized screen, and Fabric
  works normally without Mod Menu.
- Save, cancel, section reset, full reset, invalid input, migration, reconnect,
  and restart-required states behave as specified.
- Display toggles, colors, opacity, default sorting, status visibility, and
  accuracy/detail visibility affect every listed UI surface without changing TTC
  calculations.
- A remote unprivileged client cannot change server-owned values; effective
  server values remain consistent between clients.
- English and Ukrainian labels have matching keys/placeholders, dedicated-server
  startup stays client-class-free, and all four release-matrix builds pass.
- Prepared-client smoke covers the loader entry point, editing, reset/cancel,
  immediate client updates, server-authority messaging, and persistence on every
  target.
