# Audit supported builds and repository warnings

Tracking: [#374](https://github.com/cTux/ae2-crafting-time/issues/374).

Each run asks whether the current default branch fails a supported build or
emits a repository-owned warning, including deprecated APIs in source/build
logic. This page defines the procedure; it does not claim clean builds.

## Establish the run

Use a clean checkout and record the exact default-branch commit. Enumerate
every row from `scripts/release-matrix.json`, then follow the existing
[build workflow](../building.md) with complete Gradle warning output where
applicable. Keep logs per target and record relevant toolchain/dependency identities.

Follow repository PR/check ordering for any resulting changes. A successful
build of one target does not cover the matrix.

## Attribute and fix

Treat every build error as actionable. Trace its shared cause and fix all
affected targets without skipping a row, weakening tests, or hiding failure.

Classify each warning as repository source/build logic, generated code,
toolchain, or third-party dependency. Fix repository-owned warnings and record
proven external ownership separately; an external warning has not been fixed
just because its owner is known.

For deprecated APIs, verify the supported replacement against the relevant
upstream API/version. Reuse the existing compatibility boundary when targets
differ. A narrow workaround or suppression is acceptable only if no supported
replacement exists: record the limitation, exact scope, versions, and removal
condition. Never disable warning categories globally.

Run checks required by each change and rebuild every affected matrix row.
Review final logs, and update contributor/compatibility guidance if the supported
procedure or API boundary changed. Make no code change when no problem exists.

## Per-run report

After fixes merge, comment on #374 with Yes or No to the opening question,
the audited commit and every matrix row, each error/warning and attribution,
resolution or external owner, and links to PRs, checks, build logs, and CI.
For a clean run, state no fix was needed and provide clean-build evidence.

Use `Refs #374`, never a closing keyword. Remove only the current run's claim
and leave the recurring issue open. Merely documenting this procedure is not
an execution or closure of the audit.
