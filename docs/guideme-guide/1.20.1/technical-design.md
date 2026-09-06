# Minecraft 1.20.1 Guide Book Technical Design

This design implements the [specification](spec.md) for
[#265](https://github.com/cTux/ae2-crafting-time/issues/265).

## Verified platform seams

- GuideME 20.1.15's `GuideItem` reads the string NBT key `guideId`; its data-driven
  guide definition still supplies the localized display name and model.
- AE2 Fabric 15.4.10 automatically merges Markdown below every mod namespace's
  `ae2guide` resource folder. Its public `AppEng.openGuideAtPreviousPage` seam
  opens a namespaced initial page while retaining the guide's normal history.
- The existing shared source already owns
  `guideme_guides/guide.json` and the English/Ukrainian Markdown under
  `guides/ae2craftingtime/guide/`. Those files remain the one authored copy.

Sources: [GuideME 20.1.15 `GuideItem`](https://github.com/AppliedEnergistics/GuideME/blob/v20.1.15/src/main/java/guideme/internal/item/GuideItem.java),
[AE2 15.4.10 addon guide docs](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fabric/v15.4.10/guidebook.md#for-addon-authors), and
[AE2 15.4.10 `AppEng`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fabric/v15.4.10/src/main/java/appeng/core/AppEng.java).

## Shared content pipeline

Keep the current Markdown in
`shared/src/main/resources/assets/ae2craftingtime/guides/ae2craftingtime/guide/`.
During resource processing:

- Forge copies it to the GuideME guide-content location expected by the existing
  `ae2craftingtime:guide` definition;
- Fabric copies the same files to
  `assets/ae2craftingtime/ae2guide/`, preserving `_uk_ua/` and relative links;
- modern NeoForge output remains unchanged.

The build validates that the rendered outputs have identical Markdown hashes and
matching English/Ukrainian page/navigation sets. Generated copies are JAR output,
not committed duplicate sources.

## Forge implementation

Add GuideME 20.1.15 as `compileOnly` and optional runtime development input.
Add an optional `guideme` dependency in `mods.toml`, ordered after GuideME on
both sides with minimum `20.1.15`.

Use a Forge conditional recipe so the recipe is absent when `guideme` is absent.
The contained shapeless recipe uses exactly:

- `minecraft:book`;
- `ae2:certus_quartz_crystal` (uncharged item ID);
- `minecraft:clock`.

Its result is one `guideme:guide` with SNBT
`{guideId:"ae2craftingtime:guide"}`. Use the existing data-driven guide
definition for title/model/content. No production Java references a GuideME
class; JEI or other optional hooks stay behind an installed-mod check.

Add GuideME to only the Forge 1.20.1 release-matrix relation and teach both
deployment scripts/tests to emit the matching optional dependency. Do not add it
to Fabric.

## Fabric implementation

Register `ae2craftingtime:guide` as a plain `Item` in the Fabric entrypoint.
Its small subclass overrides use on the client, calls
`AppEng.instance().openGuideAtPreviousPage(new ResourceLocation(
"ae2craftingtime", "index.md"))`, returns success, and never mutates the stack.
Server use acknowledges the interaction without loading client guide classes;
place the opener behind the existing Fabric client entrypoint/client-only class
boundary.

Add the translated item name and `minecraft:item/book` generated model. Add one
ordinary shapeless recipe with the same three exact items and one registered
guide result. AE2 is already required, so no recipe condition or new dependency
is needed.

## Validation and failure behavior

- Static build checks parse both recipe forms, exact ingredient IDs/counts,
  Forge NBT, Fabric result ID, model parent/texture, resource-copy hashes,
  metadata, and release relations.
- A missing/invalid Forge guide ID must not craft. An invalid Fabric initial page
  may fall back only through AE2's documented previous-page behavior and must log
  the missing page during tests.
- Dedicated servers may load item/recipe data but never client guide screen
  classes. Optional Forge class resolution is tested with GuideME absent.
- The test driver identifies the stack by item plus NBT on Forge and by item ID
  on Fabric, then proves actual screen/page semantics instead of metadata only.

## Verification map

| Acceptance area | Proof |
| --- | --- |
| Recipe and identity | build parser checks plus 2x2/3x3 crafting, invalid substitutes, sync, and reload |
| Content parity | source/output hashes, page/link/navigation validation, English and Ukrainian rendering |
| Optional behavior | Forge client/dedicated startup and recipe lookup with and without GuideME |
| Fabric path | AE2 15.x screen opens at `ae2craftingtime:index.md` and navigates Chapter 1 |
| Distribution | four-row build regression, JAR content audit, and release-script dry-run relations |
