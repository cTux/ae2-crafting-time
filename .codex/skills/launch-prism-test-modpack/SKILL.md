---
name: launch-prism-test-modpack
description: Launch, verify, and close the installed ATM10 test modpack through Prism Launcher's CLI for AE2 Crafting Time tests. Use when a test requires checking that the Minecraft client loads; do not use UI automation merely to press Prism's Launch button.
---

# Launch Prism Test Modpack

Launch the existing Prism instance from PowerShell:

```powershell
$prism = 'C:\Users\cccTu\AppData\Local\Programs\PrismLauncher\prismlauncher.exe'
& $prism --launch 'All the Mods 10 - ATM10'
```

- Use the instance folder ID `All the Mods 10 - ATM10`, not a display-name guess. Its configured root is `E:\games\mc-instances`.
- Use shell/process inspection for launching and startup verification. Do not invoke `computer-use` or click through Prism's UI unless the user specifically asks for UI interaction.
- When the request authorizes installing a test build, remove existing enabled `ae2-crafting-time-*.jar` files from the instance's `minecraft\mods`, copy in the new loader-compatible JAR, and verify exactly one enabled AE2 Crafting Time JAR remains. Replace previous versions directly; do not create backups.
- Before launching, check whether this instance is already running and capture the new Minecraft process identity; do not start a duplicate client.
- A successful Prism process start is not proof that Minecraft loaded. Verify the newest populated `minecraft\logs\latest.log` under the instance and, when relevant, wait for the title-screen/startup completion marker or the requested test state.
- For a load-only smoke test, close the exact launched Minecraft process after the success marker and confirm it exited. First call that process's `CloseMainWindow()` and wait up to 30 seconds; if it remains alive, force-stop only that captured PID. Never leave the client running or kill processes broadly by names such as `java` or `javaw`.
- If the requested test goes beyond loading, close the client only after reaching the requested state and collecting the required evidence.
- Do not modify the instance, install a JAR, select an account, join a server/world, or perform gameplay unless the current request authorizes it.
- Launching a desktop process may require elevated execution in a managed environment; request that permission for the CLI command when required.
