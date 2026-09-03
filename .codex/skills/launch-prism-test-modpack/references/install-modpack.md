# Install A Modpack

Read this when the exact managed instance is absent from Prism's **Codex** group.

## Prepare

- Use `use-codex-vm` for VM lifecycle and VNC control.
- Resolve the project and release before opening Prism. Keep the provider,
  project/version IDs, title, release number, Minecraft version, and loader.
- Prism uses `E:\games\mc-instances` on the host through
  `\\vmware-host\Shared Folders\mc-instances`. Install into the **Codex** group.
  Create **Codex** as part of installation if the group is missing.
- Install sequentially. Do not launch a client while Prism installs another pack.
- A matching instance outside **Codex** does not count. Download a separate
  instance into **Codex**; do not copy or move the other instance into the group.

## Select The Release

- Open **Add Instance**, choose the provider, search the exact title, select the
  exact project, and verify the closed release label before **OK**.
- Modrinth resets provider and compatibility filters for each dialog. Reapply
  Minecraft and loader filters first; use the unfiltered history only when the
  exact release is not available through a filtered result. Treat every history
  row independently because versions and release channels can interleave.
- CurseForge search may rank a related series first. Match the visible title and
  Minecraft-version description, not row position. Keep author-selected optional
  mod defaults unless the request says otherwise.
- Result rows can move while artwork loads. Before **OK**, recheck both the
  populated name and full selected-version label.

## Blocked Downloads

- For CurseForge's **Blocked mods found**, choose **Open Missing**, never
  **Skip**. Skipping creates an incomplete instance.
- Brave opens the required download pages. Starting with the newest download
  tab, wait for its automatic download and close only that tab; repeat until the
  required tabs are handled. Do not click unrelated tabs or download buttons.
- Return to Prism and accept only when every row is green and the footer says
  **All mods found**. Retry only a file that did not arrive.

## Prove Installation

- `N out of N complete` proves downloads, not installation. Wait for the dialog
  to close and the instance to appear.
- Use Prism's **Folder** action and verify `instance.cfg`, `mmc-pack.json`,
  `flame`, and `minecraft`. Do not guess a folder from the display name.
- Verify membership in **Codex** before staging, replacing JARs, or launching.
- Do not use Prism's `--import` CLI option; it leaves an interactive dialog.
