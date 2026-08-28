---
name: launch-prism-test-modpack
description: Check AE2 Crafting Time against Minecraft versions or explicitly named modpacks. Use Prism in the Codex VM only for a specific requested modpack; when no modpack is named, run the repository's applicable run-*.bat launchers instead.
---

# Launch Prism Test Modpack

## Choose The Check Path

- When the user explicitly names one or more modpacks to check, use the Prism workflow below in the Codex VM. Load the global `use-codex-vm` skill for VM lifecycle and direct-control instructions.
- When the user does not name a specific modpack, do not use Prism or the VM. Run the applicable repository-root `run-*.bat` files sequentially to check the requested Minecraft/loader versions; for a broad all-version check, run every `run-*.bat` file. Verify each client reaches startup without an AE2 Crafting Time failure, close that exact client, then continue.
- Do not reinterpret a generic request such as checking supported versions, dependencies, builds, or compatibility as a request to test the installed Prism modpack inventory.

## End-to-End Contract

Given a specific modpack name, complete this whole workflow unless the user narrows it:

1. Resolve the exact CurseForge or Modrinth project and a release supported by a row in `scripts/release-matrix.json`. Record the canonical pack title, provider, pack release, Minecraft version, and loader. If the name matches multiple projects, use the requested provider/version when supplied; otherwise use the exact canonical-title match and report ambiguity instead of silently choosing a similarly named pack.
2. Reuse an exact existing instance or install the release through Prism into the **Codex** group. In the dedicated test VM, installation is complete only when Prism shows the instance and its **Folder** action opens the guest instance directory.
3. Read `instance.cfg` and `mmc-pack.json` from that installed instance. Confirm its managed pack identity, Minecraft version, and loader match the resolved release, then inspect enabled mod JAR metadata, including nested JAR-in-JAR archives, for mod ID `ae2`. AE2 is mandatory: when it is absent, do not copy or launch AE2 Crafting Time; remove that instance from the Codex test inventory and report it as ineligible rather than failed. Never infer AE2 presence from filenames alone.
4. Select the `scripts/release-matrix.json` row matching that Minecraft version and loader. Unless the user supplied an exact JAR, build that row from the current worktree once per campaign and copy its `dist\ae2-crafting-time-<modVersion>-<loader>-<minecraftVersion>.jar` into `minecraft\mods`. Stop as unsupported if no matrix row matches; never substitute another loader or Minecraft version.
5. Launch the instance through Prism inside the test VM, inspect Prism's console and the game window, decide whether it reached the title screen with AE2 Crafting Time loaded, diagnose any failure, and close the exact guest game window even when it crashed but remained open.
6. Report the requested name, resolved title/release, instance ID, Minecraft version, loader, copied JAR, result, and failure reason when applicable.

## Install Modpacks Quickly

