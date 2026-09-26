# JECT release discovery relation

Status: draft

Scope: [#440](https://github.com/cTux/ae2-crafting-time/issues/440).

Hold: `do-not-implement-yet` remains in force. Resolve the current matching
artifact targets before implementing the release relation.

## Requested behavior

List Just Enough Crafting Tree (JECT) as a CurseForge `optionalDependency`
only for releases with a matching Minecraft/loader artifact. JECT is described
by the issue as a client-side crafting-tree viewer. This relation helps players
discover it; it does not mean Crafting Time requires, bundles, or integrates it.

Preserve AE2, GuideME, and existing relations. This issue explicitly excludes
loader metadata, runtime hooks, development-client pins, and compatibility claims.

## Implementation boundary

Use the existing relation construction in `scripts/deploy-changed.ps1` and
`scripts/deploy-changed.sh`, following the release-matrix target identities.
Verify the official JECT project/slug and exact supported artifacts first.
Both publishing paths must emit the same relationship for the same target.

Update [release guidance](../release.md) to explain discovery versus required
dependency or tested integration. Do not infer support for a target from a
similarly named Minecraft version or another loader's file.

## Acceptance and completion

- Dry-run and existing release-script tests cover an applicable target and a
  target without a matching JECT artifact.
- PowerShell and shell emit the same optional relation where applicable.
- Unsupported targets omit it; unrelated relations are unchanged.
- Release documentation describes the limited discovery meaning.

Retain exact artifact evidence, dry-run output, test results, source commit,
and current-head CI. Follow the release workflow for any later publication;
this documentation change neither uploads a release nor proves runtime
compatibility.
