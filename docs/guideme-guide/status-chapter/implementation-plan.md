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
