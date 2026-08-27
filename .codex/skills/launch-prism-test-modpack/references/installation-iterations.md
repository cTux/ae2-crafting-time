# Installation Iterations

UI time measures the active Prism selection flow from **Add Instance** to **OK** and excludes downloads. Total time includes download and Prism finalization.

| # | Provider | Pack | Minecraft | Loader | UI time | Total time | Result | Improvement |
|---:|---|---|---|---|---:|---:|---|---|
| 1 | CurseForge | Better MC [FORGE] BMC4 v61 | 1.20.1 | Forge | n/a | 105.3s | Partial: blocked mods skipped | Never count **Skip** as a complete install; distinguish green **Found** from red **Not found**. |
| 2 | Modrinth | Fabulously Optimized 6.2.3 | 1.21.1 | Fabric | 353.5s | 387.7s | Installed | Jump through the unfiltered version history with its scrollbar and verify the exact live Prism label. |
| 3 | Modrinth | Simply Optimized & Up to Date 1.0.0 | 1.21.1 | Fabric | 127.5s | 148.3s | Installed | Apply loader and Minecraft filters before search so Prism preselects a compatible release. |
| 4 | Modrinth | Cobblemon Official Modpack 1.7.3 | 1.21.1 | Fabric | 51.6s | 94.0s | Installed | Select an exact project already visible in the initial catalog and verify the default release's first row. |
| 5 | Modrinth | COBBLEVERSE 1.7.42 | 1.21.1 | Fabric | 37.4s | 70.2s | Installed | Verify the populated name and version after selection because result rows can shift as artwork loads. |
| 6 | Modrinth | Better MC [FABRIC] BMC2 v40 | 1.20.1 | Fabric | 50.7s | 94.2s | Installed | Skip opening the version picker when the closed label already provides exact release and Minecraft version; render latency caused this measured regression. |
| 7 | CurseForge | Valhelsia 6 6.2.3 | 1.20.1 | Forge | 78.6s | 112.9s | Installed | Reject an incompatible series from its result description and choose a compatible neighboring series without another search. |
| 8 | CurseForge | DeceasedCraft 5.10.17 | 1.20.1 | Forge | 55.1s | 364.5s | Installed after 2 manual files | Activate each **Open Missing** browser tab once, then wait for Prism's green **All mods found** state before **OK**. |
| 9 | CurseForge | Enigmatica 10 1.31.0 | 1.21.1 | NeoForge | 65.1s | 124.6s | Installed | Match the exact visible project title because search may rank an older related series first. |
| 10 | Modrinth | Vanilla Perfected 1.20.1 Full Release | 1.20.1 | Fabric | 140.5s | 198.0s | Installed | Treat every version row independently because the history interleaves Minecraft versions and release channels. |
| 11 | Modrinth | BigChadGuys Plus 2.11.0 | 1.20.1 | Fabric | 95.8s | 145.6s | Installed | For a unique exact search result, skip sorting and filters when the closed release label already matches. |
| 12 | CurseForge | Monifactory Beta 0.13.7 | 1.20.1 | NeoForge | 178.5s | 277.4s | Installed; optional mods defaulted off | Switch providers without retyping because Prism preserves the query; accept optional-mod defaults when none were requested. |
| 13 | CurseForge | Project Architect 2 6.0 | 1.20.1 | Forge | 152.1s | 1855.4s interrupted | Installed after 23 manual files | Start at the newest CurseForge tab, then wait and close each download tab so the preceding required page gains focus automatically; keep unrelated tabs open. |
| 14 | CurseForge | All the Mods 9 - No Frills 0.2 | 1.20.1 | Forge | 92.3s | 240.9s | Installed after 1 manual file | Prism preserves the last-used group; verify the prefilled **Codex** value instead of opening its selector. |
| 15 | CurseForge | Better MC [NEOFORGE] BMC5 v52 | 1.21.1 | NeoForge | 114.1s | 315.0s | Installed after 4 manual files | After one punctuation correction confirmed ATM9 Sky was absent, replace the query in the same dialog instead of restarting the install flow. |
