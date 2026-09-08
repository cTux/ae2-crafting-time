# NeoForge Early-Display Config Smoke Spec

## Goal

Make the prepared NeoForge 1.21.1 smoke start from an attributable loader
configuration so a previous launch cannot decide whether the next primary graph
starts. Tracking issue: [#357](https://github.com/cTux/ae2-crafting-time/issues/357).

## Restored behavior

- **ED-01:** Every prepared NeoForge graph starts with its own loader-owned
  `config/fml.toml` state inside the disposable runtime.
- **ED-02:** The runner preserves the pre-launch and post-launch loader config
  state, or records that either state was absent, before another graph can
  replace it.
- **ED-03:** A missing or stale loader key cannot make the primary graph fail
  while a later graph succeeds only because the first launch repaired the file.
- **ED-04:** The original failed campaign and log stay unchanged. A retry uses a
  new campaign and never reclassifies the old result.
- **ED-05:** Forge, Fabric, prepared launcher installations, production configs,
  and player instances are unchanged.

## Acceptance criteria

- A NeoForge 1.21.1 launch with no `fml.toml` creates a complete loader config
  and reaches normal startup.
- Repeating the same primary graph uses a newly isolated loader config and
  reaches the same startup state.
- Evidence identifies the loader version and shows whether `earlyWindowSquir`
  existed before and after launch. The runner never invents a default value.
- The complete 32-case NeoForge 1.21.1 primary graph passes in one client and
  one loaded disposable world, followed by a confirmed normal process exit.
- The runner's focused contract test covers absent and existing loader config
  inputs without launching Minecraft.

## Non-goals

- Patching or replacing NeoForge/FML.
- Editing a player's loader config.
- Treating a later successful graph as proof that an earlier failed graph
  passed.
- Changing mod selection, fixture behavior, smoke assertions, or visual gates.
