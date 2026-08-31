# GuideME Guide Implementation Plan

## 1. Reconcile The Source Of Truth

- Read `docs/feature-coverage.md` and every linked shipped player-facing spec.
- Compare config defaults/ranges, translations, dependency metadata, and
  release-matrix rows with the current code and packaged metadata.
- Exclude planned features and record any documentation drift as a separate
  fix before using it as guide content.

Gate: the content checklist names only behavior present in current release
artifacts.

## 2. Add The Data-Driven Guide Shell

- Add `assets/ae2craftingtime/guideme_guides/guide.json` for
  `ae2craftingtime:guide` using GuideME's generic item settings and the
  translatable mod name.
- Add the seven English pages and their navigation/frontmatter.
- Map the smallest useful set of AE2 autocrafting items to relevant pages for
  GuideME's open-guide key.
- Use native GuideME Markdown, links, item links, keybind tags, and navigation;
  add no Java integration unless resource-only behavior fails an acceptance
  check.

Gate: GuideME loads the guide, the start page, and every navigation node on
both modern targets.

## 3. Write Complete Player Documentation

- Explain the overview, estimates, screens, diagnostics, controls/config,
  integrations, and troubleshooting/privacy areas from the specification.
- Include exact defaults, ranges, controls, state precedence, supported target
  matrix, and optional integration limits.
- State uncertainty and limitations beside the related feature rather than in
  a detached disclaimer.
- Add links between related topics and back to the overview.

Gate: every shipped player-facing feature-map entry maps to at least one guide
section, and no planned feature is presented as shipped.

## 4. Add Ukrainian Content

- Translate the full page set under `_uk_ua`.
- Preserve filenames, navigation topology, item targets, links, code/config
  identifiers, numeric values, and placeholders.
- Keep player terminology aligned with the existing Ukrainian language JSON.

Gate: English and Ukrainian pass structural parity and link checks.

## 5. Add The Smallest Structural Check

- Add one focused test that validates the guide definition, bilingual page
  parity, navigation, relative links, item ids, and required content headings.
- Extend existing resource/JAR checks only where needed to prove both modern
  distributions contain the guide.
- Do not add a new parser or test framework; use existing JUnit and standard
  Java file/JSON support already present in the build.

Gate: the test fails for a missing translation page, broken link, invalid
navigation target, or absent required topic.

## 6. Verify In Game

- Build the `1.21.1 NeoForge` and `26.1.2 NeoForge` rows.
- Inspect each JAR for the definition and complete bilingual resource set.
- Run prepared clients for both rows and verify guide discovery, relevant-item
  opening, navigation, search, light/dark themes, and English/Ukrainian pages.
- Start both 1.20.1 rows and confirm the inert resources do not change startup
  or UI behavior.
- Run documentation/link checks and `git diff --check`.

Completion gate: all structural, packaging, and four-row compatibility checks
pass; the two GuideME-capable clients complete the full guide smoke path; and
the issue acceptance criteria are checked against the resulting artifacts.
