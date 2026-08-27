---
name: launch-prism-test-modpack
description: Take a modpack name, install its compatible release through Prism, add the matching AE2 Crafting Time build, then launch, verify, diagnose, and close it. Use for modpack installation campaigns or Minecraft client load checks.
---

# Launch Prism Test Modpack

## End-to-End Contract

Given a modpack name, complete this whole workflow unless the user narrows it:

1. Resolve the exact CurseForge or Modrinth project and a release supported by a row in `scripts/release-matrix.json`. Record the canonical pack title, provider, pack release, Minecraft version, and loader. If the name matches multiple projects, use the requested provider/version when supplied; otherwise use the exact canonical-title match and report ambiguity instead of silently choosing a similarly named pack.
2. Reuse an exact existing instance or install the release through Prism into the **Codex** group. Installation is complete only when the instance appears in `E:\games\mc-instances`.
3. Read `instance.cfg` and `mmc-pack.json` from that installed instance. Confirm its managed pack identity, Minecraft version, and loader match the resolved release.
4. Select the `scripts/release-matrix.json` row matching that Minecraft version and loader. Unless the user supplied an exact JAR, build that row from the current worktree once per campaign and copy its `dist\ae2-crafting-time-<modVersion>-<loader>-<minecraftVersion>.jar` into `minecraft\mods`. Stop as unsupported if no matrix row matches; never substitute another loader or Minecraft version.
5. Launch the instance through Prism's CLI, inspect the new logs/crash report, decide whether it reached the title screen with AE2 Crafting Time loaded, diagnose any failure, and close the exact captured Minecraft process even when it crashed but remained open.
6. Report the requested name, resolved title/release, instance ID, Minecraft version, loader, copied JAR, result, and failure reason when applicable.

## Install Modpacks Quickly

- Inventory `E:\games\mc-instances` first. Reuse an existing managed instance when `instance.cfg` confirms the same pack and exact version through `ManagedPackID`, `ManagedPackVersionID`, and `ManagedPackVersionName`; also confirm Minecraft and loader in `mmc-pack.json`.
- Resolve the requested project and exact release before opening Prism. For Modrinth, keep the project ID, version ID, title, version number, Minecraft version, and loader so the UI pass is only search, select, verify, install.
- For a batch, install a small pack first to prove Prism and the provider are working, then process the remaining packs sequentially. Do not launch clients while Prism is installing another pack.
- Install missing modpacks through Prism Launcher's UI with Computer Use. Open **Add Instance**, verify **Group** is **Codex**, select the provider, search the exact title, select the exact project, verify the displayed release/version, then install. Prism normally preserves the last-used group, so do not open the group selector when it already says **Codex**. Every newly installed modpack belongs in that group.
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
- Prism's **New Instance** dialog can appear as a child capture alongside the main-window capture. Use the screenshot whose dimensions and contents match the dialog; coordinates are screenshot-local. Do not reuse coordinates or screenshot IDs after any UI change.
- With Windows DPI scaling, accessibility element clicks can report out-of-bounds geometry. Prefer a fresh screenshot-backed click for the provider, search field, result, and **OK**. Before typing, visually confirm the search field's caret or active underline; its accessibility `focused_element` can remain stale on the group field.
- Treat `N out of N complete` as download completion, not installation completion. Success is when the progress dialog closes and the new instance appears in Prism.
- If Prism reports `Modrinth::GetProjects` or another final metadata failure after its internal retries, retry once. If it repeats or remains at 100% without progress, abort that install, record it as blocked, and continue the batch; do not loop or redownload indefinitely.
- After installation, resolve the instance folder ID from `E:\games\mc-instances` or the installed instance's `instance.cfg`; do not guess it from the display name.

For empirical UI timing and observed failure paths, read [references/installation-iterations.md](references/installation-iterations.md) when optimizing this workflow.

## Launch And Check A Modpack

Launch every installed instance through Prism's CLI, never by clicking the UI Launch button:

```powershell
$prism = 'C:\Users\cccTu\AppData\Local\Programs\PrismLauncher\prismlauncher.exe'
$instanceId = 'All the Mods 10 - ATM10'
& $prism --launch $instanceId
```

- Use `All the Mods 10 - ATM10` for the installed ATM10 test instance. Its configured root is `E:\games\mc-instances`.
- Use shell/process inspection for launching and startup verification. Use Computer Use only for modpack installation, not for launching or checking startup.
- When the request authorizes installing a test build, remove existing enabled `ae2-crafting-time-*.jar` files from the instance's `minecraft\mods`, copy in the new loader-compatible JAR, and verify exactly one enabled AE2 Crafting Time JAR remains. Replace previous versions directly; do not create backups.
- Derive the artifact name from the selected release-matrix row and the current `modVersion`; do not pick a JAR merely because its filename contains the game version. Build each required matrix row once and reuse that verified artifact across compatible instances in the same campaign.
- Before launching, check whether this instance is already running and capture the new Minecraft process identity; do not start a duplicate client.
- A successful Prism process start is not proof that Minecraft loaded. Verify the newest populated `minecraft\logs\latest.log` belongs to this launch, confirms AE2 Crafting Time was discovered/loaded, and reaches the title-screen/startup-completion marker. Also inspect the newest crash report when the process exits or stalls. Ordinary warnings are not failure by themselves; a fatal exception, crash report, unresolved dependency/module error, or exit before startup completion is failure.
- For a load-only smoke test, close the exact launched Minecraft process after the success marker and confirm it exited. First call that process's `CloseMainWindow()` and wait up to 30 seconds; if it remains alive, force-stop only that captured PID. Never leave the client running or kill processes broadly by names such as `java` or `javaw`.
- Apply the same close sequence after a failed or crashed launch if the captured process is still alive. Confirm no process tied to that exact instance remains before continuing.
- If the requested test goes beyond loading, close the client only after reaching the requested state and collecting the required evidence.
- Do not modify the instance, install a JAR, select an account, join a server/world, or perform gameplay unless the current request authorizes it.
- Launching a desktop process may require elevated execution in a managed environment; request that permission for the CLI command when required.
