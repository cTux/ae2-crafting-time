# Status Chapter Implementation Plan

Lifecycle: see the [scope status and evidence](spec.md).

Implement [#305](https://github.com/cTux/ae2-crafting-time/issues/305) as one
documentation feature commit after the approved planning change.

Sections 1-6 and their completion gate below record the completed original
ten-page delivery. NO CHANNEL was added afterward. For current #412 work, use
only the Recurrent follow-up and wiki publication sections.

## 1. Lock status inventory

- Re-read renderers, status aggregation, `TtcText`, locales, and status designs.
- Confirm ten states and precedence on all targets; seek new approval on drift.

Gate: S1, S2, S4 with current source evidence.

## 2. Author English pages

- Add landing and ten children at exact paths/positions.
- Include label, location, condition, recovery, clearing, limits, image/caption,
  and links; add the compact decision path and chapter links.

Gate: S1-S5.

## 3. Complete screenshot coverage

- Copy seven reviewed images. Extend the existing provider-dispatch scenario to
  capture NO TARGET, INPUT BLOCKED, and LOCKED with no controls in frame.
- Inspect native files, update gallery evidence, alt text, and captions.

Gate: S3 and S7.

## 4. Add Ukrainian parity

- Translate naturally using exact UI labels while preserving paths, positions,
  images, links, conditions, recovery, and limits; compare trees side by side.

Gate: S6.

## 5. Extend validation

- Add manifest, label, navigation, link, image, reachability, and locale checks
  to `checkGuideResources`.
- Add the smallest focused check for missing page/image/return link/locale peer/
  stale label; extend four-JAR assertions.

Gate: S9 and packaging coverage for S8.

## 6. Verify UI and guide

- Let the commit hook create/update the PR before tests.
- Run docs/link checks, `checkGuideResources`, focused build-logic checks,
  resource processing, and `git diff --check`.
- QA both guide renderer paths: navigation, search, pages/images/links, themes,
  locale switch, optional GuideME absence. Run status scenarios on applicable
  targets and compare labels, tooltips, precedence, mixed batches, and clearing.
  Archive evidence; report local checks separately from CI.

Gate: S8-S10 and required CI green.

## Completion gate

S1-S10 have evidence; all ten pages work in both languages; all four JARs have
correct resources with no runtime/dependency/recipe changes; the PR links #305
and all three planning documents.

## Recurrent follow-up (#412)

The original ten-page implementation above and the later NO CHANNEL page are complete. For
[#412](https://github.com/cTux/ae2-crafting-time/issues/412), execute only these
steps; do not recreate those pages or their images. The new completion gate is
twelve pages, S11-S14 plus S3 and S5-S10 for the addition. Preserve all eleven
existing positions, including NO CHANNEL at 3 and TTC estimate at 10.

### Before implementation: verify runtime prerequisites

The September 20 investigation found CodexVM powered off. Under authorized VM
access, start/reuse the dedicated VM and check SSH, guest JDKs, installed native
loader libraries, fixture markers and the exact worktree share before committing
to runtime verification. Inspect `launch.json` under the prepared root documented
in `docs/dev-client.md`; use its loader-specific subdirectory when necessary.

| Purpose | Target and profile | Loader / AE2 / renderer | Java |
| --- | --- | --- | --- |
| GuideME page review | `1.21.1-neoforge`, compatible | NeoForge 21.1.251 / AE2 19.2.17 / GuideME 21.1.19 | 21 |
| Bundled renderer review | `1.20.1-fabric`, compatible | Fabric 0.19.5 / AE2 15.1.0 / AE2 bundled guide | 17 |
| Additional packaging | `1.20.1-forge` | Forge 47.4.23 / AE2 15.4.10 / GuideME 20.1.15 | 17 |
| Additional packaging | `26.1.2-neoforge` | NeoForge 26.1.2.109 / AE2 26.1.10-beta / GuideME 26.1.12-beta | 25 |

These pins come from `scripts/run-client-versions.json` at
`8561ae116b9439f122a77b89b79216c1e12d65f4`. Reconcile any later catalogue changes
before launching. In particular, the older GuideME review used NeoForge
21.1.238; it does not prove a prepared 21.1.251 installation exists.

Use the existing host build, guest-local staging, source-only fixture copy and
exact-process cleanup paths. `prepare-ui-smoke-launch.ps1` validates an existing
loader installation; it does not provision missing native loaders. Missing
manifests, guest runtimes, fixtures or matching installations stop implementation
readiness until a documented prerequisite is delivered or explicitly authorized
within scope. Do not substitute another profile or add substantial verification
infrastructure silently. No new guide automation or full recurrence campaign is
required for this content change.

### Implement and verify the addition

1. Read the recurrence spec/design and current locale keys. Confirm the
   native-plan scope, mixed-shortage quantity, recovery, and clearing rules.
   Author both `statuses/recurrent.md` locale peers as specified (S11).
2. Append navigation position 11, add the separate Crafting Plan explanation in
   both landing pages, and add each estimate page's Next link. Verify return,
   previous, and time-estimates links agree across locales (S5, S6, S12).
3. Use the prepared-client smoke workflow's `recurrent-plan` evidence to obtain
   and inspect an exact Recurrent crop. Package the PNG, record its source, and
   write matching captions/alt text. Use the guide-image refresh workflow for
   the crop. Do not fabricate an image or treat old unreviewed evidence as a
   current screenshot (S3, S7, S13).
4. Extend root `build.gradle`'s existing `statusPages` map as designed. Reuse the
   existing failure checks for missing page, locale, label, image, position, and
   navigation. Format the `%s` translation with the pages' fixed example amount
   and add a negative check that rejects a stale formatted Recurrent label.
   Keep exact validation for other labels. Keep transformations and
   packaging ownership unchanged (S9).
5. After the implementation commit hook creates the PR, run `checkGuideResources`,
   applicable resource processing/four-JAR checks, and `git diff --check`.
   Run `powershell -NoProfile -File scripts/test-export-guide-image.ps1` for
   the image export workflow. Build the four `distMod` tasks named by
   `scripts/release-matrix.json`; resource processing alone does not prove JAR contents.
   Inspect packaged locale pages and image in all four distributions (S8, S9).
6. After prerequisite checks and cheap validation pass, use the two renderer
   targets above sequentially through the existing prepared-client interactive
   launch and VNC workflow. No dedicated guide scenario currently exists. Open the new page from
   the book, search for it, traverse its links, and compare its screenshot/text
   to the native-plan label and hover help. Exercise the GuideME and Fabric
   renderer paths, readability in both themes, and translation parity under the
   current locale verification policy. Retain actual evidence; do not substitute
   a successful resource check for rendered-page review (S10-S13).

   Before launching, record a wall-time budget and the planned processes: one
   guest Minecraft client at a time, two renderer sessions, and only a focused
   recurrence session if a replacement capture is needed. Record actual timings
   and stop bounded lack of progress before retrying; do not expand to a full matrix
   to diagnose a guide page.

   Keep runtime smoke in English under the current locale policy; verify
   Ukrainian text, label formatting, matching links and identical images through
   resource checks and human translation review. Review both themes at 1920x1080,
   with the game maximized and effective GUI scale recorded. Record the tested
   implementation SHA, exact profile, process, screenshots, manual verdict and
   cleanup. If another resolution is required, reconcile the image-review policy
   before substituting it. Check the new page and changed navigation, not every
   unchanged historical status scenario.

   Prefer the inspected retained `recurrent-plan` capture for the book image.
   If it cannot yield a readable private-data-free crop, run only `recurrent-plan`
   on the prepared 1.21.1 NeoForge compatible target after plan-only selection.
   Inspect the raw image and sidecar, retain provenance and distinguish reused
   illustration evidence from current-head rendered-book verification.

Close #412 only after the book checks and live wiki publication below pass. This
planning-only PR updates the three documents, checks their consistency and
links, and leaves game resources unchanged; it does not require a client launch.

## Publish the matching wiki update (S14)

After the book implementation is merged, extend the completion gate above with:

1. Refresh the separate wiki checkout and mirror the canonical English/Ukrainian
   Recurrent pages using the file names and link mapping in the design. Update
   both status landings and estimate-page navigation; copy the reviewed PNG.
2. Compare the adapted pages with the merged guide source, verify both locales'
   internal links and image targets, and review the wiki diff for unrelated edits.
3. Commit and publish the wiki update. Open both live Recurrent pages on GitHub,
   follow landing/estimate/return/related links, and inspect the rendered image,
   caption, and text. Record source and wiki commits plus the live page URLs.

S14 is required alongside the book checks before #412 is complete. Issue #411
already delivered the initial wiki and is not a blocker. This planning follow-up
adds the publication requirement; it does not publish incomplete book content.
