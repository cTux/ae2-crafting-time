---
name: refresh-guide-images
description: Refresh AE2 Crafting Time gallery and GuideME screenshots from reviewed UI smoke evidence. Use for screenshot crops and book image readability, not new artwork or running a general smoke campaign.
---

# Refresh gallery and guide images

Use the exact pack, release, and smoke run requested by the user. Read the run's
manifest, semantic results, image sidecars, and visual review before selecting
captures. A semantic pass alone does not prove that an image shows its caption.
If a context is missing, capture it in a focused smoke on that same pack through
`launch-prism-test-modpack`; never substitute an older pack's screenshot.

Inspect each original. Crop to the described rows, total, or complete tooltip,
keeping enough context to explain the feature. Exclude unrelated HUD, chat,
account names, coordinates, and server details. Do not redraw, retouch, or
compose UI evidence. Keep raw PNGs and sidecars unchanged in the smoke archive.

Use `scripts/export-guide-image.ps1` from the repository root. Supply `Source`,
`Destination` ending in `.jpg`, and physical-pixel `X`, `Y`, `Width`, `Height`.
The default quality is 90; inspect small colored text for compression damage.
Record the source run, relative capture path, SHA-256, and crop rectangle in
`docs/images` so a later maintainer can reproduce the exports.

Gallery exports keep the crop's native size. GuideME's image layout divides
intrinsic dimensions by four before clamping to page width. Export book copies
with `-OutputWidth <min(1600, cropWidth * 4)>` to fill the available width at ordinary GUI scales;
this changes display size, not the information in the original capture. Confirm
this behavior against the supported GuideME versions when they change.

Update `docs/images/README.md`, other gallery references, and every screenshot
page under `shared/src/main/resources/assets/ae2craftingtime/guides/ae2craftingtime/guide`,
including `_uk_ua`. Keep paired locale images identical. Preserve branding PNGs.
Update `checkGuideResources` image-extension checks when changing formats.

Follow the repository commit/PR order, then run `checkGuideResources` and the
export script's self-test. Inspect the actual book at 1920x1080 on the requested
pack: every screenshot must load, use the available width, retain readable UI
text, and remain inside the scrollable page without covering navigation. Review
every page that embeds an image, including both locales; exports alone do not
verify in-game rendering. Report actual byte sizes and link the reviewed smoke
provenance with the issue. Close or merge only when the task authorizes it.

For compact book copies, start at `-Quality 75`; keep gallery quality 90.
Compare actual totals with the previous files and disclose any increase needed
for readability. Do not claim JPEG is always smaller than PNG for pixel art.
The export self-test is `powershell -NoProfile -File scripts/test-export-guide-image.ps1`.
