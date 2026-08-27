---
name: launch-prism-test-modpack
description: Launch the installed ATM10 test modpack through Prism Launcher's CLI for AE2 Crafting Time gameplay tests. Use when a test requires starting the Minecraft client; do not use UI automation merely to press Prism's Launch button.
---

# Launch Prism Test Modpack

Launch the existing Prism instance from PowerShell:

```powershell
$prism = 'C:\Users\cccTu\AppData\Local\Programs\PrismLauncher\prismlauncher.exe'
& $prism --launch 'All the Mods 10 - ATM10'
```

- Use the instance folder ID `All the Mods 10 - ATM10`, not a display-name guess. Its configured root is `E:\games\mc-instances`.
- Use shell/process inspection for launching and startup verification. Do not invoke `computer-use` or click through Prism's UI unless the user specifically asks for UI interaction.
- Before launching, check whether this instance is already running; do not start a duplicate client.
- A successful Prism process start is not proof that Minecraft loaded. Verify the newest populated `minecraft\logs\latest.log` under the instance and, when relevant, wait for the title-screen/startup completion marker or the requested test state.
- Do not modify the instance, install a JAR, select an account, join a server/world, or perform gameplay unless the current request authorizes it.
- Launching a desktop process may require elevated execution in a managed environment; request that permission for the CLI command when required.
