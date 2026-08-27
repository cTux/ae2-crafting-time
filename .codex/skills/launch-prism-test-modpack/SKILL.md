---
name: launch-prism-test-modpack
description: Quickly install one or many exact Prism modpack releases through Prism's UI with Computer Use, then launch, verify, and close them through Prism's CLI for AE2 Crafting Time tests. Use for modpack installation campaigns or Minecraft client load checks.
---

# Launch Prism Test Modpack

## Install Modpacks Quickly

- Inventory `E:\games\mc-instances` first. Reuse an existing managed instance when `instance.cfg` confirms the same pack and exact version through `ManagedPackID`, `ManagedPackVersionID`, and `ManagedPackVersionName`; also confirm Minecraft and loader in `mmc-pack.json`.
- Resolve the requested project and exact release before opening Prism. For Modrinth, keep the project ID, version ID, title, version number, Minecraft version, and loader so the UI pass is only search, select, verify, install.
- For a batch, install a small pack first to prove Prism and the provider are working, then process the remaining packs sequentially. Do not launch clients while Prism is installing another pack.
- Install missing modpacks through Prism Launcher's UI with Computer Use. Open **Add Instance**, select the provider, search the exact title, select the exact project, verify the displayed release/version, then install.
- On CurseForge, choose **Sort by Total Downloads** before selecting candidates. If **Blocked mods found** appears, green **Found** entries need no action, but red **Not found** entries require **Open Missing** and manual downloads. **Skip** creates an incomplete instance; record it as partial, never successful.
- Modrinth's version picker is a single unfiltered history. Resolve the target release first, then drag its popup scrollbar proportionally to the target release era and select the row whose label confirms the exact Minecraft version and release channel. Treat Prism's live label as authoritative when a search snippet disagrees.
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
- Before launching, check whether this instance is already running and capture the new Minecraft process identity; do not start a duplicate client.
- A successful Prism process start is not proof that Minecraft loaded. Verify the newest populated `minecraft\logs\latest.log` under the instance and, when relevant, wait for the title-screen/startup completion marker or the requested test state.
- For a load-only smoke test, close the exact launched Minecraft process after the success marker and confirm it exited. First call that process's `CloseMainWindow()` and wait up to 30 seconds; if it remains alive, force-stop only that captured PID. Never leave the client running or kill processes broadly by names such as `java` or `javaw`.
- If the requested test goes beyond loading, close the client only after reaching the requested state and collecting the required evidence.
- Do not modify the instance, install a JAR, select an account, join a server/world, or perform gameplay unless the current request authorizes it.
- Launching a desktop process may require elevated execution in a managed environment; request that permission for the CLI command when required.
