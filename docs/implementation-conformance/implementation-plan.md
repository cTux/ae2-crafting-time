# Conformance documentation correction plan

Lifecycle and acceptance: [specification](spec.md). Evidence: [technical design](technical-design.md).

1. Correct the current reference prose for D1-D10 using the mapped requirements
   and source. Preserve historical feature baselines and their evidence; link
   current configuration/collection rules where later work supersedes them.
2. Review the entire documentation diff against the evidence table. Confirm no
   runtime, resource, configuration, metadata or release file changes (D11).
3. Check local Markdown targets and source references, inspect whitespace with
   `git diff --check`, and record the exact candidate paths and SHA-256 hashes.
   These are static documentation checks, not pre-PR repository test execution.
4. Deliver one documentation commit through the repository hook, then read back
   the canonical PR. Use `Refs #372`; no closing keyword. Review current-head
   comments and required GitHub CI independently of the local static checks.
5. After authorized merge and readback, record the documentation PR/checks in
   the recurrent issue report. Answer Yes with the corrected mismatches, audited
   revision and static-scope limits. Remove only this run's owned claim and leave
   #372 open with `recurrent` intact.

## Verification mapping and feasibility

| Criteria | Check |
| --- | --- |
| D1-D3 | Compare updated sample/estimate/persistence statements with the newer feature specs and both API bridges plus all four SavedData adapters. |
| D4-D5 | Compare request/cache semantics with CpuTtcCache and compare version identifiers with all four StatsNetwork adapters. |
| D6-D8 | Compare native/optional boundaries, current defaults and shared source sets with the cited contracts and code; retain historical tooltip evidence. |
| D9 | Verify the runner and ResourceFixtureClient expose both modes, and keep production/visual acceptance limits explicit. |
| D10-D11 | Compare test ordering with AGENTS and the testing skill; inspect full diff, local links, whitespace, PR review and current-head CI. |

Git, gh and PowerShell are available for this documentation workflow. No new
test infrastructure or Minecraft launch is needed. Guest Java installations,
prepared profiles and VM access were not qualified by this static audit; do not
infer runtime readiness from tracked launcher files. Existing feature campaigns
retain their own prerequisites and completion gates. No local repository tests
were run before PR creation, and this prose-only correction adds no fake tests.
