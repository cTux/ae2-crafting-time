# Launch And Verify

Read this after an eligible named-modpack instance exists.

## Stage The Instance

- Reconfirm `instance.cfg` and `mmc-pack.json`, then inspect enabled mod JAR
  metadata, including nested archives, for mod ID `ae2`.
- Copy eligible instances to a temporary guest-local NTFS root and point Prism
  there for the campaign. Java watch registration fails on VMware's shared
  filesystem with `java.io.IOException: Incorrect function`.
- After testing, sync logs and crash reports to the shared instance, restore
  Prism's normal shared root, and remove only the marked temporary copy.

## Install The Test JAR

- Match the Minecraft version and loader to `scripts/release-matrix.json`.
- Unless the user supplied an exact artifact, derive the distribution filename
  from that row and the current `modVersion`. Build each required row once and
  reuse its verified artifact across compatible instances.
- Remove existing enabled `ae2-crafting-time-*.jar` files, copy the selected JAR,
  and verify exactly one enabled copy remains. Replace directly; do not create
  backups.

## Launch And Decide

- Confirm Prism does not already mark the instance as running, then launch its
  selected entry inside the VM.
- Keep Prism's console visible. Verify the newest populated `latest.log` belongs
  to this launch, shows AE2 Crafting Time loaded, and reaches the startup marker.
  Also confirm the title screen in the current guest framebuffer.
- A warning alone is not failure. A fatal exception, crash report, unresolved
  dependency or module error, or exit before startup completion is failure.
- If the process exits or stalls, inspect the newest crash report and current
  log before deciding.

## Finish

- For a smoke test, close the exact Minecraft window after the startup result and
  confirm Prism returns it to a non-running state. Prefer normal close or in-game
  quit; use Prism's selected-instance **Kill** only if that exact client is stuck.
- Apply the same cleanup after failure. Never kill Java processes broadly.
- If testing goes beyond startup, close only after reaching the requested state
  and collecting its evidence.
