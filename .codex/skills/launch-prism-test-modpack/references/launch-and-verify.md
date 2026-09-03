# Launch And Verify

Read this after an eligible named-modpack instance exists in Prism's **Codex** group.

## Stage The Instance

- Reconfirm **Codex** group membership, `instance.cfg`, and `mmc-pack.json`, then
  inspect enabled mod JAR metadata, including nested archives, for mod ID `ae2`.
- Copy eligible instances to a temporary guest-local NTFS root and point Prism
  there for the campaign. Java watch registration fails on VMware's shared
  filesystem with `java.io.IOException: Incorrect function`.
- Keep the staged instance in **Codex** too. Never stage a pack from another group.
- After testing, sync logs and crash reports to the shared instance, restore
  Prism's normal shared root, and remove only the marked temporary copy.

## Install The Test JAR

- Match the Minecraft version and loader to `scripts/release-matrix.json`.
- Unless the user supplied an exact artifact, derive the distribution filename
  from that row and the current `modVersion`. Build each required row on the
  host once and reuse its verified artifact across compatible instances. Build
  the matching test-driver JAR on the host when the requested smoke needs it.
- Add the session worktree to the VM's shared folders, reusing an exact existing
  share. Copy its built JARs into the guest-local instance; never run a JAR build
  or a Gradle launcher inside CodexVM.
- Replace existing enabled `ae2-crafting-time-*.jar` files with the selected
  production JAR and the matching test-driver JAR when needed. Verify one
  production copy and at most one required driver remain. Replace directly;
  do not create backups. Compare SHA-256 hashes of every copied JAR against
  its host artifact before launch.

## Launch And Decide

- For an SVGA3D atlas-size failure, distinguish `GL_MAX_TEXTURE_SIZE` from
  Minecraft's square-allocation probe. Read the
  [guarded driver probe](../../../../docs/test-driver/technical-design.md#codexvm-rectangular-atlas-probe)
  before opting in. Verify real allocation and startup inside CodexVM; do not
  substitute host testing or downscale the pack's assets to claim a VM pass.

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
