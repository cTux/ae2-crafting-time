# Verify server options authority and synchronization

Tracking: [#512](https://github.com/cTux/ae2-crafting-time/issues/512).

The [configuration specification](spec.md) defines ownership. This checklist
covers its remaining dedicated-server acceptance after
[PR #510](https://github.com/cTux/ae2-crafting-time/pull/510).
An integrated-owner save/reopen check alone does not prove server permissions.

## Boundaries to inspect

`OptionsSession` and `ClientServerOptions` maintain the editor and effective
snapshot. `ServerOptionsRuntime.accept` checks permission and revision before
saving the world-owned file, applying the new configuration, and distributing
the next revision. `ServerOptionsPermission` has version-specific variants.
`ServerOptionsWire` owns bounded decoding and validation.

The server is authoritative even if the client changes its editable flag.
Use a disposable world and controlled test inputs for rejection cases.

## Required cases

| Actor or event | Required result |
| --- | --- |
| Normal player opens Server tab | Effective values visible, server editing disabled; Client settings remain editable |
| Integrated owner or permission-level-4 operator saves | New effective values acknowledged, world file saved |
| Second connected client | Receives the updated effective revision |
| World restart | Saved server values survive |
| Old editor revision submits | Rejected without overwriting newer values; effective snapshot or actionable error returned |
| Operator loses permission while editing | Submission rejected server-side |
| Forged editable flag, malformed or oversized update | Rejected without changing effective values or disk contents |
| Dedicated-server startup and updates | No client screen classes loaded |

Capture file contents before and after rejected edits. An unchanged UI alone
cannot prove the server rejected a forged payload. Separate invalid payload
checks from valid authorized save failures.

## Campaign and completion evidence

Run applicable supported loaders, retaining exact commit, loader/dependency
identity, actor permission, client/server logs, revisions, UI captures, and
save/restart observations. Mark absent targets as unverified rather than
generalizing from Forge. Reuse existing wire/permission tests and prepared
dedicated-server fixtures; preserve decoder limits and save-before-apply.

[#536](https://github.com/cTux/ae2-crafting-time/issues/536) concerns a missing file
after a reported in-game edit. The [startup-generation scope](server-config-startup/spec.md)
is separate: creating defaults on startup does not establish edit authorization.
Link fixes, CI, and retained runtime evidence before closing #512. This page
adds a verification plan, not a completed permission audit.
