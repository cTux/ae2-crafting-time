# Exact ingredient mismatch diagnostics

Status: planned; this documentation does not implement the feature.

Tracking issue: [#327](https://github.com/cTux/ae2-crafting-time/issues/327).

Source report: [Problem with autocrafting](https://www.reddit.com/r/allthemods/comments/1w5bqj5/problem_with_autocrafting/).

## Goal

When AE2 reports an item as missing while the ME network stores the same base
item with different saved data, make that near-match visible in the crafting
plan and explain the safe next step. This helps players find patterns encoded
with a different NBT tag or data-component patch without changing AE2's plan.

## Player behavior

- Keep AE2's existing `Missing` amount and add a gold `Stored variant` line to
  that row when the current ME network contains a positive amount of the same
  item under a different exact AE2 item key.
- Add these tooltip lines:
  - `The ME network stores this item with different saved data.`
  - `Re-encode the pattern using the item the network actually produces or stores.`
- Diagnose only positive missing amounts for item keys. The stored near-match
  must have the same registered item and a different exact key. NBT on 1.20.1
  and data components on newer targets remain part of exact identity.
- Treat the line as evidence of a stored near-match, not proof of the particular
  field or mod behavior that created it. Do not name ownership, energy, security,
  damage, or another component unless AE2 exposes that fact directly.
- Remove the line when the exact item becomes available, all near-matches leave
  the network, the plan is replaced, the menu closes, the player reconnects, or
  another network/menu becomes active.
- Preserve stored/craft/missing quantities, Start-button behavior, TTC text,
  TTC colors, and every sort order. The warning follows its row after sorting.
- If the recurrent-ingredient feature from #320 also diagnoses the row, keep
  both independent facts: its `Recurrent` replacement and this additional
  `Stored variant` line.

## Compatibility and text

Support Forge and Fabric 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2. Use the
logical server's current storage snapshot in singleplayer and multiplayer. The
feature works without learned timing samples and with substitutions enabled or
disabled; it reports AE2's resulting exact missing key rather than re-evaluating
substitution rules.

English uses `Stored variant`. Ukrainian uses `Інший варіант у сховищі`.
Ukrainian tooltip text is:

- `У ME-мережі є цей предмет з іншими збереженими даними.`
- `Перекодуйте шаблон, використавши предмет, який мережа справді виробляє або зберігає.`

The wording explains the condition without relying on color. Native AE2 craft
confirmation tables opened from supported terminals inherit the feature.
Crafting Tree's separate tree, ME Requester, running crafting-status rows, and
unsupported custom planners are outside this change.

## Non-goals

Do not repair or rewrite patterns, toggle substitutions, normalize or ignore
item data, change AE2 extraction/crafting, identify the differing field, list
every stored variant, add a comparison screen, diagnose fluids or chemicals,
persist diagnoses, add a setting, or release mod JARs as part of this planning
PR. A near-match outside the active ME network is not reported.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| V1 | A positive missing item row plus a stored same-item/different-key stack shows `Stored variant` and both tooltip instructions; the native missing quantity is unchanged. |
| V2 | An exact stored key, an equal display name with a different registered item, or a same-item near-match with zero available amount does not produce a false warning. |
| V3 | Ordinary shortages, fluids, chemicals, successful plans, and plans without a positive missing amount retain their native presentation. |
| V4 | Multiple stored variants produce one line on the matching row; unrelated missing rows remain unchanged and exact item variants stay distinct. |
| V5 | Replan, menu replacement, sorting, cancellation, disconnect, another player, and another network cannot carry the warning to a stale or different row. |
| V6 | Both locales, all four targets, all TTC sort modes, and no-sample plans render correctly; #320 recurrence can coexist without either diagnosis erasing the other. |
| V7 | Planning, submission, quantities, pattern data, storage contents, timing samples, saved data, and optional separate screens are unchanged. Missing or rejected diagnostic data leaves the native plan usable. |

See the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).
