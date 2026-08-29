---
name: ae2-crafting-time-pr
description: Open, rebase, or recover a pull request for AE2 Crafting Time without dragging in unrelated commits. Use when creating a PR, branching for a change, fixing a wrong-base PR, writing a commit message, or staging files.
---

# AE2 Crafting Time Pull Request Workflow

The repo's `scripts/after-commit.ps1` hook auto-pushes and opens a PR, but it
bases the PR on `origin/master` and builds the body from `origin/master..HEAD`.
These rules keep that range clean so the PR only contains the intended work.

## Branch from master, not a feature branch

- Before the first commit, make sure the branch forks from `origin/master`:
  `git fetch origin master` then branch from `origin/master`.
- Never fork from another in-flight feature branch (e.g. `feat/linux-scripts`).
  The hook would then include that branch's commits in `origin/master..HEAD` and in
  the PR description, making the PR dirty and confusing reviewers.

## Recover from a wrong-base PR

1. `gh pr close <n>` the bad PR. A closed PR on the same branch name later blocks
   the hook's auto-create (see step 6).
2. `git push origin --delete <branch>` to drop the remote branch.
3. `git fetch origin master` and `git reset --hard origin/master` on the branch.
4. `git cherry-pick <intended-commit>` to reapply only the wanted commits.
5. `git push --set-upstream origin <branch>` (fresh remote branch).
6. The hook calls `gh pr view <branch>`; a closed PR for that branch name makes it
   think the PR "already open", so it **skips** creation. Create it manually:
   `gh pr create --base master --head <branch> --title "..." --body "..."`.

## Commit messages

- Do not use `git commit -m` with `*` or unescaped quotes — the shell mangles them
  (you get `pathspec ... did not match any file`). Write the message to a temp file
  and run `git commit -F <file>`.

## Staging

- Always run `git status` before `git add`. Research and extraction steps can drop
  files into the working tree (e.g. an `appeng/` sources extraction from a jar).
- Stage only the intended paths (e.g. `git add docs/<feature>/`), never `.` blindly.

## Docs and implementation are separate PRs

- Spec/planning docs live in their own PR. The code implementation is a later,
  separate PR. Do not bundle them.

## Spec doc layout (research-style tasks)

When a task is "research then write a spec", mirror the existing `docs/` style and
split into a small set under `docs/<feature>/`:

- `index.md` — goal, scope, key decisions.
- `data-model.md` — keys, persistence/NBT, packet codec, migration.
- `collection.md` — where the data comes from and how it is wired.
- `estimation.md` — user-facing behavior.
- `implementation-plan.md` — ordered tasks with file map and tests.

Load `ae2-crafting-time-writing` first and keep the casual, direct, technically
precise voice. Lead with what the reader needs; use natural headings.

## One-line reminder

`scripts/after-commit.ps1` -> push branch, then `gh pr create --base master`. Its
range `origin/master..HEAD` must stay clean. Run `scripts/setup-git.ps1` once per
clone so the hook is installed (it sets `core.hooksPath` to `.githooks`).
