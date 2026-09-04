# Provider Locate Spec

## Goal

Extend the private delayed-output warning (issue #118) so the warned player
can jump straight to the cause: the output name in the warning is a clickable
link that highlights the provider block that ran the craft, in the world, for
15 seconds. The word "delayed" renders in red. Provider-to-craft links and
pending delayed warnings survive leaving and re-entering the world.

This covers [issue #231](https://github.com/cTux/ae2-crafting-time/issues/231).

## Player behavior

- The delayed warning keeps its #118 wording, timing context, owner-only
  delivery, and once-per-stall rules.
- The output name renders underlined with a hover hint that invites the click.
- Clicking the name highlights each resolved provider block with a blinking
  red edge stroke for 15 seconds.
  Only the warned player ever receives highlights.
- The word "delayed" renders in red in English and Ukrainian.
- Clicking a link that belongs to another player, or whose record expired or
  was lost across a reload, highlights nothing and answers with a short
  expiry notice visible only to the clicker.
- When no provider position resolves for a delayed output, the name renders
  as plain text with no click action.
- Leaving and re-entering the world keeps working links for active crafts. A
  craft that is still delayed after re-entering warns again with a working
  link.

## State rules

- `Provider link` means all of these are true:
  1. A pattern producing the output was dispatched by a crafting CPU during
     its current job.
  2. AE2's crafting service still offers that pattern through at least one
     provider at notify time.
  3. At least one offering provider resolves to a world position through its
     grid node location.
- Positions resolve at notify time, not at dispatch time, so a provider that
  moved or unloaded between dispatch and stall does not produce a stale box.
- Links are per crafting CPU and output. A new job replaces the previous
  job's links for that CPU. Finishing, cancelling, disabling the profiler,
  or clearing the scope drops its links.
- The persisted copy (output, owner, dimension, provider positions, display
  name) is a fallback, not the authority: live dispatch data wins whenever
  the job ran during this session.
- Persisted links never grant visibility: a locate click only serves records
  owned by the clicking player.

## Compatibility

- Support 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge, and 26.1.2 NeoForge.
- Use the same server-owned path in singleplayer and on a dedicated server.
- No new config option; locating follows the existing `notifyOnDelayed`
  server setting.
- Update English and Ukrainian text together; the split status word keeps
  matching placeholders.
- Keep command arguments, packet fields, collection sizes, and decoded
  positions bounded and validated.
- Bump every affected loader compatibility boundary in the same change.
- Old world saves without the new section load normally with empty provider
  state.

## Not included

- Locating the external processing machine behind a provider; the provider
  block itself is the highlight target.
- Choosing one provider when several ran the craft's patterns; all resolved
  positions highlight together (bounded).
- Clickable links for NeoEco and AE2 Lightning Tech CPU jobs: their dispatch
  paths do not expose patterns yet, so those warnings render plain names
  until a later change records them.
- A locate action outside the delayed warning (no hotkey, no screen button).
- Changing the 15-second highlight duration in-game.
- Teleporting the player to the provider.

## Acceptance criteria

- The warned player sees an underlined output name with a hover hint and a
  red "delayed" word.
- Clicking the name draws boxes on the correct provider block(s) for
  15 seconds; other players see nothing.
- Clicking another player's or an expired link shows the expiry notice and
  draws nothing.
- Leaving and re-entering the world keeps the link working for an active
  craft; a still-delayed craft warns again.
- A craft with no resolvable provider position renders a plain name.
- World saves from before the feature load with empty provider state and no
  errors.
- English and Ukrainian messages keep matching placeholders.
- Packet, NBT, and message-component round trips have full test coverage.
