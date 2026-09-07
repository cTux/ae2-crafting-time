# Recurrent ingredients in the crafting plan

Status: planned; this documentation does not implement the feature.

Tracking issue: [#320](https://github.com/cTux/ae2-crafting-time/issues/320).

## Goal

When crafting A needs B and crafting B needs A, the crafting plan should explain
the loop instead of presenting the affected ingredient as an ordinary shortage.
Give that missing row the status `Recurrent`, in red text.

## Player behavior

- Replace the affected row's `Missing` label with `Recurrent`. Keep its existing
  missing quantity and unit, for example `Recurrent: 1`.
- Apply the same label to the missing-amount line in the row tooltip. Add the
  explanation: `This ingredient is missing because its crafting recipe depends on itself, directly or through other recipes.`
- Mark only missing rows for which the current calculation proves that recursion
  prevented a recipe from being used. Do not mark every item in a circular
  recipe catalogue, or every ancestor of a blocked ingredient.
- Cover direct self-dependencies, A -> B -> A, and longer loops. A row represents
  the actual missing ingredient reported by AE2; a two-recipe loop need not
  produce two missing rows.
- Keep ordinary shortages as `Missing`. A rejected circular alternative alone
  is not enough when an eligible alternative remains: show the shortage that
  AE2 actually reaches along that alternative.
- A successful plan, including one made possible by seed ingredients or a valid
  alternative, has no Recurrent rows. Preserve AE2's own treatment of requested
  output already in storage; this feature does not change what counts as a seed.
- If one row combines ordinary and recurrent shortages, use Recurrent when any
  positive part has proven recurrence. Keep AE2's total missing amount; do not
  claim it is an exact recurrent-only quantity or invent split quantities.
- Replace the status when the plan changes. Closing the menu, reconnecting, or
  opening another network must not carry the previous plan's status forward.
- Keep stored/craft quantities, TTC lines, TTC colors, row order, and the Start
  button's existing behavior. A red label must remain red in every TTC sort mode.

## Compatibility and text

Support Forge and Fabric 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2. Use the
same logical-server result in singleplayer and multiplayer. The feature must
work without learned timing samples.

English uses `Recurrent`, not `Recursive`, in normal weight and Minecraft red.
Ukrainian uses `Циклічне`; its tooltip is
`Цього інгредієнта бракує, бо рецепт його виготовлення залежить від нього самого — безпосередньо або через інші рецепти.`
Text and the tooltip explain the status without relying on color alone.

Native AE2 plan tables opened from supported terminals inherit the feature.
Crafting Tree's separate tree, ME Requester, and running crafting-status rows
are outside this change. An unsupported custom planner or a peer without the
diagnostic capability keeps ordinary Missing text; absence of evidence never
becomes a positive diagnosis.

## Non-goals

Do not repair recipes, resolve loops, modify AE2's calculation or submission,
add a graph viewer, change TTC estimation, reorder missing rows, add a setting,
persist diagnoses, or release mod JARs as part of this planning PR.
Missing-first sorting is tracked separately in
[#318](https://github.com/cTux/ae2-crafting-time/issues/318).

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| R1 | A -> B -> A without a usable seed shows a red Recurrent label and unchanged quantity on the actual missing row, with the explanation on hover. |
| R2 | Direct self-dependency and a three-recipe loop receive the same diagnosis. |
| R3 | Ordinary shortages and failures on an eligible nonrecursive alternative keep Missing; a successful seeded or alternative plan has no Recurrent label. |
| R4 | Mixed rows follow the aggregation rule; unrelated missing rows stay Missing. Resource variants remain distinct. |
| R5 | Replan, menu replacement, another network, cancellation, disconnect, and late packets cannot show an old diagnosis. |
| R6 | Both locales, all four targets, all TTC sort modes, no-sample plans, and quantities/units remain correct. Long text stays within the existing table layout. |
| R7 | Craftability, calculation results, timing data, saved data, and optional separate screens are unchanged. Missing or rejected diagnostic data leaves the native plan usable. |

See the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).
