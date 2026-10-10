---
name: pr
description: Prepare or update this project's hook-created GitHub PR with evidence and required sections. Not implementation, review, or merge.
metadata:
  upstream-repository: https://github.com/mattpocock/skills
  upstream-revision: 49dd158d1076134a641b33efb035946536778336
  upstream-path: skills/engineering/pr
  credits:
    skill: show-me
    author: Dex Horthy
    organisation: Humanlayer
    url: "https://github.com/humanlayer/skills/blob/main/plugins/show-me/skills/show-me/SKILL.md"
license: MIT
---

# PR

Produce one validated PR title, one evidence-based body, and—unless the user asks only for a draft—the created PR URL. Use repository evidence and judgment; never invent intent, test results, issue links, or skill usage.

## Inspect

1. Read repository instructions and PR templates.
2. Determine base and head branches. Review the full `base...head` commits, diff, and changed files.
3. Inspect working-tree changes. Exclude unrelated or uncommitted work and mention material leftovers.
4. Check whether the head already has a PR. Record and reuse it, then continue with the requested body preparation and verification instead of creating a duplicate.
5. Gather issue context, breaking changes, rollout risks, verification evidence, and implementation skills from the task record. Treat unavailable facts as unknown.

## Title

Discover title policy from repository instructions, PR-title workflows, commitlint configuration, and package scripts. Apply the repository's exact allowed types, scopes, casing, and length. If none exists, use `type(optional-scope): imperative summary` with no trailing period.

Describe the whole PR. Validate the exact title with the repository-provided command when available; do not install a validator solely for this step. Revise until it passes.

## Body

Use these required sections and preserve any additional mandatory sections or checkboxes from the repository's PR template. Write every section as plain human-readable text a curious 10-year-old could follow: short sentences, everyday words, and unfamiliar terms explained the first time they appear. Never paste commit messages, diffs, or bullet logs as the body; those are source material, not the explanation.

```markdown
## What?

Explain the resulting behavior and the material technical decisions in plain words. Note compatibility, migration, rollout, breaking changes, and non-goals when relevant.

## Why?

State the problem or need and the consequence of leaving it unresolved, in plain words a non-developer can follow.

## Where?

- `path or component`: Explain its role in the solution and important integration boundaries in plain words.

## Verification

- `command or check`: Passed — concise observed result.
- Not run: Reason.

<!-- Insert any other required template sections here. -->

## Skills used

- `$skill-name`: Explain how it helped implement the changes.
```

Make Why, What, and Where detailed enough to review without reconstructing the diff. Consolidate repetitive paths. Keep `## Skills used` last, with nothing after it. List only skills that actually helped implement the changes—not available, inspected, PR-authoring, or evaluation skills. If none can be established, use `- None recorded.`

Use the smallest [visual](VISUALS.md) only when it clarifies a complex change.
Put observed before/after evidence in Verification, and explain rollback or
hard-to-reverse effects in What when material. Never fabricate a pre-change test
run; report skipped checks and pending CI separately. Keep Skills used last.

## Create and verify

1. If the user requested only preparation, return the title and body without pushing or creating.
   Otherwise let the configured post-commit hook create the PR; edit and read back
   that PR instead of creating a duplicate. Local checks follow PR creation.
2. If the hook-created PR exists, update its title/body with a body file and
   continue directly to verification. Preserve its base/head and draft state
   unless the user requested a change.
3. Only if no PR exists after checking the hook result, push the head if needed
   and create the PR with the selected base/head and a body file. Honor an
   explicit draft/ready choice; otherwise use ready for completed, verified work.
   Never force-push unless explicitly authorized.
4. Read the PR back from GitHub. Verify title, base/head, draft state, required sections, and that `Skills used` is last.
5. Return the PR URL, title, draft state, verification summary, pending checks, and material uncommitted leftovers.
