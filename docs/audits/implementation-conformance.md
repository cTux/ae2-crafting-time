# Audit implementation against documentation

Tracking: [#372](https://github.com/cTux/ae2-crafting-time/issues/372).

Each run asks whether code, configuration, automation, or workflows depart from
repository documentation. This is a recurring audit procedure, not a claim that
the current implementation has been checked.

## Establish authority and scope

Record the exact audited default-branch revision. Read applicable contributor
instructions, specifications, technical designs, implementation plans, testing
guidance, and release guidance. Use the [documentation lifecycle](../documentation-status.md)
to distinguish shipped requirements from drafts, paused work, and future additions.

Map each authoritative requirement to its actual code/configuration or workflow
entry point and existing checks. Include shared behavior and loader/version
boundaries. A proposal that has not been implemented is not automatically a
regression in shipped behavior.

## Investigate mismatches

Trace actual behavior rather than relying on file names or a stale plan. For
each mismatch, retain the requirement/source references, observed behavior,
impact, and the smallest reproduction or check.

When implementation contradicts authoritative requirements, fix the implementation.
When documentation is missing, stale, or ambiguous, reconcile it from repository
evidence and established requirements before using it as an authority. Do not
rewrite requirements merely to bless a defect, or invent a new product decision
under the name of an audit.

Follow the applicable development/writing skills and commit/PR ordering. Run
checks and review required by the actual change, report CI separately, and mark
unrun runtime boundaries explicitly. Make no change when no mismatch exists.

## Per-run report

After fixes merge, comment on #372 with Yes or No to the opening question,
the audited revision/scope, every mismatch and resolution, and links to relevant
documentation, implementation PRs, checks, and CI. If nothing was found, state
that result and provide the evidence used.

Use `Refs #372`, never a closing keyword. Remove only the current run's claim
and leave the recurring issue open. Adding this procedure does not execute the
audit or establish that implementation matches every document.
