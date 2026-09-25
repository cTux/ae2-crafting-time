# Compact Crafting Plan amounts

Status: merged in [issue #525](https://github.com/cTux/ae2-crafting-time/issues/525). Default-off follow-up: [issue #529](https://github.com/cTux/ae2-crafting-time/issues/529).

The Crafting Plan grid still uses separate AE2 `Available` and `To Craft` lines
after the crafting-status change in #438. Show these two amounts on one line
above TTC: `4/10` when both exist, `A4` for available only, `C10` for craft
only, and no quantity line when neither exists. Keep AE2's SLOT formatting,
including units, and its original full labeled tooltip lines. Keep missing and
recurrent warnings separate.

The existing default-off `compactStatusAmounts` client choice controls both
Crafting Plan and Crafting Status. Its visible label becomes **Compact crafting
amounts**. Turning it off restores the native lines in both windows without
changing TTC. The stored config key stays the same so existing choices survive.

Only replace native lines when their keys and formatted values match the
expected AE2 output. Preserve unfamiliar lines and use the native display if
the match is uncertain. Use the existing quantity badge style, width limit,
and TTC color with the normal TTC color as fallback. Add a plan-specific
tooltip legend, including when detailed profiling tooltips are off.

Verify available-only, craft-only, both, empty, large, fluid, missing, and
recurrent rows; option on/off and relaunch; tooltip values; the four supported
targets; and readable screenshots at the existing cell width.
