---
name: run-ae2-client-smoke
description: Run prepared AE2 Crafting Time repository clients or automated UI smoke tests for supported targets. Use for requests such as smoke UI test 1.20.1-forge; do not use Prism or named modpack instances.
---

# Smoke-Test Prepared AE2 Clients

## Route

- Build production and test-driver JARs only on the host. Follow
  [host build and VM staging](../../../docs/dev-client.md#host-build-and-vm-staging)
  for Java selection, the session worktree share, and artifact replacement.
- Load `use-codex-vm` before every Minecraft launch or visual check. Reuse the
  running VM; add the session worktree as its own share unless that exact
  directory already has one.
- Use the prepared clients and launchers in this repository. Never open Prism
  unless the user explicitly asks to test a modpack; that request uses
  `launch-prism-test-modpack` instead.
- Copy the host-built JARs into the exact guest-local prepared client, then
  launch its installed loader directly. Preserve the requested target, profile,
  and scenario. Use the single-launch suite for the full compatible graph,
  with a fresh world and screenshots per case. Run clients sequentially.
- Run `scripts/run-ui-smoke.ps1` on the host for all four compatible suites.
  Use `run-ui-smoke-matrix.ps1 -Target <id>` for one target or `-Latest` for
  diagnostics. These commands build on the host and dispatch only packaged
  artifacts to the guest's installed native loader. The dispatcher shares the
  exact worktree; `-GuestSourceRoot` selects an existing share when needed.
- The guest requires matching prepared `launch.json` manifests as documented
  in `docs/dev-client.md`. Missing or mismatched native installations are setup
  failures. Never substitute a guest Gradle build or a Prism modpack.
- Read current status, evidence, and launcher logs from `build/ui-smoke`.
  Record the exact client PID and stop only that process when needed; do not
  reuse a PID from a previous run.
- Maximize the exact Minecraft window before visual inspection. Test-driver
  scenarios may maximize themselves, but verify the captured result. Confirm
  the exact client stopped before continuing; never kill Java processes broadly.

Run 1.20.1 clients on Java 17, 1.21.1 on Java 21, and 26.1.2 on Java 25.
Resolve the launch machine's `JAVA_HOME_17`, `JAVA_HOME_21`, or `JAVA_HOME_25`
with `scripts/get-java-home.ps1`; never use a path copied from the other machine.
These are guest runtime versions; Gradle and all JAR builds stay on the host.

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

For every UI smoke run, read and follow
[the screenshot archive contract](../../../docs/ui-smoke-evidence.md). Archive
evidence before the runner overwrites its scenario folder on a later attempt.

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

- Keep the live runtime on guest-local NTFS. Use the shared worktree to copy
  built JARs into that runtime and return status, logs, and evidence.
- Do not install a modpack, select an account, join a world or server, or perform
  gameplay unless the request authorizes it.
- Never substitute a loader, target, scenario, or profile. Do not reinterpret a
  build or dependency request as permission to launch Minecraft.
