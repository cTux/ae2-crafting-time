# NeoForge Early-Display Config Smoke Technical Design

## Evidence and root cause

Campaign `20260908T132513130Z` ran NeoForge 1.21.1 with loader `21.1.238`
and FML `4.0.43`. Its primary graph failed in `DisplayWindow.initRender` before
world loading. Bytecode line mapping identifies the null Boolean as FML's
`EARLY_WINDOW_SQUIR` entry, serialized as `earlyWindowSquir` in `fml.toml`.
The immutable archive keeps the original launcher log, but not the loader config
as it existed before the failed launch.

The prepared runner uses one persistent
`build/ui-smoke/<target>/<profile>/runtime` per worktree. It replaces JARs and
worlds but leaves `runtime/config/fml.toml` in place. The failed primary launch
was followed by an AdvancedAE launch in that same runtime, and the latter
passed early display. This makes the result depend on loader config written by
an earlier process and prevents an attributable first-start/subsequent-start
comparison.

## Fix

`prepare-ui-smoke-launch.ps1` owns native-runtime staging. For NeoForge targets,
it will snapshot any existing `config/fml.toml` into the current graph's
evidence directory, remove only that disposable runtime file, and record an
explicit absence marker when no file exists. The native loader then creates its
own version-matched config. After the process exits, `run-ui-smoke.ps1` copies
the resulting file into the same evidence directory or records that it is
absent.

The runner does not parse, patch, or supply `earlyWindowSquir`. Deleting the one
loader-owned file is smaller and safer than copying a version-specific template:
FML remains the source of defaults, and future loader keys need no runner
change. The prepared launcher and other config files remain untouched.

## Data flow

```text
existing disposable runtime/config/fml.toml
  -> prelaunch evidence copy or absent marker
  -> remove runtime fml.toml
  -> launch exact prepared NeoForge loader
  -> postlaunch evidence copy or absent marker
  -> existing semantic, visual, archive and exit gates
```

## Failure handling

- A snapshot or removal failure stops before launch.
- The post-launch capture runs for both success and failure and does not replace
  the original launcher log.
- Evidence filenames are fixed and graph-local, so later graphs cannot overwrite
  another graph's state.
- Forge and Fabric skip this NeoForge-only path.

## Coverage

The existing PowerShell contract-test pattern will exercise absent and present
`fml.toml` inputs, verify exact byte preservation in the pre-launch copy, verify
that only the runtime file is removed, and verify post-launch presence/absence
capture. The real gate is a fresh complete NeoForge 1.21.1 primary smoke.
