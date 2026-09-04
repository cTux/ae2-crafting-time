# Provider Locate Spec

## Goal

Extend the private delayed-output warning (issue #118) so the warned player
can jump straight to the cause: the output name in the warning is a clickable
link that highlights the provider block that ran the craft, in the world, for
15 seconds. The word "delayed" renders in red. Provider-to-craft links and
pending delayed warnings survive leaving and re-entering the world.

The same private warning, with the same clickable provider link, also fires
for `NO SPACE` and `NO POWER` rows: the craft cannot proceed and the owner
should learn about it without watching the status screen.

This covers [issue #231](https://github.com/cTux/ae2-crafting-time/issues/231).

Highlight style follows
[issue #237](https://github.com/cTux/ae2-crafting-time/issues/237)
as refined by
[issue #239](https://github.com/cTux/ae2-crafting-time/issues/239):
clicks highlight with thick (2-3x) rainbow-cycling edges only, while the
stuck output's item icon stays centered on each camera-facing block face on
a red background for as long as the output is delayed.
(Supersedes the diagonal-rod style from
[issue #234](https://github.com/cTux/ae2-crafting-time/issues/234), which
read as messy rectangles in-game.)

## Player behavior

- The delayed warning keeps its #118 wording, timing context, owner-only
  delivery, and once-per-stall rules.
- The output name renders underlined with a hover hint that invites the click.
- Clicking the name highlights each resolved provider block for 15 seconds
  with a thick (2-3x) rainbow-cycling edge outline, and pins the stuck
  output's item icon on a red plate centered on each camera-facing face for
  as long as the output stays delayed. Non-item outputs show the red plate
  with no icon. Only the warned player ever receives highlights.
- Double-clicking a delayed row in the crafting CPU screen locates the same
  way as clicking the chat link, without needing the chat message.
- The word "delayed" renders in red in English and Ukrainian.
- Clicking a link that belongs to another player, or whose record expired or
  was lost across a reload, highlights nothing and answers with a short
  expiry notice visible only to the clicker.
- When no provider position resolves for a delayed output, the name renders
  as plain text with no click action.
- Leaving and re-entering the world keeps working links for active crafts. A
  craft that is still delayed after re-entering warns again with a working
  link.
- A `NO POWER` output warns its owner once while dispatch keeps failing for
  lack of energy, with a red status word and a clickable provider name when
  the provider resolves.
- A `NO SPACE` output warns its owner once while its crafting CPU cannot
  store finished items, with a red status word and a clickable provider name
  when the provider resolves.
- A reason that clears (power restored, storage freed, output unblocked)
  re-arms that reason, so a later genuine stall warns again.
- A row that is both delayed and blocked warns once per reason, since each
  warning names a different problem.

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
- `NO SPACE` mirrors the client row predicate on the server: the CPU reports
  it cannot store items, and the output has stored items with nothing still
  outstanding.
- `NO POWER` reuses the existing per-output dispatch-power diagnostics with
  their 20-tick freshness window.
- The persisted copy (output, owner, dimension, provider positions, display
  name) is a fallback, not the authority: live dispatch data wins whenever
  the job ran during this session.
- Persisted links never grant visibility: a locate click only serves records
  owned by the clicking player.

## Compatibility

- Support 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge, and 26.1.2 NeoForge.
- Use the same server-owned path in singleplayer and on a dedicated server.
- No new config option; locating and blocked warnings follow the existing
  `notifyOnDelayed` server setting, which covers stuck-craft warnings.
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
  paths do not expose patterns yet, so provider positions never resolve
  there and names render plain. `NO SPACE` detection still works on every
  CPU type; `NO POWER` warnings cover the standard and AdvancedAE dispatch
  paths that observe power.
- `NO PROVIDER` warnings: missing providers flap during normal play, so only
  `NO SPACE` and `NO POWER` warn.
- A locate action outside the delayed warning (no hotkey, no screen button).
- Changing the 15-second highlight duration in-game.
- Teleporting the player to the provider.

## Acceptance criteria

- The warned player sees an underlined output name with a hover hint and a
  red "delayed" word.
- Clicking the name draws thick (2-3x) rainbow-cycling edge boxes on the
  correct provider block(s) for 15 seconds and pins item-on-red plates
  there while the output stays delayed; other players see nothing.
- Clicking another player's or an expired link shows the expiry notice and
  draws nothing.
- Leaving and re-entering the world keeps the link working for an active
  craft; a still-delayed craft warns again.
- A craft with no resolvable provider position renders a plain name.
- A `NO POWER` warning arrives once per power-failure episode and returns
  after power is restored and fails again.
- A `NO SPACE` warning arrives once per storage-blocked episode and returns
  after space is freed and blocks again.
- World saves from before the feature load with empty provider state and no
  errors.
- English and Ukrainian messages keep matching placeholders.
- Packet, NBT, and message-component round trips have full test coverage.
