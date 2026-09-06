# In-game Configuration Screen Implementation Plan

Implement [#117](https://github.com/cTux/ae2-crafting-time/issues/117) from the
[specification](spec.md) and [technical design](technical-design.md).

## 1. Lock the typed config contract

- Add Minecraft-free client/server setting models with the exact defaults,
  ranges, sort values, validation, reset, and ownership from the spec.
- Route current config access through `ServerConfig`; add the client getters used
  by UI code.
- Add focused tests for defaults, every boundary, NaN/infinity, invalid RGB,
  per-field fallback, and shrinking retained-sample limits.

Gate: every setting has one owner and one tested default/range.

## 2. Split storage and migrate existing files

- Register client/server TOML specs on Forge and both NeoForge targets.
- Extend Fabric's existing parser with typed client and world-server files plus
  atomic save.
- Migrate known values from `ae2craftingtime-common.toml` only when the new owner
  key is absent; keep the legacy file untouched.
- Test missing, valid, malformed, partial, interrupted-save, and repeated-migration
  cases on all loader implementations.

Gate: existing values survive, invalid values fall back narrowly, and dedicated
servers never load client config classes.

## 3. Build the native screen

- Add the shared working-copy/descriptor state and the smallest `mc1201` and
  `mc2612` native widget adapters.
- Implement categories, help/default/range text, availability states, save,
  cancel, section reset, full reset, numeric/color validation, and save errors.
- Add matching English and Ukrainian labels, descriptions, errors, ownership,
  availability, and apply-timing strings.

Gate: keyboard, mouse, narration, scrolling, focus, and parent-screen return work
at supported GUI scales in both code seams.

## 4. Register loader entry points

- Register Forge and NeoForge config-screen factories from client-only setup.
- Add the optional Fabric Mod Menu entrypoint and metadata without making Mod
  Menu a runtime requirement.
- Add startup/packaging checks for client-only isolation and optional dependency
  behavior.

Gate: every loader details page opens the screen where supported; Fabric starts
cleanly with and without Mod Menu.

## 5. Apply client settings

- Replace hard-coded surface visibility, detail/status visibility, sort defaults,
  TTC/status/total colors, and badge color/opacity at their shared UI seams.
- Keep calculations, packets, current per-screen sort cycling, and the Controls
  binding unchanged.
- Cover Crafting Plan, Crafting Status, Crafting Tree, and ME Requester with
  focused rendering/behavior checks.

Gate: each option changes only its named surface and persists across restart.

## 6. Apply and expose server settings

- Replace the central delay constants with validated server values and preserve
  profiling/retention/filter/chat/notification behavior.
- Add the effective-server-config snapshot and fixed typed update packet to the
  existing protocol; bump client/server protocol versions together on all four
  targets.
- Send snapshots on login/reload. Require integrated-owner or permission-level-4
  authority for updates; validate direction, context, revision, lengths, enums,
  and ranges before one atomic save/apply/broadcast.

Gate: two clients observe the same effective server values and cannot override
them; valid config reloads do not corrupt samples or active crafts.

## 7. Complete cross-target verification

- Run unit and integration tests, translation parity, documentation/link checks,
  `git diff --check`, and all four release-matrix builds.
- Run prepared-client smoke on all targets for loader entry, save/cancel/reset,
  invalid input, immediate client changes, persistence, authority/restart text,
  and representative optional displays.
- Start dedicated servers on every target; run Fabric with and without Mod Menu;
  audit player JARs for test-driver content and unintended dependencies.

Completion gate: every acceptance criterion has passing automated or retained
runtime evidence, screenshots are reviewed, all processes/worlds are cleaned up,
and no repository-owned warning remains.
