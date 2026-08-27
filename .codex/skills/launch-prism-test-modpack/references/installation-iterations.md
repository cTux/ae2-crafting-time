# Installation Iterations

UI time measures the active Prism selection flow from **Add Instance** to **OK** and excludes downloads. Total time includes download and Prism finalization.

| # | Provider | Pack | Minecraft | Loader | UI time | Total time | Result | Improvement |
|---:|---|---|---|---|---:|---:|---|---|
| 1 | CurseForge | Better MC [FORGE] BMC4 v61 | 1.20.1 | Forge | n/a | 105.3s | Partial: blocked mods skipped | Never count **Skip** as a complete install; distinguish green **Found** from red **Not found**. |
| 2 | Modrinth | Fabulously Optimized 6.2.3 | 1.21.1 | Fabric | 353.5s | 387.7s | Installed | Jump through the unfiltered version history with its scrollbar and verify the exact live Prism label. |
| 3 | Modrinth | Simply Optimized & Up to Date 1.0.0 | 1.21.1 | Fabric | 127.5s | 148.3s | Installed | Apply loader and Minecraft filters before search so Prism preselects a compatible release. |
| 4 | Modrinth | Cobblemon Official Modpack 1.7.3 | 1.21.1 | Fabric | 51.6s | 94.0s | Installed | Select an exact project already visible in the initial catalog and verify the default release's first row. |