- Use the global `use-codex-vm` skill to start or reuse the dedicated VM and operate it through VNC. This skill owns only the Prism and modpack workflow.
- Inventory `E:\games\mc-instances` on the host first; Prism sees it in the guest at `\\vmware-host\Shared Folders\mc-instances`. Reuse an existing managed instance when `instance.cfg` confirms the same pack and exact version through `ManagedPackID`, `ManagedPackVersionID`, and `ManagedPackVersionName`; also confirm Minecraft and loader in `mmc-pack.json`.
- Keep that shared folder as Prism's normal instance root, but do not launch Minecraft from the VMware shared filesystem: Java watch-service registration fails there with `java.io.IOException: Incorrect function`. For a test campaign, copy only the eligible instances to a temporary guest-local NTFS instance root, point Prism there, launch sequentially, sync logs and crash reports back to the shared folder, then restore Prism's shared instance root and remove the marked temporary copy.
- Resolve the requested project and exact release before opening Prism. For Modrinth, keep the project ID, version ID, title, version number, Minecraft version, and loader so the UI pass is only search, select, verify, install.
- For a batch, install a small pack first to prove Prism and the provider are working, then process the remaining packs sequentially. Do not launch clients while Prism is installing another pack.
- Install missing modpacks through Prism Launcher's UI in the guest. Open **Add Instance**, verify **Group** is **Codex**, select the provider, search the exact title, select the exact project, verify the displayed release/version, then install. Prism normally preserves the last-used group, so do not open the group selector when it already says **Codex**. If a fresh Prism profile has no groups, finish the install, select the instance, choose **Change Group**, enter `Codex`, and verify the instance moves under that heading.
- On CurseForge, choose **Sort by Total Downloads** before selecting candidates. Whenever **Blocked mods found** appears, always click **Open Missing**. Never click **Skip**: it creates an incomplete instance that must be recorded as partial, not successful.
- Brave opens every required CurseForge mod or resource-pack download page. Start at the newest download tab, wait for its automatic download, then close it with `Ctrl+W`; closing it focuses the preceding download tab and starts that download, so repeat until every opened download tab is closed. Do not click a download button or close unrelated browser tabs. Return to Prism, wait until every row is green and the footer says **All mods found**, then click **OK**. Use **try again** only for a tab whose file still did not arrive; avoid duplicate downloads.
- Modrinth's version picker is a single unfiltered history. Resolve the target release first, then drag its popup scrollbar proportionally to the target release era and select the row whose label confirms the exact Minecraft version and release channel. Treat Prism's live label as authoritative when a search snippet disagrees.
- Do not infer a Modrinth release's Minecraft version from neighboring rows: histories can interleave several game versions and release channels. Read the target row itself and recheck the closed label after selection.
- Prefer **Filter options** before a Modrinth search: select the loader and Minecraft version first. Compatible search results then default to a matching release and usually avoid the version-history picker entirely.
- In Modrinth's Minecraft-version filter, press `Page Down` until the target version is visible instead of dragging the tiny scrollbar. If the exact searched project disappears after filtering, it has no compatible release; clear the query and select the highest-ranked compatible candidate rather than opening that project's version history.
- For repeated installs with the same Modrinth compatibility target, apply loader and Minecraft filters, then select the next visible compatible catalog result directly. Search only when the intended project is absent from that filtered first page.
- Prism preserves **Group**, but each new **Add Instance** dialog resets the provider and Modrinth compatibility filters. Reapply provider, loader, and Minecraft version on every iteration; do not waste time checking whether those filters persisted.
- Selecting a Minecraft version leaves Modrinth's checklist popup open. Dismiss it by clicking the empty project-detail pane before selecting a catalog row; dragging or clicking through the popup can select the wrong pack.
- Before typing a Modrinth query, scan the initial visible catalog for the exact project. For a visible popular pack whose default release already matches the target Minecraft version and loader, select it directly and only open the version picker long enough to verify its first row.
- When an exact Modrinth query yields one unambiguous project, skip sorting and filters: select the sole result and accept it only if the closed version label shows the target release and Minecraft version.
- Modrinth result rows can shift while artwork loads. After selecting a row, verify both the populated **Name** field and the full **Version selected** label before **OK**; continue with the actual selected compatible pack or correct the selection from fresh state.
- Do not open the version picker when the closed **Version selected** label already shows the exact pack release and Minecraft version. Opening it is only a fallback for truncated or ambiguous labels.
- On CurseForge, use each result's one-line Minecraft-version description to reject incompatible pack series before selecting them. When a neighboring series matches, select it from the same results instead of issuing a second search.
- CurseForge search relevance can rank an older similarly named series above the exact query. Match the visible project title, not row position, before selecting.
- If an exact search yields no results, switch provider and wait for the preserved query to render before retyping; it may immediately expose a compatible predecessor series. Refocus the search field before using `Ctrl+A`; clicking an empty result pane moves focus away from it.
- If a candidate remains absent after one exact-title or punctuation correction, do not restart **Add Instance** or count it as an install iteration. Replace the query with the next compatible candidate in the same provider dialog and keep the existing timer.
- If **Select Optional Mods** appears and the request did not specify optional features, keep the pack author's default checked state and click **OK**; do not enable everything.
- Do not use Prism's `--import` CLI option to install a modpack; it opens an interactive import dialog and does not complete the installation unattended.
- Treat `N out of N complete` as download completion, not installation completion. Success is when the progress dialog closes and the new instance appears in Prism.
- If Prism reports `Modrinth::GetProjects` or another final metadata failure after its internal retries, retry once. If it repeats or remains at 100% without progress, abort that install, record it as blocked, and continue the batch; do not loop or redownload indefinitely.
- After installation, use Prism's **Folder** action and verify that the opened guest directory contains `instance.cfg`, `mmc-pack.json`, `flame`, and `minecraft`; do not guess the folder ID from the display name.

For empirical UI timing and observed failure paths, read [references/installation-iterations.md](references/installation-iterations.md) when optimizing this workflow.

## Launch And Check A Modpack

Launch every installed instance from its selected Prism entry inside the dedicated test VM.

- Use the selected instance under `E:\games\mc-instances` on the host and its matching staged guest-local NTFS copy for the launch; do not assume a previously tested pack is still installed.
- Before building or copying a test JAR, inspect enabled mod JAR metadata, including nested JAR-in-JAR archives, for mod ID `ae2`. If AE2 is absent, remove the instance and skip the launch; a dependency error caused by testing an AE2-free pack is invalid test evidence.
- Keep Prism's console visible while the client starts. A successful Prism action is not proof that Minecraft loaded: verify AE2 Crafting Time in the console/log output and the title screen in the guest framebuffer.
- When the request authorizes installing a test build, remove existing enabled `ae2-crafting-time-*.jar` files from the instance's `minecraft\mods`, copy in the new loader-compatible JAR, and verify exactly one enabled AE2 Crafting Time JAR remains. Replace previous versions directly; do not create backups.
- Derive the artifact name from the selected release-matrix row and the current `modVersion`; do not pick a JAR merely because its filename contains the game version. Build each required matrix row once and reuse that verified artifact across compatible instances in the same campaign.
- Before launching, check Prism's selected-instance controls for an existing running state; do not start a duplicate client.
- A successful Prism process start is not proof that Minecraft loaded. Verify the newest populated `minecraft\logs\latest.log` belongs to this launch, confirms AE2 Crafting Time was discovered/loaded, and reaches the title-screen/startup-completion marker. Also inspect the newest crash report when the process exits or stalls. Ordinary warnings are not failure by themselves; a fatal exception, crash report, unresolved dependency/module error, or exit before startup completion is failure.
- For a load-only smoke test, close the exact Minecraft window after the success marker and verify Prism returns the instance to a non-running state. Use the window close or in-game quit path first; use Prism's selected-instance **Kill** action only if that exact client remains stuck. Never kill Java processes broadly.
- Apply the same close sequence after a failed or crashed launch if the exact guest client remains open. Confirm Prism no longer marks that instance as running before continuing.
- If the requested test goes beyond loading, close the client only after reaching the requested state and collecting the required evidence.
- Do not modify the instance, install a JAR, select an account, join a server/world, or perform gameplay unless the current request authorizes it.
- Launching a desktop process may require elevated execution in a managed environment; request that permission for the CLI command when required.
