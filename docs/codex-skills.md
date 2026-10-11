# Codex skills

Project-local skills live in `.codex/skills/`. Prefer their project-adapted
instructions over same-purpose global copies. Use `ask-matt` when you want help
choosing a workflow; it reads the actual skill before recommending a route.

## Upstream and local rules

The imported skills come from [mattpocock/skills](https://github.com/mattpocock/skills)
at commit `49dd158d1076134a641b33efb035946536778336`. Each imported `SKILL.md`
records its upstream path and revision. The MIT notice is retained in
`.codex/skills/LICENSE.mattpocock`; the PR visuals also retain the HumanLayer
notice in `.codex/skills/LICENSE.humanlayer` and their upstream credits.
Review local adaptations before updating; do not overwrite them with a bulk refresh.

All 20 engineering and seven productivity skills are included, plus five portable
experimental skills: chief-of-staff, loop-me, writing-beats, writing-fragments,
and writing-shape. These experiments retain explicit invocation and are adapted
to project scope. The generic TypeScript, Claude-specific, course scaffolding,
and Husky pre-commit skills do not apply to this Java/PowerShell project and are
excluded. No `resolving-merge-conflicts` skill exists at the pinned revision.

`to-spec` replaces `plan-new-feature`, retaining the reviewed GitHub issue, spec,
technical design, implementation plan, and exact issue-text approval rules.
`implement` replaces `implement-planned-feature`, retaining main-agent ownership
of implementation and review. `implement-spec` handles ticket dependencies with
the same ownership. `pr` combines upstream evidence/rollback guidance with the
project's required PR sections and hook-created PR workflow.

The specialized AE2 development, writing, release, test-driver, prepared client
smoke, Prism modpack, and guide-image skills remain. Generic upstream skills
complement them rather than replacing Minecraft-specific rules and checks.

Read [tracker configuration](agents/issue-tracker.md),
[triage mapping](agents/triage-labels.md), and [domain layout](agents/domain.md)
when those workflows apply. `AGENTS.md` remains authoritative for Git, test
ordering, and labels. Upstream role names are prose, not new GitHub labels.

## Discovery and invocation

Imported explicit-only skills retain `policy.allow_implicit_invocation: false`
in `agents/openai.yaml`. `to-spec` and `implement` keep automatic discovery from
the local skills they replace. Common disciplines remain automatically available.
Use `$skill-name` to explicitly invoke an installed skill.

These are repository-owned copies; global skills and plugin caches are unchanged.
New skills become available on the next turn. File validation alone does not
prove that a running chat has refreshed its skill catalog.
