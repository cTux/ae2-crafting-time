# World Server Config Startup Design

Lifecycle: see the [scope status and criteria](spec.md).

## Current path and root cause

At investigation revision `f71f5156366305836cde564316e40adec79a3afc`, all four `Ae2CraftingTime` loader initializers call the shared `ServerOptionsRuntime.initialize` from `ServerStartedEvent` or Fabric's `SERVER_STARTED`. The initializer resolves `server.getWorldPath(LevelResource.ROOT)` plus `serverconfig/ae2craftingtime-server.toml` and calls `ServerConfigFile.load`.

That loader reads a regular world file, otherwise known legacy common-file keys, otherwise defaults. Neither method writes a missing file. The only production caller of `ServerConfigFile.save` is `ServerOptionsRuntime.accept`, after authorized edit validation. Startup therefore cannot generate a file without an edit.

Sources:

- `shared/src/main/java/com/ctux/ae2craftingtime/core/ServerConfigFile.java` and its matching `shared/src/test/java` test;
- `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/ServerOptionsRuntime.java`;
- `versions/<target>/src/main/java/com/ctux/ae2craftingtime/mc1201/Ae2CraftingTime.java` on all four targets.

Both `ProfilerBridge` variants and notification, stats and calculation consumers read the same runtime model and need no changes. Client-only initialization does not invoke logical-server startup.

## Shared change

Keep startup creation and recoverable write handling in Minecraft-free `ServerConfigFile.load`, which has one production caller. After successful parsing, save the effective model only when the destination is absent. Preserve every existing destination; a non-regular-file result is not permission to replace it.

Reuse `save`: it serializes every server-owned `OptionFeature` plus `maxSamples`, `outlierMultiplier`, `minimumNoProgressSeconds` and `typicalDurationMultiplier`. Its `ClientConfigFile.saveLines` backend already creates parents and atomically replaces through a temporary sibling. No new serializer, loader backend or dependency is needed.

Catch generation `IOException` separately from parsing. Log the destination and exception using the existing server-config logger, then return the successfully loaded model. Existing read errors still reach runtime recovery; do not generate from partially read values. A failed write after migration must never enter the runtime's read-error handler and discard migrated settings.

## Boundaries and tests

Never normalize or add keys to an existing world file at startup. Keep legacy sources untouched, preserve current known-key migration, and leave authorized edit revisions and acknowledgment unchanged. Introduce no client references, wire changes or persisted-data version changes.

Use `ServerConfigFileTest` with real temporary files. A regular file where a parent directory is required gives deterministic write failure without relying on OS permission behavior. Assert effective returned values and file bytes. Cover all new shared lines and branches under the existing 100% gate.

## Runtime proof

Prepared-client dispatch, native launch manifests, dedicated provisioning and the shared dedicated `startup-only` case already exist. The startup-only case currently proves startup/shutdown, not generated config content. Add a bounded assertion or retain post-run file checks using the existing launch/evidence paths. Source availability does not prove guest installation readiness.

See the [plan](implementation-plan.md) for preflight and launch budget. Do not add a general test framework or provisioning system for this fix.
