# Guide Book Technical Design

## Decision

Use `guideme:guide` with the persistent component
`guideme:guide_id = ae2craftingtime:guide`. This is our book's stack identity;
there is no separately registered `ae2craftingtime:guide_book` item. A vanilla
shapeless recipe produces that stack, and GuideME supplies opening, localized
naming, model selection, navigation, and search.

This meets the [spec](spec.md) without new Java, renderer, registry, packet
handler, or second documentation library.

## Research evidence — 2026-09-02

| Evidence inspected | Finding and consequence |
| --- | --- |
| [Data-driven guides](https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/20-data-driven-guides.md) | The generic item selects its guide through a component; `item_settings` supplies title and model. |
| [21.1.0 GuideItem](https://github.com/AppliedEnergistics/GuideME/blob/v21.1.0/src/main/java/guideme/internal/item/GuideItem.java) and [26.1.10-alpha GuideItem](https://github.com/AppliedEnergistics/GuideME/blob/v26.1.10-alpha/src/main/java/guideme/internal/item/GuideItem.java) | Both read the component, open on the logical client, and leave the held stack intact. |
| [21.1.0 component registration](https://github.com/AppliedEnergistics/GuideME/blob/v21.1.0/src/main/java/guideme/internal/GuideME.java) | Persistent and network codecs preserve the guide identity through normal item save/sync. |
| [21.1.0 model dispatcher](https://github.com/AppliedEnergistics/GuideME/blob/v21.1.0/src/main/java/guideme/internal/item/GuideItemDispatchUnbakedModel.java) and [26.1.10-alpha dispatcher](https://github.com/AppliedEnergistics/GuideME/blob/v26.1.10-alpha/src/main/java/guideme/internal/item/GuideItemDispatchModel.java) | Both accept the configured block-model ID. Use `minecraft:item/book`; no new 26.1 item-definition file because GuideME owns the item. |
| [21.1.0 definition codec](https://github.com/AppliedEnergistics/GuideME/blob/v21.1.0/src/main/java/guideme/internal/datadriven/DataDrivenGuide.java) | Supports `item_settings`; omit newer optional fields such as `custom_colors` and `default_language`. |
| [Authoring](https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/30-authoring/index.md) and [translation](https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/60-translation.md) | Guide-specific resource root, Markdown, navigation frontmatter, relative links, and `_uk_ua` fallback layout. |
| Cached `appeng:appliedenergistics2:19.0.24`, `META-INF/neoforge.mods.toml` | Does **not** require GuideME. Our runtime-only development pin is not a published dependency guarantee. |
| Cached `org.appliedenergistics:appliedenergistics2:26.1.10-beta`, same metadata entry | Requires GuideME `>=26.1.10-alpha` on both sides. Retain that minimum. |
| Recipes in those AE2 JARs, including `data/ae2/recipe/charger/charged_certus_quartz_crystal.json` | 1.21.1 uses ingredient objects; 26.1.2 uses strings. Both use singular `recipe/` and result `id`. |
| [Release matrix](../../scripts/release-matrix.json), modern build files, [architecture](../architecture.md) | Shared main assets reach all releases; active recipe data belongs only to modern modules. |
| [GuideME tags](https://github.com/AppliedEnergistics/GuideME/tags) | Official 20.1 releases exist. The 1.20.1 exclusion is scope, not availability. |

Tagged source and locally inspected minimum artifacts establish compatibility;
upstream main documentation explains authoring. Research is not an in-game test.

## Resource ownership

```text
shared/src/main/resources/assets/ae2craftingtime/
  guideme_guides/guide.json
  guides/ae2craftingtime/guide/
    index.md
    getting-started.md
    _uk_ua/index.md
    _uk_ua/getting-started.md
  lang/en_us.json                       # add guide.ae2craftingtime.name
  lang/uk_ua.json                       # same key

versions/1.21.1-neoforge/src/main/resources/data/ae2craftingtime/
  recipe/guide_book.json                 # conditioned on GuideME
versions/26.1.2-neoforge/src/main/resources/data/ae2craftingtime/
  recipe/guide_book.json                 # conditioned on GuideME
```

Shared assets are inert on 1.20.1 without GuideME. Do not share active recipe
or advancement data. Incidental resource discovery if players independently
install GuideME on an old target is outside our support claim; no gating code.

Shared definition:

```json
{
  "item_settings": {
    "display_name": { "translate": "guide.ae2craftingtime.name" },
    "model": "minecraft:item/book"
  }
}
```

English name: `AE2 Crafting Time Guide`. Ukrainian:
`Посібник AE2 Crafting Time`. Do not replace the global GuideME/vanilla book
model, copy textures, or override GuideME's global item-name translation.

`index.md` navigation: title `AE2 Crafting Time`, position `0`, icon
`minecraft:book`. Chapter navigation: localized chapter title, parent
`index.md`, position `0`, icon `minecraft:clock`. Reciprocal links target
`getting-started.md` and `index.md`. Ukrainian copies keep these paths and
parent values; translate titles, headings, link labels, and body copy.

No `item_ids` entries: the book selects our guide directly without competing
with AE2 item documentation. GuideME owns Home, history, language fallback,
search, and themes. No custom reader position is stored by this mod.

## Recipe and discovery

Recipe ID: `ae2craftingtime:guide_book`. Exact inputs are deliberate: the
checked AE2 Certus tag also accepts charged crystals.

1.21.1 recipe:

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [
    { "item": "minecraft:book" },
    { "item": "ae2:certus_quartz_crystal" },
    { "item": "minecraft:clock" }
  ],
  "result": {
    "id": "guideme:guide",
    "count": 1,
    "components": { "guideme:guide_id": "ae2craftingtime:guide" }
  }
}
```

26.1.2 uses the same type, category, and result, replacing only the ingredient
array with:

```json
[
  "minecraft:book",
  "ae2:certus_quartz_crystal",
  "minecraft:clock"
]
```

Use vanilla consumption/remainders; no custom serializer. Add a
`neoforge:mod_loaded` condition for `guideme` to each recipe so missing GuideME
does not produce an unknown-item recipe error. Recipe discovery follows
Minecraft's normal recipe behavior; no separate advancement is needed.

## Dependency contract

Add an optional `guideme` dependency to each modern module's
`src/main/resources/META-INF/neoforge.mods.toml`, with `side="BOTH"` and
`ordering="AFTER"`:

| Target | Range | Build/runtime ownership |
| --- | --- | --- |
| 1.21.1 NeoForge | `[21.1.0,)` | Keep the existing minimum runtime dependency; latest client preparation supplies its compatible pin. No Java compile dependency. |
| 26.1.2 NeoForge | `[26.1.10-alpha,)` | Existing AE2 transitive resolution supplies the minimum when present; no bundled copy. |

Add GuideME project `Ck4E7v7R` as optional in the two modern Modrinth rows of
`scripts/release-matrix.json`. The current CurseForge uploaders do not send
file relations. In `Publish-CurseForge` in `scripts/deploy-changed.ps1` and
`publish_curseforge` in `scripts/deploy-changed.sh`, when the row lists GuideME,
add `relations.projects` with required `applied-energistics-2` and optional
`guideme` entries. Each uses `slug`; the types are `requiredDependency` and
`optionalDependency`, as defined by the official
[CurseForge Upload API](https://support.curseforge.com/support/solutions/articles/9000197321).
Include AE2 explicitly so these file relations retain the base dependency.
Leave old-row payloads unchanged. Echo these relations in dry-run output and
extend the existing `scripts/test-deploy-changed.ps1` and `.sh` checks to prove
both modern inclusion and old-row absence; do not perform a live upload.
Update `docs/dependencies.md`; mark the book shipped in
`docs/feature-coverage.md` only when implemented. No old-target dependency.

This does not change installation requirements or the AE2 minimum. Missing
GuideME leaves the book recipe unavailable while the rest of the mod keeps
working. Verify the optional ranges against the implementation-time artifacts
without silently expanding scope.

## Runtime flow and failures

1. When installed, GuideME registers its item and component on both logical sides.
2. Server loads the conditioned vanilla recipe; crafting creates the component-
   bearing stack and syncs it through normal inventory synchronization.
3. Client resources register our guide, two pages, and localized name.
4. GuideME reads the ID for rendering/name/use; opening stays client-side.
5. Vanilla saves the component; GuideME handles native reading history.

Dedicated servers need no guide screen or Markdown parsing to craft the item.
Install GuideME on both sides to use the guide. No external page fetch, custom
world state, command execution, or new network trust boundary is introduced.
Client resource reload and server datapack reload must preserve a usable book.

Malformed or missing components/resources fail implementation checks. Native
unknown-guide/missing-page messages are diagnostics, not accepted behavior.
There are no shipped guide items to migrate. Keep the chosen guide ID stable
after release.

## Validation and alternatives

Add one root `checkGuideResources` Gradle task using built-in Groovy JSON and
bounded text checks; attach it to the existing `test` lifecycle. Validate
definition/name/model, bilingual page topology, relative links, exact recipes
and result components, conditions, translation keys, metadata, and no old-target
recipe data. Do not require translated headings to equal English headings.

Native client/server checks establish codec validity, appearance, use,
persistence, navigation, translation, and search. The
[implementation plan](implementation-plan.md) maps these to A1–A8.

A Java book duplicates GuideME behavior. A renamed vanilla book cannot open
the guide. A global model override affects other books. Patchouli/custom
readers add an unnecessary UI and dependency. None is needed here.
