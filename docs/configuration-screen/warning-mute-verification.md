# Verify personal craft-warning mute

Tracking: [#513](https://github.com/cTux/ae2-crafting-time/issues/513).

This is the remaining multiplayer verification for the
[configuration screen](spec.md), following implementation
[PR #510](https://github.com/cTux/ae2-crafting-time/pull/510).
The reported single-client Forge save/reopen result does not establish isolation
between players. No new runtime result is claimed here.

## Fixture and ownership

Use two clients on one dedicated server and a reproducible craft that generates
delayed and blocked private warning chat, including repeated delay episodes.
Give each client an applicable owned craft; do not assume all private messages
are broadcast. Neither player needs operator permission for the local
`receiveCraftWarnings` preference.

The client saves its preference through `ClientOptionsRuntime` and sends
`WarningPreferenceC2S`. Inspect `WarningPreferenceServer` and
`WarningPreferences` when delivery disagrees with the selected value.
Keep the server-owned notification controls distinct from personal preferences.
Current source gates delayed/blocked warnings with `notifyOnDelayed`; detail/reset
notices use `showChatMessages`. Verify each family against its own control.

## Scenario matrix

1. With both clients opted in and server notifications enabled, confirm each
   receives its applicable delayed and blocked warning.
2. Mute A using Done. Start a new warning episode for each player: A receives
   none, B still receives its own warning. Confirm B's saved preference is unchanged.
3. Re-enable A and trigger a fresh episode. A's warnings resume without changing B.
4. Repeat with B muted and A enabled to detect accidental global state.
5. Exercise `notifyOnDelayed` and `showChatMessages` independently. Confirm the
   corresponding server-global messages are suppressed for both players; also
   check detail/reset notices separately so these two controls are not conflated.
   An observed mapping that contradicts the specification is a finding, not a pass.
6. Disconnect and reconnect each client. Verify the saved personal choice is
   resynchronized and one player's old connection cannot change the other's choice.

Keep episode deduplication and notification timing intact. No result requires
adding extra notifications or changing who owns a craft.

## Evidence and completion

Record source commit, target, timestamps, both client logs and screens, craft
ownership, client preferences, server settings, and expected/actual delivery for
each step. Cover applicable supported loaders; explicitly list every untested
target. Reuse prepared fixtures and existing preference tests rather than adding
a separate notification framework.

Report failures with their minimal reproducer and link fixes plus current-head
CI. Close this verification issue only when its acceptance is evidenced; merging
this checklist does not prove multiplayer behavior.
