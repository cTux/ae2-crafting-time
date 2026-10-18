# Badge Background Switch

Status: in-progress

Scope: An independent client switch for backgrounds drawn by AE2 Crafting Time.

Issue: [#532](https://github.com/cTux/ae2-crafting-time/issues/532)

Planning: [Technical design](technical-design.md) and [reviewed plan](implementation-plan.md).

The original investigation lacked guest-tool access. Later isolated CPU-list
Off/On evidence is recorded in the [#696 fixture scope](../../test-driver/spec.md#optional-badge-backgrounds-696);
it does not establish the original all-target visual matrix.

Implementation: [PR #543](https://github.com/cTux/ae2-crafting-time/pull/543) is merged.
The implementation is present; all-target visual acceptance remains an explicit
qualification boundary unless supported by separate retained evidence.

## Current default

The original proposal used On. The current [configuration contract](../spec.md#feature-switches)
and merged [reset correction #620](https://github.com/cTux/ae2-crafting-time/pull/620)
use Off for fresh models, missing/invalid values, Reset All and Reset section.
This does not finish the original visual verification scope. Companion design
and plan On-default references are historical and superseded by this contract.

## Behavior

Add **Badge background** to Client / Appearance with key `badgeBackground` and
default **Off**. Off skips the mod's rounded background rectangles behind Crafting
Plan and Crafting Status row badges, totals, CPU-card totals, and supported
Crafting Tree and ME Requester labels. Text, its shadow, position, scaling,
foreground color, and interactions retain their existing behavior.

Keep the saved Badge opacity and Badge background color unchanged when switching
Off and back On. On draws with those saved values, including opacity zero. The
switch is independent of Text shadow, fast-to-slow TTC colors, compact amounts,
and server profiling. Native AE2 window, row, and tooltip backgrounds are outside
its scope.

Use the existing options session: Done saves and applies, Cancel discards draft
changes, and Reset section or Reset all restores Off in the draft. Resets retain
their existing behavior of also restoring the relevant color and opacity defaults;
only toggling must preserve custom values. Save the switch in
`config/ae2craftingtime-client.toml`. A missing or malformed value defaults Off.

## Compatibility

Cover all four release targets: 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge,
and 26.1.2 NeoForge. Optional surfaces retain their existing availability. Add
matching English and Ukrainian labels and explain the option in both GuideME
locales and the GitHub wiki. No server setting, packet change, or world-save
migration is needed.

## Acceptance criteria

| ID | Required result |
| --- | --- |
| A1 | Client / Appearance shows Badge background, defaults Off, and keeps pagination and color/opacity input validation working. |
| A2 | Off skips every mod badge background, including normal and scaled Plan/Status rows; text and native AE2 backgrounds remain visible and unchanged. |
| A3 | Off/On preserves custom background color and opacity; shadow, TTC colors, compact amounts, and profiling remain independent. |
| A4 | Save/load and relaunch retain Off; missing/malformed values use Off; copy, Cancel, section reset, and full reset follow the existing session behavior. |
| A5 | Both renderer families build across all four targets; shared changed behavior has 100% line/branch coverage, and English/Ukrainian keys and player documentation agree. |
| A6 | Both states are visually verified on every supported target, including readability on tinted Plan/Status rows and restoration of the selected background appearance. |

Completion requires actual automated and visual evidence for these criteria.
An unavailable prepared client is a verification blocker, not a visual pass.
