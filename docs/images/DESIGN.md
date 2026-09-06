# GitHub README presentation

## Direction

Use the approved charcoal-and-cyan README concept: the supplied hourglass,
Minecraft lettering, clear downloads, one real screenshot, and short sections.
GitHub owns the page layout, typography, link states, borders, and light/dark
themes. This is a Markdown document, not a custom website.

## Banner

- Canvas: 1600 by 420 pixels; scales to the available README width.
- Background: AE2's Sky Stone Small Bricks texture, tiled at an integer scale
  and darkened for readable text.
- Frame: AE2 Crafting CPU ring side and corner textures at an integer scale.
- Title: `#f0f6fc`; tagline: `#48e6ee`; shadow: `#080e12`.
- Artwork: `project-icon.png`, the user's original 512-pixel PNG, unchanged.
- Lettering: the actual default ASCII bitmap glyphs in Minecraft 1.20.1,
  rendered at integer scales. No installed font or generated lettering.
- Copy: “It's AE2 Crafting Time!” and “Autocrafting adventure you might want to
  understand in details”.

## Page structure and primitives

Use GitHub's native paragraphs, links, badges, headings, table, and disclosures.
Keep download links in a centered paragraph so they can wrap. The banner has
descriptive alternative text; the screenshot has a caption and gallery link.
The three feature columns use a normal Markdown table. Detailed features,
technical documentation, and project statistics remain available in disclosures.
Preserve the development disclosure and unofficial status.

## Responsive and accessibility behavior

The banner and screenshot shrink with GitHub's content column. Downloads wrap;
the feature table uses GitHub's native overflow behavior. Body text remains real,
selectable text and follows the reader's theme and zoom settings. Links keep
native focus/hover states. No motion or scripts are needed.

## Source and regeneration

The hourglass was supplied as `ae2-crafting-time-512-optimized.png`. Its original
bytes are retained as `project-icon.png`; it is a README branding asset, not an
in-game asset. Minecraft's font atlas and AE2's textures are read from locally
installed JARs and are not included separately in this repository.

On Windows, from the repository root:

```powershell
./scripts/render-readme-banner.ps1 `
    -MinecraftClientJar 'path/to/minecraft-1.20.1-client.jar' `
    -Ae2Jar 'path/to/appliedenergistics2-forge-15.4.10.jar'
```

This writes `docs/images/readme-banner.png`. The script uses Windows' built-in
System.Drawing library and does not download anything.

## AE2 texture attribution

The banner background and frame use modified copies of these Applied Energistics
2 textures from version 15.4.10:

- `sky_stone_small_brick.png`
- `crafting/ring_side_hor.png`
- `crafting/ring_side_ver.png`
- `crafting/ring_corner.png`

Applied Energistics 2 textures and models are copyright (c) 2020 Ridanisaurus
Rid and copyright (c) 2013-2020 AlgorithmX2 et al. The modified banner is
licensed under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/).

## Verification scope

Compare the banner and section hierarchy with the approved mockup, then inspect
the actual GitHub-rendered README, including expanded disclosures. GitHub's
native spacing and badge shapes replace the mockup's illustrative browser styling.
Check local links, image loading, narrow layouts, and light/dark contrast.
