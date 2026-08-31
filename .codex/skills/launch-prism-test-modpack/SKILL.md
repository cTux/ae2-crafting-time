---
name: launch-prism-test-modpack
description: Audit dependencies or run AE2 Crafting Time client, UI, version-matrix, and named-modpack smoke tests in CodexVM. Do not use for build-only verification.
---

# Test AE2 Crafting Time Clients

## Always use CodexVM

- Load `use-codex-vm` before every Minecraft launch or visual check. Reuse the
  running VM and its read/write `projects` share; do not add a duplicate share.
- Read the repository through `\\vmware-host\Shared Folders`, then stage the
  checkout and live Minecraft runtime on guest-local NTFS before launching.
  Loom writes below the checkout and fails on VMware shared-folder rename and
  watch semantics. Pass a guest-local `-RuntimeDirectory` to the staged
  repository launchers.
- Clients use the repository's 8 GiB heap setting. Maximize the exact Minecraft
  window through VNC before visual inspection. Test-driver scenarios maximize
  themselves, but still verify the resulting window.
- Run Gradle on JDK 17 for the 1.20.1 targets and JDK 21 for both NeoForge
  targets. The 26.1.2 project selects its installed JDK 25 toolchain itself;
  starting the multi-project Gradle build on JDK 25 breaks Fabric configuration.
- Run clients sequentially. Confirm the exact client stopped before continuing;
  never kill Java processes broadly.

## Route

- For a named modpack, resolve the exact pack release and inspect the managed
  instance inventory. Read
  [install-modpack.md](references/install-modpack.md) only when an exact eligible
  instance is missing. Then read
  [launch-and-verify.md](references/launch-and-verify.md).
- For a supported target, run its `scripts-run/run-*.bat` launcher inside the
  VM. Use the matching `-latest` launcher only for the latest profile. For an
  all-version sweep, run all four supported targets and report each separately.
- For Forge 1.20.1 test-driver UI work, run `scripts/run-ui-smoke.ps1` inside the
  VM with the requested `-Scenario`, `-Latest`, or `-Interactive` option.
- Do not reinterpret a build, dependency, or generic compatibility request as a
  request to test installed modpacks.

## Dependency audits

Treat `scripts/run-client-versions.json` as both the candidate inventory and
known-issues list. Verify newest releases from official loader or project
metadata, then run the latest profile. Keep an incompatible project in the
latest set; exclude it only from `compatible` with a concrete `reason` and any
`issue_url` or `upstream_issue_url`.

Promote versions into `compatible` only after the complete target graph starts
and the requested smoke checks pass. Create or comment on upstream or local
issues only when the user requests it; include reproduction evidence and link
the two issues when both exist.

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

- Do not place a live Minecraft runtime on VMware's shared filesystem; stage it
  on guest-local NTFS and sync evidence back afterward.
- Do not install a JAR, select an account, join a world or server, or perform
  gameplay unless the request authorizes it.
- Never substitute another loader or Minecraft version, infer mod presence from
  filenames, start duplicate clients, or kill Java processes broadly.
- Retry a repeated provider metadata failure once. If it repeats or stalls at
  completion, record the install as blocked and continue the batch.
