# Feature Chapter Implementation Plan

Implement [#304](https://github.com/cTux/ae2-crafting-time/issues/304) as one
documentation feature commit after the approved planning change.

## 1. Lock inventory and evidence

- Reconcile the 13 pages with current shipped `docs/feature-coverage.md` and
  defining docs; review every candidate image.
- If shipped scope changed, update issue and planning docs and obtain approval.

Gate: each shipped non-status feature maps once; no planned/internal content.

## 2. Author English pages

- Add the landing and 13 pages at the designed paths/positions.
- Include purpose, location, use, example, limits, screenshot/caption, and links.
- Link root, Chapter 1, and Statuses without duplicating recovery content.

Gate: F1-F4.

## 3. Package screenshots

- Copy reviewed existing images; capture only saved-history, configuration,
  addon-support, and guide-book gaps through the existing workflow.
- Inspect native-resolution files, update the gallery, alt text, and captions.

Gate: F3 and F6.

## 4. Add Ukrainian parity

- Translate naturally while preserving facts, paths, positions, images, links,
  and limits; compare both trees side by side.

Gate: F5.

## 5. Extend validation

- Extend `checkGuideResources` with the designed manifest/link/image/parity rules.
- Add the smallest focused build-logic check proving missing page, image, return
  link, and locale peer are rejected; extend four-JAR assertions.

Gate: F8 and packaging coverage for F7.

## 6. Verify surfaces

- Let the commit hook create/update the PR before tests.
- Run docs/link checks, `checkGuideResources`, focused build-logic checks,
  target resource processing, and `git diff --check`.
- QA GuideME and Fabric renderer paths: navigation, search, every page/image/link,
  themes, locale switch, and optional-dependency absence. Archive evidence and
  report local checks separately from CI.

Gate: F7-F9 and required CI green.

## Completion gate

F1-F9 have evidence; all 13 pages work in both languages; all four JARs contain
correct resources without runtime/dependency/recipe changes; the PR links #304
and all three planning documents.
