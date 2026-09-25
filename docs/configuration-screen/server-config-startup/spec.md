# Generate the World Server Config on Startup

Status: ready-to-implement

Scope: Create the missing world-owned server config during logical-server startup.

Issue: [#535](https://github.com/cTux/ae2-crafting-time/issues/535)

Planning: [Technical design](technical-design.md) and [reviewed plan](implementation-plan.md).

Runtime prerequisite: verify prepared clients, Java runtimes and disposable dedicated-server fixtures before the required campaign. Their guest state has not yet been verified for this task.

## Behavior

Starting a world creates `<world>/serverconfig/ae2craftingtime-server.toml` when it is missing. The file contains every effective server setting in the existing save format, so owners can discover settings without editing Options. For singleplayer, `<world>` is the selected save under `saves`; for a dedicated server, it is the world selected by `level-name`.

Load and validate values before generating the file. If `config/ae2craftingtime-common.toml` supplies legacy values, persist those migrated values and defaults for the remaining settings. Keep the legacy file intact. If neither file exists, persist normal typed defaults.

Existing world files retain their exact bytes, including comments, unknown keys and invalid fields. The existing parser still determines their effective values. Generation failure logs the destination and cause, retains effective values in memory and lets startup continue. Read failure keeps existing recovery behavior and cannot overwrite an existing file. Launching a client without starting a logical server creates no server config.

## Compatibility and limits

Cover integrated and dedicated servers on 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge and 26.1.2 NeoForge. Change no defaults, permissions, packets, profile data, file format or in-game edit behavior. The separate missing-file report after an edit remains in [#536](https://github.com/cTux/ae2-crafting-time/issues/536).

The world owns this file on every loader. Do not create a global server-config file or add a new setting for generating it.

## Acceptance criteria

| ID | Required result |
| --- | --- |
| A1 | First startup creates a missing world file with every effective server value, including when `serverconfig` is absent. |
| A2 | Legacy values survive generation and the legacy file is unchanged. |
| A3 | Existing world files remain byte-for-byte unchanged; later starts do not rewrite generated files. |
| A4 | Generation failure logs destination and cause, preserves the effective model and allows startup. Read failure cannot overwrite existing config. |
| A5 | Verify integrated and dedicated startup on all four targets, and no server-file creation during client-only launch. |
| A6 | English/Ukrainian GuideME configuration pages and the corresponding wiki page explain the world path, generation and migration. |

Completion requires automated checks, current-head CI and retained runtime evidence. A build alone does not complete A5.
