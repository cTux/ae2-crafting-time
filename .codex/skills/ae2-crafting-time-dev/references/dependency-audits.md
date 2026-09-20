# Dependency release audits

Inventory supported targets from `scripts/release-matrix.json`, integrations
from `docs/dependencies.md`, and exact client pins from
`scripts/run-client-versions.json`. Check official release files for each
Minecraft/loader pair, including prereleases and separately published forks.
Record retrieval date, exact version/file ID, source URL, and required dependencies.
Compare artifact IDs, not only display versions. Search open and closed issues
before creating follow-ups; do not duplicate already tracked work.

## Classify each result

- New loader release on a supported Minecraft target: attempt a version-only
  update and base AE2 UI smoke on that target.
- New release of an existing optional addon on an existing target: attempt a
  version-only update and only that addon's focused UI smoke on that target.
- Existing addon newly publishes another supported Minecraft/loader pair:
  create one issue per addon/target pair. Publication is not integration proof.
- New potential optional dependency: create a research issue with its actual
  crafting, storage, or UI relevance, published target files, and acceptance checks.
- A candidate needs production, mixin, test-driver, or integration fixes: create
  a separate issue with the evidence and retain the previous supported version.
  Do not implement those fixes during a research-only audit.

Distinguish confirmed API/runtime failures from unavailable files, missing native
loader installations, missing test scenarios, and other verification blockers.
Never call an untested candidate incompatible or supported.

## Version-only means version-only

Keep minimum build dependencies and loader declarations unchanged. Optional
runtime ranges remain open-ended: "maximum supported" means the highest
verified release, not a new rejection cap. Change only the candidate's exact
client pin and corresponding version/evidence documentation. Keep other pins
fixed; include only required transitive dependencies in its focused runtime.
If the candidate requires broader changes, document them in its follow-up issue.

Create the PR before running local smoke, as required by `AGENTS.md`. Use the
prepared-client skill with explicit `-Target`, `-ProjectId`, and `-Scenario`;
use `-BaseOnly` and a native AE2 UI scenario for a loader-only bump. Do not use
`-Latest` to verify a single bump: it also updates unrelated loader, AE2, and
transitive versions. Keep full-graph qualification separate; a focused result
does not prove addon coexistence. Record exact loaded versions, artifact hashes,
source SHA, assertions, screenshot review, and any setup failure.

Before launch, inspect the selected JAR's embedded required dependencies and
compare them with the resolved bundle. Hosting metadata can omit prerequisites.
Verify the bundle contains the requested addon and exact artifact; a cache hit
or successful launch alone is not compatibility evidence. Until the focused
cache identity includes project selection, move only the task-owned cache aside
between selections ([#458](https://github.com/cTux/ae2-crafting-time/issues/458)).
Until array dispatch is fixed, do not pass multiple project IDs through the
PowerShell executable boundary: later IDs can bind to the staging directory
([#466](https://github.com/cTux/ae2-crafting-time/issues/466)). A missing required
graph is a follow-up issue, not permission to change the resolver in this audit.

Promote only after the requested focused smoke succeeds. Keep unsuccessful
candidates out of a ready bump PR. Open research/fix issues and leave PRs
unmerged when the user requests research without merging.

## Preserve old and new API support

Reuse `IntegrationCatalog` and `IntegrationSelection`. Keep old adapters
packaged and put the newest supported API contract first. Select exactly one
matching adapter at startup by target, side, installed mod, and raw class/member
contract; never select solely by lexical version order or load optional classes
early. A newer installed version uses an older adapter only if its API contract
still matches. Never retry another adapter after a runtime failure.

A breaking v1.1 can coexist with v1 support in the same target JAR through
separate adapters. It does not mean loading both dependency JARs together, nor
using one mod JAR across incompatible Minecraft/loader targets. An unknown API
must not receive an incompatible hook. Contract selection alone does not prove
semantic compatibility: a future adapter implementation must independently
verify both released API families and the absent-mod case. During research,
record that work in an issue instead of adding speculative adapters.
