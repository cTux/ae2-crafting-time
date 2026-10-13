# Scale-1 rendering design

[Scope and status](spec.md)

## Evidence and cause boundary

Investigation base: `2af87e253b781f24046f4ccfc66c8d375fdf2fd8`.
The original report used PR #694, commit `e47ede5914bad6e2772b7f11f3f719d4e9eebfe2`.
Archive `20261012T165658058Z-4096bc46` retains the Forge/Fabric scale-1 PNGs and
sidecars. The Forge `status-scale-default-1.png` SHA-256 is
`90fc62533c329f8de0b3e77278a651e0915c5ab7caf729aacaf194f7a204f3fb`.
Its sidecar records 2558x1093, scale 1, SVGA3D, five-pixel table text and
six-pixel CPU TTC. Keep those captures as failure evidence.

`CpuTtcLayout.badge` starts at 0.6 scale and may shrink further to fit.
Both `CPUSelectionListMixin` adapters apply that scale to both axes.
AE2 `AbstractTableRenderer` uses 67x22 cells, three columns, 0.5 text scale,
and one-pixel line spacing. `CraftingRowState.badgeTextScale` can additionally
compress TTC and amounts horizontally to 90 font pixels (45 GUI pixels).
Fractional downsampling loses source bitmap pixels. This establishes a rendering
mechanism, not a proven SVGA-only defect or before-change regression.

Shared `CraftPlanScenario.screenshot` reads the framebuffer synchronously.
`CpuListTtcScenario.SCALE_SMALL` captures before restoring Auto; Fabric's runtime
callback is at `GameRenderer.render` TAIL. That rules out the obvious queued
capture/scale-reset race. Compare settled-frame PNGs with the VM display before
changing production layout to resolve the remaining capture question.

## Shared geometry and thin adapters

Add one pure shared layout policy for native tables. Inputs are effective GUI
scale, original column count/cell dimensions/visible rows, and whether the
affected TTC presentation is active. The scale-1 policy returns one column,
width `columns * (cellWidth + 1) - 1`, cell height 32, text scale 1, and
`max(1, (originalRows * (cellHeight + 1)) / 33)` visible rows. The native mode
returns original geometry. Centralize row index, hit-test and scroll calculations
in this policy rather than reproducing constants in screen mixins.

Preserve AE2's renderer and its item/tooltip flow. Adapt its geometry constants,
text scale/inverse scale, line height, loop limits and instance scroll count at
the existing render boundary. Recompute scale-sensitive values per render;
constructor-only values become stale after resizing. Optional presentation
injections use `require = 0`; verify production-remapped targets and startup.
Do not overwrite the entire renderer.

Make activation atomic across geometry consumers. Reuse the registered
`IntegrationMixinPlugin.postApply` seam to inspect the transformed method
instructions for the expected geometry-hook call sites, with exact per-adapter
method/site counts established from the pinned targets. Record renderer and
screen-consumer readiness in a small shared registry, initially false. Checking
only that a generated handler method exists is insufficient: a failed optional
injection can leave that method without a call site. Every new geometry hook
returns its original native value unless both the renderer and the current
screen's interaction hooks are verified. Query readiness at runtime rather
than caching it in constructors; screen class transformation completes before
that screen renders. The Plan screen needs its own interaction hooks; the
Status screen needs its own, not an unrelated screen to have loaded first.

An absent/mismatched site keeps the entire table policy inactive, including
scroll and click consumers. Preserve original constants and behavior on that
path. CPU presentation has its own fit/tooltip readiness pair and likewise
retains its previous behavior if either half is missing. Keep this check narrow
to the changed presentation hooks; do not alter integration selection for
unrelated adapters. Cover complete, partial, absent and load-order readiness
with pure tests and a small transformed-bytecode boundary fixture.

Consumers to update together:

- `shared/src/mc1201` and `shared/src/mc2612` `AbstractTableRendererMixin`.
- Both versions of `CraftConfirmScreenMixin` and `CraftingCPUScreenMixin`, which
  independently use 67/22/3 constants for TTC click lookup.
- Native renderer scroll-range calculation and hovered-stack bounds; their
  geometry must match the screen click consumers, including after scale changes.
- Both native test-driver row observers/fixtures. Update observed native
  geometry, never invent replacement geometry in screenshot sidecars.

The mc1201 adapter serves Forge/Fabric 1.20.1 and NeoForge 1.21.1. mc2612 serves
NeoForge 26.1.2. Read the actual pinned AE2 implementation in each target before
choosing exact injection descriptors; the older cached AE2 source established
the layout mechanism, not the new production mixin descriptors.

## Text and CPU cards

Scale-1 table drawing bypasses the 90-font-pixel compression. Calculate available
text width from the actual cell minus icon/right margin and badge padding.
Keep a pixel-aligned origin and scale 1. Reuse the existing component styling,
badge/background and shadow policy. A font-aware ellipsis fit must preserve the
component's full text for native hover; do not convert styled components to
unstyled strings. Shared decisions/width budgets belong in pure covered code;
font splitting remains an adapter operation.

Extend `CpuTtcLayout` with the effective-scale policy: scale 1 at GUI scale 1,
existing behavior otherwise. Fit the visible component to the card's available
width before measuring layout. Add complete TTC to `CPUSelectionList.getTooltip`
when shortened: current native tooltip has CPU name/storage/job details, not
the TTC estimate. Read the same CPU estimate used by the visible card; preserve
expiry/unknown/disabled behavior. Do not introduce another duration grammar.

## Verification prerequisites

Read-only preflight on 2026-10-13 established an idle CodexVM desktop, key-based
SSH, a running VMware Tools service, prepared `launch.json` files for all four
targets, and installed Java 17.0.20.1, 21.0.12.1 and 25.0.4.1. Existing target
game directories and referenced asset indexes exist. Version-specific prepared
manifests include Forge 47.4.23/47.4.26, Fabric 0.19.5, NeoForge 21.1.251 and
26.1.2.109. The dispatcher must select the manifest matching the resolved
compatible lock; base manifests can contain older loaders. Preflight is not
a successful client launch or a full library-integrity check.

The existing disposable fixture/host-build/guest-local-launch lifecycle applies.
No new runner or native-host installation is part of this plan. Native hardware
remains conditional on explicit authority and a verified disposable launch path.
The existing `SmokePixels.Compare` exact-region engine is sufficient for the
glyph contract; qualify a fixed text region with no foreground mask and prove a
missing-stroke rejection. Do not add OCR or a new screenshot framework.
