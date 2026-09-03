# Guide Book Implementation Plan

Implement the [spec](spec.md) using the resource-only
[design](technical-design.md). This planning change does not implement the mod.

## 1. Confirm baseline and dependencies

- Read `AGENTS.md`, applicable development/writing/release skills, and these
  documents. Fetch the current base and confirm the minimum artifacts cited.
- Add the designed required GuideME metadata to both modern module TOMLs.
  Keep AE2 minimums and old-target metadata unchanged.
- Extend only modern Modrinth/CurseForge dependency declarations and update
  `docs/dependencies.md`. Keep compatible runtime pins; no broad dependency
  refresh, bundling, or compile-time GuideME API dependency.
- Use the design's exact PowerShell/Bash upload seams and conditional file
  relations; extend both existing deployment dry-run tests. No live release.

Gate: modern clients and dedicated servers, including AE2 19.0.24 installs,
are told to install GuideME. No old target requires it. A6–A7.

## 2. Add book identity and bilingual content

- Add shared `guideme_guides/guide.json` with the designed guide ID, title key,
  and vanilla book model. Add the exact English/Ukrainian title translations.
- Add `index.md`, `getting-started.md`, and their `_uk_ua` copies at the
  designed resource root. Use the spec's supplied introduction/chapter copy.
- Add localized navigation, chapter parent, and reciprocal links. No empty
  later chapters or `item_ids` bindings.
- Check claims against current `TtcText`, estimate renderers, and persistence.
  Correct factual drift without expanding chapter scope.

Gate: exactly two useful pages per language, matching structure, no custom
item class/texture/key/packet or profiler change. A2–A5, A8.

## 3. Add crafting and discovery

- Add `data/ae2craftingtime/recipe/guide_book.json` in both modern modules:
  object ingredients for 1.21.1, strings for 26.1.2, same component-bearing result.
- Add each module's `advancement/recipes/misc/guide_book.json`: unlock on an
  ordinary book, with standard recipe-unlocked criterion/reward as designed.
- Use exact item inputs; clock is consumed. Keep recipe and advancement data
  out of shared resources and both old modules.

Gate: both native recipes and discovery are defined without serializers or
item registration. A1, A6–A7.

## 4. Add one resource check and commit through the hook

- Add root `checkGuideResources`, wired to `test`, using `JsonSlurper` and
  bounded text checks; no new parser or framework.
- Check guide ID/name/model, component-bearing result, exact ingredients/count,
  target JSON shape, recipe/unlock IDs, bilingual keys/page paths, navigation,
  local links, dependency declarations, and absence of old-target recipe data.
- Review all changes. Update `docs/feature-coverage.md` to describe the actual
  implemented two-target behavior.
- Make one conventional feature commit using the required post-commit hook.
  Do not run local tests before the hook creates the PR.

Gate: one reviewable feature commit/PR, with checks ready. A4–A8. The planning
PR itself must not close #144 as implemented.

## 5. Validate resources, packages, and native data

- After PR creation, run `checkGuideResources` and applicable skill-required
  checks. Temporarily remove one Ukrainian page, break one link, and omit a
  recipe result's guide ID in turn; confirm the task rejects each, restore it,
  and rerun. Never commit broken fixtures.
- Run the existing PowerShell/Bash deployment checks after updating their
  relation assertions. Verify required GuideME/AE2 relations on modern rows,
  unchanged old-row payloads, and no network upload in dry-run mode.
- Build the four release-matrix rows through the repository workflow. Inspect
  JAR contents: guide assets in shared resources; recipe/advancement data and
  required GuideME metadata only on the two modern targets.
- Start a dedicated server on each modern target. Check native recipe and
  advancement decoding, then datapack reload, with no new warnings/errors.
- On the 1.21.1 minimum graph, remove GuideME once to confirm a clear loader
  dependency failure, then restore it. For 26.1.2 confirm our requirement agrees
  with AE2's existing required dependency.

Gate: structural checks, packages, servers, and dependency boundaries pass
with exact version sets recorded. A1, A4–A8.

## 6. Targeted player verification

Use `run-ae2-client-smoke` and `use-codex-vm`; obey their launch, timing, and
screenshot rules. Run clients sequentially using existing `scripts-run`
launchers. No new broad driver framework is required.

| Check | Evidence | Criteria |
| --- | --- | --- |
| Survival crafting | Fresh player obtains an ordinary book and discovers recipe. Craft in 2×2 and 3×3 grids at different positions; all six ingredient orderings match. Output is one book with our guide ID. Repeat/shift crafting consumes one of each per result, including clock. | A1 |
| Rejected inputs | No output with missing ingredient, unrelated extra item, quartz dust, charged Certus, Nether Quartz, written book, or enchanted book. | A1 |
| Appearance/use | Inventory/hand/dropped appearance matches vanilla book with no glint. Main-hand and eligible off-hand use opens our guide repeatedly without consuming it. Ordinary book and AE2 guide unchanged. | A2–A3 |
| Reading | First opening shows introduction; navigate to chapter and back, close/reopen, use Home, and search chapter text without parser/model errors. | A3–A4 |
| Language/themes | English (`en_us`) names and complete pages in both themes; links, navigation, and resource reload work. Check Ukrainian page/key parity and fallback structurally, without another smoke language. | A4–A5 |
| Save/multiplayer | Store crafted book in chest, reload world, retrieve/open. On each modern dedicated server, craft/drop/pick up, reconnect, and reopen. Inspect persisted component and correct guide destination. | A3, A6 |
| Old targets | Start 1.20.1 Forge/Fabric without adding GuideME for this feature; no new recipe errors or dependency requirement. | A7 |

Run the full targeted path on GuideME minimums `21.1.0` and
`26.1.10-alpha`. If prepared compatible modern profiles use other pins,
also verify their recipe loading, model, and opening. Report each version set
separately rather than treating one run as proof for another.

Capture crafting result/title, introduction, and chapter one for both modern
targets in English only. Archive through
`docs/ui-smoke-evidence.md`. Report actual checks, fixes, and GitHub CI
separately; research checks do not count as runtime evidence.

## Completion gate

- A1–A8 have recorded evidence on the stated matrix.
- Both languages are complete and accurate; no later chapter is implied to exist.
- Four distributions preserve target boundaries; both modern client/server
  paths pass; new repository-owned warnings/errors are resolved.
- Implementation PR links #144 and all three documents, states the new direct
  GuideME dependency, and reports real local verification and GitHub CI.
- Issue updates/closure, merge, and release follow their own authorization
  workflows. The planning PR is not feature completion.
