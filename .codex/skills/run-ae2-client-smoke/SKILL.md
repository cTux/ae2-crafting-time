---
name: run-ae2-client-smoke
description: Run prepared AE2 Crafting Time repository clients or automated UI smoke tests for supported targets. Use for requests such as smoke UI test 1.20.1-forge; do not use Prism or named modpack instances.
---

# Smoke-Test Prepared AE2 Clients

## Route

- Load `use-codex-vm` before every Minecraft launch or visual check. Reuse the
  running VM and its read/write `projects` share; do not add a duplicate share.
- Use the prepared clients and launchers in this repository. Never open Prism
  unless the user explicitly asks to test a modpack; that request uses
  `launch-prism-test-modpack` instead.
- For Forge 1.20.1 UI smoke, dispatch
  `scripts/invoke-ui-smoke-codexvm.ps1` with the requested `-Scenario`, `-Latest`,
  or `-Interactive` option. Prefer OpenSSH; use `-Transport Vmrun` when the
  OpenSSH path cannot be dispatched reliably.
- For another supported target, run its `scripts-run/run-*.bat` launcher inside
  the VM. Use a matching `-latest` launcher only when the latest profile is
  requested. Run all-version clients sequentially and report each separately.
- Read `status.json`, evidence, and launcher logs from `build/ui-smoke` instead
  of polling build progress through VNC. The status owns the exact PID; stop
  only that process through the same dispatcher when needed.
- Maximize the exact Minecraft window before visual inspection. Test-driver
  scenarios may maximize themselves, but verify the captured result. Confirm
  the exact client stopped before continuing; never kill Java processes broadly.

Run 1.20.1 clients on JDK 17 and both NeoForge clients on JDK 21. The 26.1.2
project selects its installed JDK 25 toolchain; do not start the multi-project
build on JDK 25.

## Dependency audits

Treat `scripts/run-client-versions.json` as the candidate inventory and known
issues list. Verify newest releases from official loader or project metadata,
then run the latest profile. Keep an incompatible project in the latest set;
exclude it only from `compatible` with a concrete `reason` and any `issue_url`
or `upstream_issue_url`.

Promote versions into `compatible` only after the complete target graph starts
and the requested smoke checks pass. Create or comment on upstream or local
issues only when the user requests it; include reproduction evidence and link
the two issues when both exist.

Immediately before the final complete compatible-profile smoke, fetch `origin`
and rebase the clean work branch onto `origin/master`. If a later base change
touches production, build, dependency, fixture, or driver code, rebase and run
that final smoke again.

## Timing Report

Start a wall-clock timer before the first smoke-test action. Record actual phase
start and end timestamps from the host, status file, and logs. End every
smoke-test report with this table and a total row:

| Part of the smoke UI testing task | Time | Why it took that long |
|---|---:|---|

Include each material phase: VM setup, client staging or dependency resolution,
build, Minecraft/mod loading, world and UI assertions, evidence review, cleanup,
and every failed attempt or retry. Use concrete observed causes rather than a
generic explanation. Keep the successful run's runtime separate from total task
time. Mark an unavailable duration as `not measured`; do not estimate it.

## Boundaries

- Keep the staged checkout and live runtime on guest-local NTFS; Loom is not
  reliable on the VMware shared folder. Sync only status, logs, and evidence.
- Do not install a modpack, select an account, join a world or server, or perform
  gameplay unless the request authorizes it.
- Never substitute a loader, target, scenario, or profile. Do not reinterpret a
  build or dependency request as permission to launch Minecraft.
