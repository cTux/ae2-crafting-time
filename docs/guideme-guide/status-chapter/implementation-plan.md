# Status Chapter Implementation Plan

Implement [#305](https://github.com/cTux/ae2-crafting-time/issues/305) as one
documentation feature commit after the approved planning change.

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

The original ten-page implementation above is complete. For
[#412](https://github.com/cTux/ae2-crafting-time/issues/412), execute only these
steps; do not recreate those pages or their images. The new completion gate is
eleven pages, S11-S13 plus S3 and S5-S10 for the addition.

1. Read the recurrence spec/design and current locale keys. Confirm the
   native-plan scope, mixed-shortage quantity, recovery, and clearing rules.
   Author both `statuses/recurrent.md` locale peers as specified (S11).
2. Append navigation position 10, add the separate Crafting Plan explanation in
   both landing pages, and add each estimate page's Next link. Verify return,
   previous, and time-estimates links agree across locales (S5, S6, S12).
3. Use the prepared-client smoke workflow's `recurrent-plan` evidence to obtain
   and inspect an exact Recurrent crop. Package the PNG, record its source, and
   write matching captions/alt text. Use the guide-image refresh workflow for
   the crop. Do not fabricate an image or treat old unreviewed evidence as a
   current screenshot (S3, S7, S13).
4. Extend root `build.gradle`'s existing `statusPages` map as designed. Reuse the
   existing failure checks for missing page, locale, label, image, position, and
   navigation; check the formatted recurrence label. Keep transformations and
   packaging ownership unchanged (S9).
5. After the implementation commit hook creates the PR, run `checkGuideResources`,
   applicable resource processing/four-JAR checks, and `git diff --check`.
   Inspect packaged locale pages and image in all four distributions (S8, S9).
6. Follow the prepared-client smoke policy sequentially. Open the new page from
   the book, search for it, traverse its links, and compare its screenshot/text
   to the native-plan label and hover help. Exercise the GuideME and Fabric
   renderer paths, readability in both themes, and translation parity under the
   current locale verification policy. Retain actual evidence; do not substitute
   a successful resource check for rendered-page review (S10-S13).

Close #412 only after the book implementation and these checks pass. This
planning-only PR updates the three documents, checks their consistency and
links, and leaves game resources unchanged; it does not require a client launch.
