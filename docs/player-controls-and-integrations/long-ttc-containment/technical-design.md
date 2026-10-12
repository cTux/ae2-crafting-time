# Long TTC containment design

[Scope and status](spec.md)

At investigation base `b27d4b96463f3d18438d887e37aeee5de9bc2641`, shared
`CraftingStatusTableRendererMixin` formats active plus pending through
`TimeEstimate`, `TtcText` and `RowTextColor`. The native renderer right-aligns
the component. Both `shared/src/mc1201` and `shared/src/mc2612`
`AbstractTableRendererMixin` adapters ask `CraftingRowState.isWidthLimited`
before applying `badgeTextScale`. The regular and delayed TTC keys are absent
from that predicate, so their full width can extend left of the cell.

Add those two keys to the shared predicate. Keep the existing 90-font-pixel
limit, right edge, horizontal-only transform and badge padding. No adapter
injection, formatter, protocol or configuration change is needed. The mc1201
adapter serves Forge/Fabric 1.20.1 and NeoForge 1.21.1; mc2612 serves NeoForge
26.1.2. Crafting Plan uses the same render path. Header, tooltip and chat text
do not pass through this row fit.

`StandardCraftFixture.quantityStatus(8)` already supplies the reported row in
both fixture implementations. The shared `StandardAe2Scenario` scale stage
captures default/uniform fonts at 1, 2 and Auto, but currently requires only
the amount observation. `LayoutValidator.validateBadges` checks GUI bounds and
item/widget intersections, not the originating row cell. Require the TTC
observation as well: existing `rowText` selects only observations inside that
row and handles the native formatted-text fallback. Compare its complete text
with the expected estimate, including uncertainty; do not accept a truncated
or missing observation. Keep amount checks. Both native observation adapters
record transformed drawing geometry, so no synthetic geometry replacement is
needed.

Unit coverage belongs in `CraftingRowStateTest`; existing width boundary cases
cover 90 and 91 pixels. Add explicit ordinary/delayed keys and retain negative
keys. Existing component/time tests protect text semantics. The native fixture
is the regression for actual cell containment; a predicate test alone is not
enough. Preserve background-off behavior and existing delayed-status coverage.

Runtime preflight verified matching version-specific manifests and Java
17/21/25. The 26.1.2 manifest inherits Minecraft 26.1.2. Its absent child-version
JAR is a placeholder classpath entry; installed NeoForge libraries and parent
client provide the implementation. Its referenced assets root lacks index 30.
Restore official hash-verified assets as described in the plan before smoke;
this is setup for the existing launcher, not new test infrastructure.
