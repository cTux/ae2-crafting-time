# Long TTC containment implementation

[Scope and status](spec.md)

1. Add the regular and delayed TTC keys to `CraftingRowState.isWidthLimited`.
   Extend its unit cases; preserve scale boundaries and unrelated keys (C1, C5).
2. In the existing `StandardAe2Scenario` amount/font scale stage, require the
   1B/2B/3B row's full expected TTC through `rowText` and compare its complete
   text, including `?`. Keep amount checks and six existing captures. Ensure
   this assertion fails with the original width predicate (C2).
3. Clarify long row fitting in the existing English/Ukrainian GuideME pages and
   wiki source. Do not add settings or change tooltip/chat precision (C5).
4. Complete review, conventional commit and hook-created PR before running
   tests. Run shared unit/coverage and relevant component checks. Inspect
   `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly` and
   retain the selection. Run its required coverage without silently expanding
   to full release suites (C1, C5).

## Existing runtime prerequisite

Preflight on 2026-10-12 verified CodexVM SSH and these compatible native
manifests below `C:/Users/Public/Documents/AE2CraftingTimeSmoke/prepared`:

| Target | Manifest subdirectory | Java |
| --- | --- | --- |
| 1.20.1 Forge | `1.20.1-forge/1.20.1-47.4.23` | 17 |
| 1.20.1 Fabric | `1.20.1-fabric/0.19.5` | 17 |
| 1.21.1 NeoForge | `1.21.1-neoforge/21.1.251` | 21 |
| 26.1.2 NeoForge | `26.1.2-neoforge/26.1.2.109` | 25 |

Each contains `launch.json`. Recheck against the current compatible lock and
resolve guest Java with `scripts/get-java-home.ps1`. The existing dispatcher
stages a guest-local runtime and disposable fixture and installs the uniform
font pack; use its exact-worktree share and fixture ownership checks.

Before the first smoke, restore missing official assets in the dedicated
prepared installation. No repository script or loader change is required.
On the guest, use the existing PowerShell download command as follows:

```powershell
$launcherRoot = 'C:\Users\Public\Documents\AE2CraftingTimeSmoke\dcd1-no-space\launcher'
$metadata = Get-Content "$launcherRoot\versions\26.1.2\26.1.2.json" -Raw | ConvertFrom-Json
$assetIndex = $metadata.assetIndex
$indexPath = "$launcherRoot\assets\indexes\$($assetIndex.id).json"
Invoke-WebRequest -UseBasicParsing -Uri $assetIndex.url -OutFile $indexPath
if ((Get-FileHash $indexPath -Algorithm SHA1).Hash -ine $assetIndex.sha1) { throw 'Asset index hash mismatch' }
$objects = (Get-Content $indexPath -Raw | ConvertFrom-Json).objects
foreach ($asset in $objects.PSObject.Properties.Value) {
    $hash = $asset.hash
    $prefix = $hash.Substring(0, 2)
    $directory = "$launcherRoot\assets\objects\$prefix"
    $destination = "$directory\$hash"
    if (!(Test-Path $destination) -or (Get-FileHash $destination -Algorithm SHA1).Hash -ine $hash) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
        Invoke-WebRequest -UseBasicParsing -Uri "https://resources.download.minecraft.net/$prefix/$hash" -OutFile $destination
    }
    if ((Get-FileHash $destination -Algorithm SHA1).Hash -ine $hash) { throw "Asset hash mismatch: $hash" }
}
```

Confirm metadata still names index `30`, official `piston-meta.mojang.com`,
SHA-1 `1c325980cb885aabe2602f94993eb2d82dd44a82` and 548391 index bytes before
download. It declares 456427629 total asset bytes; existing hash objects can
be reused. Stop on download/hash failures and retain logs. Do not launch a
different Minecraft version or change the compatible dependency graph.

## Native verification and completion

First run `scripts/run-ui-smoke-matrix.ps1 -Target 26.1.2-neoforge
-Scenario standard-status-controls -BaseOnly` as one command. After it passes,
run the same native scenario sequentially for the other three targets, using
the matching prepared manifests. These four launches cover the explicit issue
criterion; additional changed-selector cases cover connected affected paths.
Budget roughly 30 minutes for four focused launches and review, plus asset
download/build time; measure actual phases and adjust after the first launch.
This is a planning budget, not measured performance.

Review each default/wide scale capture and sidecar for the full estimate,
uncertainty, originating cell containment, compact amounts and readable glyphs.
Retain SHA-256 hashes and effective scales; follow `docs/ui-smoke-evidence.md`
for archive and review receipts (C3, C4). Preserve raw failures and distinguish
diagnostic retries from clean final proof. Confirm tested-client exit before
the next launch. Cleanly shut down exact CodexVM afterward and verify it is
absent from `vmrun -T ws list`, including on failure.

Finish only when current-head checks, native evidence and review pass and the
merged implementation is linked from the canonical spec. No current smoke or
before/after result is claimed by these planning documents.
