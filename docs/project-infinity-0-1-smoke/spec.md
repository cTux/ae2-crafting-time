# Project Infinity 0.1 Full UI Smoke Spec

## Goal

Prove that the current AE2 Crafting Time build starts and behaves correctly in
the exact Project Infinity 0.1 pack graph through one full, attributable UI
smoke campaign in CodexVM. A successful launch alone is not a pass.

Tracking issue: [#303](https://github.com/cTux/ae2-crafting-time/issues/303).

## Fixed target

The campaign targets this release, verified on 2026-09-06 from the
[official CurseForge file](https://www.curseforge.com/minecraft/modpacks/project-infinity-0-1/files/8664964):

- Project: Project Infinity 0.1 (`1266680`)
- Release: `0.0.51.3 HOTFIX`
- File: `Project Infinity 0.1-0.0.51.3 HOTFIX.zip` (`8664964`)
- Minecraft: `1.20.1`
- Loader: Forge
- AE2 Crafting Time target: `1.20.1-forge`

Do not silently switch to a newer pack file. If this file is unavailable or a
newer release should be tested instead, update this plan or get approval for
the exact replacement first.

## Requirements

- **PI-01:** Install or reuse only an instance in Prism's **Codex** group whose
  metadata proves the fixed project, file, Minecraft version, and loader.
- **PI-02:** Inspect enabled top-level and nested JAR metadata for mod ID `ae2`.
  An instance without AE2 is ineligible and must not receive or launch AE2
  Crafting Time.
- **PI-03:** Preserve the pack graph. The only allowed mod changes are adding or
  replacing this campaign's AE2 Crafting Time production and test-driver JARs,
  built for the repository's `1.20.1-forge` release row. Do not add, remove, or
  upgrade pack dependencies to make a scenario available.
- **PI-04:** Run the full core contract: `standard-ae2` (all six leaves),
  `craft-plan`, `no-space-status`, `no-provider-status`, `no-power-status`,
  `no-target-status`, `input-blocked-status`, and `locked-status`.
- **PI-05:** Inspect the installed pack inventory against
  `scripts/ui-smoke-coverage.json` and `scripts/ui-smoke-forge-suite.json`.
  Add every direct UI or direct behavior scenario whose required mod is enabled
  and whose installed version reaches a supported adapter. A missing or
  contradictory mapped scenario is a setup failure, not an exclusion. Record
  every other discovered AE2-related project as coexistence, unsupported, not
  applicable, or not tested with a concrete reason.
- **PI-06:** Expand `standard-ae2` on the host, prepare a fresh disposable world
  for every selected leaf, then run the selected pack cases sequentially in one
  Prism launch. No case may depend on another case's world or result.
- **PI-07:** Use English (`en_us`), an 8 GiB client heap, a maximized Minecraft
  window, guest-local NTFS runtime storage, and the exact Java version selected
  by `scripts/set-prism-java.ps1`.
- **PI-08:** A pack case passes only when its driver assertions pass and every
  required visual checkpoint has been opened and inspected. Screenshots support
  visible UI claims; they do not prove server-only state.
- **PI-09:** Keep newest-adapter policy checks that the pack cannot satisfy in
  separate prepared fixtures. Their results supplement the repository smoke
  policy but never change or upgrade the Project Infinity graph and never count
  as a Project Infinity pass.
- **PI-10:** Archive immutable pack identity, enabled-mod inventory, commit and
  JAR hashes, suite plan, per-case semantic results, screenshots, current logs,
  failures, timestamps, cleanup state, and measured phase timings under the
  screenshot evidence contract.
- **PI-11:** Stop after a failed case when the driver cannot safely advance in
  the same client. Preserve completed results, mark later cases `NOT_RUN`, close
  only the exact tested client, and keep the failed campaign failed even if a
  later diagnostic rerun passes.
- **PI-12:** Remove only the marked temporary guest-local copy after evidence is
  synchronized. Leave other Prism groups, instances, worlds, accounts, and Java
  processes untouched.

## Result rules

The Project Infinity campaign is `PASS` only when every selected pack case
passes, all required images have been inspected, the exact client is closed,
and the archive is complete. Use `FAIL_SETUP` for installation, eligibility,
artifact, fixture, Java, or startup failures; `FAIL` for wrong or missing
behavior; and `NOT_RUN` for later cases skipped after a failure. Missing driver
coverage is not a pass.

A separate prepared-fixture result may be `PASS`, `FAIL`, or `NOT_REQUIRED`
under the smoke policy, but it is reported outside the pack result.

## Non-goals

- Testing a different Project Infinity release, Minecraft version, or loader.
- Modifying the pack to maximize integration coverage.
- Treating startup, log absence, translation keys, or screenshots alone as a
  full UI result.
- Replacing unit, packet, packaging, or prepared-client checks.
- Testing multiplayer, progression, quests, or unrelated pack behavior.

## Acceptance criteria

- **PI-A1:** The archive proves CurseForge file `8664964`, the Codex-group
  instance, Forge 1.20.1, enabled AE2, and the selected release-matrix row.
- **PI-A2:** Host and staged production/test-driver JAR SHA-256 hashes match,
  with one enabled copy of each required artifact.
- **PI-A3:** The saved expanded suite contains every PI-04 core leaf exactly
  once and every applicable PI-05 integration case exactly once.
- **PI-A4:** One Prism client runs that suite with fresh worlds; all selected
  leaves pass or retain attributable failure and `NOT_RUN` results.
- **PI-A5:** Every required semantic check maps to inspected evidence from this
  run, and the archive contains no account data, tokens, unrelated worlds, or
  private server details.
- **PI-A6:** The exact client is no longer running, Prism is restored to its
  normal root, and cleanup touches only the marked temporary instance.
- **PI-A7:** The final report separates the Project Infinity result from any
  prepared newest-adapter result and ends with measured phase timings plus a
  total.
