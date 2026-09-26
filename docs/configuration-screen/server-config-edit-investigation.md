# Missing server config after an in-game edit

Tracking: [#536](https://github.com/cTux/ae2-crafting-time/issues/536).

A NeoForge 1.21.1 player reported that the server config was still absent after
changing a Server option and leaving the game soon afterward. The report does
not establish a failed save, missing permission, or a mistaken directory.

## Expected save and location

A valid authorized edit submitted with **Done** and acknowledged by the server
must persist promptly to `<world>/serverconfig/ae2craftingtime-server.toml`.
For singleplayer, inspect the selected world under `saves`; for a dedicated
server, inspect the world selected by `level-name`. The global `config`
directory is not the world-owned server file's location.

Reopening Options and restarting the world must preserve the accepted value.
A rejected or failed save needs actionable feedback. Cancel is not a save,
and seeing an edited draft is not proof the server accepted it.

## Reproduction and source trace

1. Start with an integrated NeoForge 1.21.1 world, then a dedicated server with
   a permission-level-4 operator and a normal player. Preserve existing files.
2. Record the exact setting, old/new value, Done or Cancel action, permission,
   save indicator, acknowledgement, server log, and world path inspected.
3. Trace `OptionsSession`, the update packet, and `ServerOptionsRuntime.accept`.
   Check receipt, authorization, revision, decoded values, disk write, and the
   effective snapshot returned to clients. Inspect rejected and I/O-failure paths.
4. Inspect the file immediately after acknowledgement and after exit. Reopen
   Options and restart the same world. Compare the persisted value without
   overwriting unrelated settings.
5. Classify the evidence: wrong path/discoverability, rejected edit, write
   failure, or another reproduced cause. If no save defect exists, improve path
   and feedback guidance instead of adding a second writer or arbitrary delay.

Record source, target, exact steps, logs, before/after values, and reviewed UI
evidence. Redact private world/server paths before publishing.

## Related scope and completion

[Startup generation](server-config-startup/spec.md), tracked in
[#535](https://github.com/cTux/ae2-crafting-time/issues/535), concerns creation
without an edit. It does not prove this reported save succeeded.
[Dedicated-server verification](server-options-verification.md), tracked in
[#512](https://github.com/cTux/ae2-crafting-time/issues/512), covers the broader
permission and synchronization matrix.

Link a reproduced cause or evidenced no-defect conclusion, any fix, persistence
checks, and current-head CI on #536. Update user guidance when the finding
changes it. This investigation document does not claim the report resolved.
