# Project Infinity 0.1 Full UI Smoke Implementation Plan

Execute this plan later as one manual named-modpack campaign linked to
[#303](https://github.com/cTux/ae2-crafting-time/issues/303). Do not run the pack
as part of this documentation change.

## Phase 1: Lock the test input

1. Start the wall-clock timer and record phase timestamps from actual events.
2. Reopen official CurseForge project `1266680` and file `8664964`. Stop for an
   explicit decision if the fixed file is unavailable or a different release is
   requested; do not silently follow `latest`.
3. Start or reuse CodexVM and inspect only Prism's **Codex** group.
4. Reuse an instance only when `instance.cfg` and `mmc-pack.json` prove Project
   Infinity `0.0.51.3 HOTFIX`, Minecraft 1.20.1, Forge, and file `8664964`.
   Otherwise install that exact file through Prism into **Codex**.
5. Verify the installed instance folder and save its identity metadata.

Completion gate: one complete, exact Codex-group instance is identified without
launching Minecraft.

## Phase 2: Prove eligibility and derive coverage

1. Inventory enabled top-level and nested JAR metadata. Stop as ineligible if
   mod ID `ae2` is absent; remove only a newly created ineligible test instance.
2. Record project IDs, mod IDs, versions, filenames, and hashes for the installed
   graph.
3. Select release row `1.20.1-forge` from `scripts/release-matrix.json`.
4. Start with `standard-ae2`, `craft-plan`, `no-space-status`,
   `no-provider-status`, `no-power-status`, `no-target-status`,
   `input-blocked-status`, and `locked-status`.
5. Join the inventory to `scripts/ui-smoke-coverage.json`; add every applicable
   Forge direct UI/behavior scenario, preserve Forge-suite order, and document
   every exclusion or unknown AE2 addon. Stop with a setup failure if an enabled
   mapped integration has a missing, contradictory, or unsupported fixture.
6. Resolve and record the adapter ID each selected integration will exercise.
   List any separate newest-adapter prepared fixtures required by SP-01 through
   SP-04.

Completion gate: the saved requested list and expanded leaf list cover every
core case and every eligible installed integration exactly once without changing
the pack.

## Phase 3: Build and stage the test artifacts

1. On the host, build `:mc_1_20_1_forge:distMod` and
   `:mc_1_20_1_forge:testDriverJar` with verified Java toolchain paths.
2. Record the commit, artifact names, and SHA-256 hashes.
3. Share the exact session worktree with CodexVM, reusing only an exact existing
   share.
4. Copy the managed Prism instance to a marked guest-local NTFS staging root
   while preserving **Codex** membership.
5. Run `scripts/set-prism-java.ps1 -InstanceDirectory <staged-instance>` and
   verify Java 17 plus the 8 GiB client heap.
6. Replace old enabled AE2 Crafting Time production/driver copies in the staged
   instance. Verify one production JAR, one driver JAR, and matching host/guest
   hashes.

Completion gate: the untouched pack graph plus the two verified test artifacts
is ready on guest-local storage.

## Phase 4: Prepare the single-launch suite

1. Create a new campaign output directory; never reuse an earlier result path.
2. Pass the requested scenarios to `scripts/prepare-ui-smoke-suite.ps1` with
   target `1.20.1-forge`, the staged runtime, and the new output directory.
3. Inspect the emitted plan. Confirm `standard-ae2` expanded to six leaves,
   every case has a unique fresh world, and the count/order matches Phase 2.
4. Save the coverage join, exclusions, adapter expectations, pack identity,
   artifact hashes, and suite plan before launch.

Completion gate: immutable preflight evidence proves exactly what will run.

## Phase 5: Run and inspect Project Infinity

1. Confirm the staged instance is not already running, then launch it once
   through Prism with the suite property.
2. Keep Prism's console visible. Verify the new log belongs to this launch,
   AE2 Crafting Time and its driver loaded, startup completed, and the title
   screen is visible.
3. Maximize the exact Minecraft window before scenario observation.
4. Let the driver advance the fresh worlds sequentially and capture every
   required checkpoint. Inspect every saved screenshot after the run; use live
   inspection too when the driver pauses or a failure needs diagnosis.
5. On failure, capture the current screen, semantic result, log, and crash report
   if present. Preserve passed cases and mark later unsafe cases `NOT_RUN`.
6. Do not relaunch automatically. Put any approved diagnostic retry in a new
   attempt directory.

Completion gate: every selected pack leaf has an attributable PASS, FAIL, or
NOT_RUN result and every required image from this attempt was inspected.

## Phase 6: Run policy supplements

1. For each newest-adapter obligation the fixed pack graph could not exercise,
   use the matching prepared `1.20.1-forge` focused fixture after the pack client
   has stopped.
2. Keep its artifacts, runtime, result, and evidence separate from the Project
   Infinity campaign.
3. Report an unavailable required newest-adapter fixture as an unmet repository
   smoke gate, not as a Project Infinity failure or pass.

Completion gate: every applicable SP-01 through SP-04 obligation is either
proved separately or named as unmet without altering the pack result.

## Phase 7: Archive, clean up, and report

1. Request normal shutdown; if necessary, kill only Prism's exact selected
   instance. Confirm Prism marks it non-running.
2. Copy current logs, crash evidence, results, screenshots, inventories, plans,
   and hashes to the session evidence archive.
3. Restore Prism's normal shared root and remove only the marked temporary
   guest-local instance after synchronization succeeds.
4. Inspect every saved image and reconcile `report.md` against the requested
   scenarios, expanded leaves, semantic results, and exclusions.
5. End the report with the required measured timing table for setup,
   installation, staging, launch, UI verification, retries, evidence collection,
   cleanup, and total time. Use `not measured` rather than estimates.
6. Report requested/resolved pack identity, instance ID, Minecraft, loader,
   production/driver JARs and hashes, selected cases/adapters, pack result,
   separate policy-supplement result, failures, cleanup state, and archive path.

Done means PI-A1 through PI-A7 are all supported by this run's retained evidence.
