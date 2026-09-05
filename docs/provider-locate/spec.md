# Provider Locate Spec

## Goal

Extend the private delayed-output warning (issue #118) with two independent
world highlights that share positions but never share lifetimes.

Red plates also clear when releasing a machine's held final output finishes
the craft immediately. This does not require another progress update or an
open terminal screen.

- Red background + item icon (plate): appears automatically when a craft
  becomes delayed, blinks, and lasts while the craft stays delayed.
- Rainbow edges: appear only after a chat-link click or crafting-item
  double-click, blink for 15 seconds, then expire on their own.

The word "delayed" renders in red. Plates are server-authoritative and return
after leaving and re-entering the world while the craft is still delayed.
Rainbow timers are never saved and never restored.

The same private warning shape, with the same clickable provider link, also
fires for `NO SPACE` and `NO POWER` rows so the owner learns about a blocked
craft without watching the status screen. Blocked warnings never create or
clear red plates.

This covers [issue #231](https://github.com/cTux/ae2-crafting-time/issues/231).

Highlight style follows
[issue #237](https://github.com/cTux/ae2-crafting-time/issues/237)
as refined by
[issue #239](https://github.com/cTux/ae2-crafting-time/issues/239):
manual locates draw thick (2-3x) rainbow-cycling edges only, while the
stuck output's item icon stays centered on each camera-facing block face on
a red background for as long as the output is delayed.
(Supersedes the diagonal-rod style from
[issue #234](https://github.com/cTux/ae2-crafting-time/issues/234), which
read as messy rectangles in-game.)

## Player behavior

- Red plates appear and blink automatically when a craft becomes delayed. No
  click is needed and no window needs to be open. The server sends them
  separately from chat.
- `notifyOnDelayed` controls chat only. Turning it off stops the private
  delayed message but never stops red plates, plate clears, or login resync.
- Double-click applies to any resolvable active crafting item, including
  normal TTC rows. The server checks the player's open CPU scope, job
  ownership, and live provider positions. Unresolvable rows answer with the
  same expiry notice as a stale chat link and draw nothing.
- Chat links and double-clicks draw rainbow edges only. They blink for 15
  seconds and never create, extend, or clear red plates.
- Lifetimes are independent:

  | Event | Red background + item icon | Rainbow edges |
  | Craft becomes delayed | Appear and blink automatically | Unchanged |
  | Chat link or crafting-item double-click | Unchanged | Appear and blink for 15s; close originating screen |
  | TTC returns to normal | Disappear | Continue until expiry |
  | Craft finishes / cancelled | Disappear | Continue until expiry |
  | Provider breaks | Remove that provider's plate | Remove that provider's outline |
  | Leave and re-enter world | Restore for crafts still delayed | Never restore |

- The delayed warning keeps its #118 wording, timing context, owner-only
  delivery, and once-per-stall rules.
- The output name renders underlined with a hover hint that invites the click.
- A delayed row's tooltip shows a "Double-Click for highlighting Pattern
  Provider in a world" hint above the details line, so the locate action is
  discoverable without the chat message.
  (See [issue #241](https://github.com/cTux/ae2-crafting-time/issues/241).)
- Every locate answers with a private system message
  ("Highlighting <provider> at <coords> in <dimension>"), whatever triggered
  it. The message names the provider block (falling back to a generic
  "Pattern Provider" name), and every coordinate is a clickable shortcut
  that teleports the clicker to that position.
  (See [issue #241](https://github.com/cTux/ae2-crafting-time/issues/241).)
- Clicking the chat link closes the chat; a double-click locate closes the
  CPU screen, so the player immediately sees the highlight.
  (See [issue #240](https://github.com/cTux/ae2-crafting-time/issues/240).)
- Plates are server-authoritative, not UI cache. Opening another CPU, the
  planning screen, or closing all windows never hides a still-delayed plate.
  Plates vanish only on an explicit server clear (recovery, finish, cancel)
  or when that provider target breaks.
- Leaving the world clears all client highlights. Red plates return only
  through server login resync for crafts still delayed. Rainbow timers are
  never serialized and never return.
- A broken provider drops only its own positions from plates and edges, in
  that dimension only. "Broken" means the actual provider target is gone:
  air, missing block entity, replacement non-provider block, or a surviving
  host without a provider service. Unloaded chunks and unreadable grid state
  are unknown, not broken, and keep the highlight. This check is shared on
  all loaders.
- Blocked reasons (`NO POWER`, `NO SPACE`) never drive red. They send chat
  with a clickable record for a manual edge locate, but no plate and no
  fallback update.
- Chat links resolve against the active job plus still-valid targets. Active
  links survive reload through persisted records and per-output fallbacks.
  Links invalidate (expiry notice, no highlight) on finish, cancel, broken
  targets, or foreign owner. Finished links never recreate red or target a
  replacement block.
- When no provider position resolves for a delayed output, the name renders
  as plain text with no click action.
- The word "delayed" renders in red in English and Ukrainian.
- Clicking a link that belongs to another player, or whose record expired or
  was lost, highlights nothing and answers with a short
  expiry notice visible only to the clicker.
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
- Identity is job + network + dimension + output + provider. Identical
  outputs on different CPUs or networks track independently. Independent
  rainbow targets never replace each other. Active plates and edges are never
  silently evicted to fit a cap.
- Positions resolve at notify time, not at dispatch time, so a provider that
  moved or unloaded between dispatch and stall does not produce a stale box.
- Links are per crafting CPU and output. A new job replaces the previous
  job's links for that CPU. Finishing, cancelling, disabling the profiler,
  or clearing the scope drops its links, plates, and click records so stale
  chat links expire.
- `NO SPACE` mirrors the client row predicate on the server: the CPU reports
  it cannot store items, and the output has stored items with nothing still
  outstanding.
- `NO POWER` reuses the existing per-output dispatch-power diagnostics with
  their 20-tick freshness window.
- The persisted copy (network, output, owner, dimension, provider positions,
  display name) is a fallback, not the authority: live dispatch data wins
  whenever the job ran during this session. The stored dimension travels with
  the fallback so resync never re-derives it from the network id alone.
- Rainbow edges are never persisted. Only plates and click records persist,
  and only for active crafts.
- Persisted links never grant visibility: a locate click only serves records
  owned by the clicking player.

## Compatibility

- Support 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge, and 26.1.2 NeoForge.
- Use the same server-owned path in singleplayer and on a dedicated server.
- No new config option. Two meanings stay explicit: double-click means "any
  active crafting item", while `notifyOnDelayed` means "chat only". Locating
  and blocked warnings follow the existing `notifyOnDelayed` server setting
  for chat; plates, plate clears, and login resync ignore it.
- Update English and Ukrainian text together; the split status word keeps
  matching placeholders.
- Keep command arguments, packet fields, collection sizes, and decoded
  positions bounded and validated.
- Wire version is Forge channel protocol `14`, Fabric
  `provider_highlight_v4` (plus `provider_locate_v1`), NeoForge registrars
  `13` on 1.21.1 and 26.1.2. The highlight codec adds `networkId` and stored
  `dimension` additively with tolerant reads for older packets and saves.
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
- A locate action outside the delayed warning and CPU double-click (no
  hotkey, no screen button).
- Changing the 15-second rainbow duration in-game.

## Acceptance criteria

- The warned player sees an underlined output name with a hover hint and a
  red "delayed" word.
- When a craft becomes delayed, the owner sees blinking item-on-red plates
  with no click and no open screen; other players see nothing.
- Clicking the name or double-clicking any resolvable active crafting item
  draws thick (2-3x) rainbow-cycling edge boxes on the correct provider
  block(s) for 15 seconds without changing red plates.
- TTC recovery, finish, or cancel clears red but leaves rainbow until its
  own expiry. Provider break removes only that provider's plate and outline.
- Leave and re-enter restores red for still-delayed crafts and never
  restores rainbow.
- The clicker also gets a private "Highlighting <provider> at <coords> in
  <dimension>" message naming the provider block, with clickable coordinates
  that teleport to each position, and the chat (or CPU screen, for
  double-click) closes so the highlight is visible.
- A delayed row's tooltip carries the double-click locate hint above the
  details line.
- Clicking another player's, finished, cancelled, or broken link shows the
  expiry notice and draws nothing.
- Leaving and re-entering the world keeps the link working for an active
  craft; a still-delayed craft warns again when the owner is online.
- A craft with no resolvable provider position renders a plain name.
- Blocked warnings send chat with a clickable edge-only link and never touch
  red.
- A `NO POWER` warning arrives once per power-failure episode and returns
  after power is restored and fails again.
- A `NO SPACE` warning arrives once per storage-blocked episode and returns
  after space is freed and blocks again.
- World saves from before the feature load with empty provider state and no
  errors.
- English and Ukrainian messages keep matching placeholders.
- Packet, NBT, and message-component round trips have full test coverage.
