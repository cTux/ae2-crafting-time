# NeoEco renderer quarantine implementation plan

See the [canonical status and criteria](spec.md) and [source evidence](technical-design.md).

## Delivery order

1. Merge these documents and align #627's follow-up with the source-established
   cause and compatible quarantine. Preserve the original observed-failure report
   and historical reproduction comment. Link the existing upstream fix; do not post upstream.
2. On the documentation base, apply the target-specific matrix exclusion, remove
   the ordinary NeoEco CPU case, and register its explicit focused diagnostic case.
   Extend existing coverage membership validation as described in the design.
   Keep the direct-behavior declaration and other targets intact.
3. Add small regressions to the existing script self-checks. Update
   `docs/dependencies.md` with the exclusion, computed suite count and diagnostic
   limitation. Move the spec to `in-progress` with the implementation PR link.
4. Create the implementation PR through the conventional commit hook before
   executing tests. Run the mapped script checks and inspect their actual results.
   Report GitHub CI separately, bound to the same current head.
5. Restore the existing VM control prerequisites, then run the complete remaining
   compatible catalogue. Preserve the source and dependency inputs during the run.
   Resolve any related blocker through the normal reviewed change path; a failed
   or incomplete catalogue does not satisfy acceptance.
6. Review all required visual evidence, archive the results, and record the exact
   tested revision and timing. Complete current-head review/CI and merge gates.
   Mark the scope finished only after delivery and required evidence are verified.

## Criterion-to-change and check map

| Criterion | Change | Required evidence |
|---|---|---|
| NE627-1 | 1.21.1 project exclusion in `run-client-versions.json` | `test-run-client.ps1`: compatible selection omits NeoEco, explicit compatible request rejects it, latest retains it; runtime dependency manifest confirms absence |
| NE627-2 | Suite removal and existing focused-case registration | `test-ui-smoke-plan.ps1`: complete compatible plan has no NeoEco case or automatic NeoEco graph; explicit latest project/leaf plan remains addressable |
| NE627-3 | Coverage membership accepts valid explicit focused cases | `test-ui-smoke-coverage.ps1`: compatible exclusion, latest unqualified direct fixture, invalid/stale focused names rejected, and existing target matrix checks pass |
| NE627-4 | Full remaining catalogue execution | Complete selection, every selected result, clean exit, hashes, archived English screenshots/sidecars, visual gate/review and phase timing |
| NE627-5 | Dependency inventory and final evidence/status | Reviewed diff preserves production support and historical failures; exact upstream fix linked; actual suite count and remaining latest limitation recorded |

Run each applicable script self-check after PR creation. Cover every changed
executable branch without weakening existing validation. Static JSON and document
changes need consistency checks, not artificial unit coverage.

## Runtime gate

After the prepared guest and existing control path are ready, use:

```powershell
./scripts/run-ui-smoke-matrix.ps1 -Target 1.21.1-neoforge
```

Review the selection before launch: this is the entire remaining compatible
catalogue, not `-BaseOnly`, a single leaf or a substituted latest graph. Use the
loader-specific 21.1.251 manifest and Java 21, 8 GiB client memory and a maximized
English window through the established CodexVM workflow. Build only on the host.

Keep `selection.json`, the dependency and artifact identities, per-case results,
client logs, exact process cleanup evidence, archive report and `gate.json`.
Inspect each `REVIEW_REQUIRED` checkpoint and any mismatch; a missing visual
baseline is not a visual pass. Report measured phase times under the existing
[evidence contract](../ui-smoke-evidence.md).

This gate does not require rerunning the unsafe historical NeoEco version or
qualifying a newer API. Its excluded coverage row must stay
`EXCLUDED` / `NOT_APPLICABLE`; the new campaign does not turn old failures into passes.

## Completion conditions

All five criteria, relevant self-checks, required current-head CI/review and the
complete runtime gate must pass. Final evidence must distinguish the compatible
quarantine from an upstream renderer fix or #457's future adapter qualification.
Keep any runtime prerequisite or failed case explicit until resolved.
