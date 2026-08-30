---
name: launch-prism-test-modpack
description: Test AE2 Crafting Time against supported versions or named modpacks. Use Prism in the Codex VM only for a named modpack; otherwise use `run-*.bat`.
---

# Test AE2 Crafting Time

## Route

- For a named modpack, load `use-codex-vm`, resolve the exact pack release, and
  inspect the managed instance inventory. Read
  [install-modpack.md](references/install-modpack.md) only when an exact eligible
  instance is missing. Then read
  [launch-and-verify.md](references/launch-and-verify.md).
- Without a named modpack, do not use Prism or the VM. Run the applicable
  repository-root `run-*.bat` launchers sequentially. For an all-version check,
  run every launcher. Verify startup, close that exact client, then continue.
- Do not reinterpret a build, dependency, or generic compatibility request as a
  request to test installed modpacks.

## Named Modpack Contract

1. Resolve the canonical CurseForge or Modrinth project and an exact release
   supported by `scripts/release-matrix.json`. Report ambiguity instead of
   silently choosing a similarly named pack.
2. Reuse an instance only when `instance.cfg` and `mmc-pack.json` prove the same
   managed pack release, Minecraft version, and loader. Otherwise install it
   through Prism's UI.
3. Inspect enabled JAR metadata, including nested JARs, for mod ID `ae2`. If AE2
   is absent, do not copy or launch this mod; remove the test instance and report
   it as ineligible rather than failed.
4. Select the exact release-matrix row. Unless the user supplied a JAR, build
   that row once per campaign and reuse its loader- and version-specific artifact.
   Stop as unsupported when no row matches.
5. Launch through Prism, verify the mod and startup completion from current logs
   plus the guest display, collect crash evidence when needed, and close only the
   exact tested client.
6. Report the requested and resolved pack names, release, instance ID, Minecraft
   version, loader, copied JAR, result, and failure reason.

## Boundaries

- Do not launch Minecraft from VMware's shared filesystem; stage eligible
  instances on guest-local NTFS and sync logs back afterward.
- Do not install a JAR, select an account, join a world or server, or perform
  gameplay unless the request authorizes it.
- Never substitute another loader or Minecraft version, infer mod presence from
  filenames, start duplicate clients, or kill Java processes broadly.
- Retry a repeated provider metadata failure once. If it repeats or stalls at
  completion, record the install as blocked and continue the batch.
