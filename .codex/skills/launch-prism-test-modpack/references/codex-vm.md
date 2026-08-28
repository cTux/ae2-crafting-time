# Codex Test VM

Use this environment for Prism installation, tuning, and Minecraft client tests without taking over the host desktop.

## Host And Guest

- VMX: `F:\VMs\Codex-Windows11\Codex-Windows11.vmx`
- VMware CLI: `C:\Program Files\VMware\VMware Workstation\vmrun.exe`
- Guest account: `Codex`
- Host Prism instances: `E:\games\mc-instances`
- Guest Prism instances: `\\vmware-host\Shared Folders\mc-instances`
- VNC helper: `.codex\skills\launch-prism-test-modpack\scripts\codex-vm-vnc.py`

The VM exposes only the host instance root above as the writable `mc-instances` VMware share. Clipboard sharing and drag-and-drop remain disabled; do not add broader shares for a test campaign.

When a campaign needs a locally built mod JAR, copy it directly into the matching host instance's `minecraft\mods` directory. Prism and Minecraft see that change through the existing share.

## Start Direct Control

Do not use the Computer Use plugin and do not automate the visible VMware host window. Control the guest through VMware's built-in VNC server.

Before changing VNC settings, confirm the VM is powered off with `vmrun -T ws list`. While it is off, configure only:

```text
RemoteDisplay.vnc.enabled = "TRUE"
RemoteDisplay.vnc.ip = "127.0.0.1"
RemoteDisplay.vnc.port = "5905"
```

Binding to `127.0.0.1` is required because this endpoint has no password. Start the VM headlessly:

```powershell
& 'C:\Program Files\VMware\VMware Workstation\vmrun.exe' -T ws start 'F:\VMs\Codex-Windows11\Codex-Windows11.vmx' nogui
```

Wait for Windows auto-login, then capture the framebuffer:

```powershell
python .codex\skills\launch-prism-test-modpack\scripts\codex-vm-vnc.py capture "$env:TEMP\codex-vm.png"
```

Inspect that image before every coordinate input. Examples:

```powershell
python .codex\skills\launch-prism-test-modpack\scripts\codex-vm-vnc.py click 200 1170
python .codex\skills\launch-prism-test-modpack\scripts\codex-vm-vnc.py text 'All the Mods 10 - ATM10'
python .codex\skills\launch-prism-test-modpack\scripts\codex-vm-vnc.py key enter
```

`text` does not press Enter. Capture again after every click, key, or text action before deciding the next input.

## Prism Verification

Prism 11 uses the shared guest path above as its **Instances** folder. Inventory and inspect it from the host path, and use Prism's selected-instance **Folder** action for guest-side readback. Installation is complete only when:

- the download/progress dialog closes;
- the instance appears under the `Codex` group;
- **Folder** opens the exact instance directory; and
- `instance.cfg`, `mmc-pack.json`, `flame`, and `minecraft` are present.

Observed baseline on 2026-08-28: `All the Mods 10 - ATM10` resolved to CurseForge release `8.0` for Minecraft `1.21.1` and installed without blocked files. Treat that version as historical evidence, not a permanent pin; verify Prism's live selected-version label on every new install.

## End Direct Control

Close guest applications, soft-stop only this VM, and wait until `vmrun -T ws list` reports no running VM before editing the VMX. Restore:

```text
RemoteDisplay.vnc.enabled = "FALSE"
```

Remove the temporary `RemoteDisplay.vnc.ip` and `RemoteDisplay.vnc.port` lines. Do not leave an unauthenticated VNC endpoint enabled after the campaign.
