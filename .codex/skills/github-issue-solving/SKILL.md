---
name: github-issue-solving
description: Check and fix a GitHub issue in this repository, then open its own pull request. Use when the user asks to investigate or solve an issue.
---

# Fix A GitHub Issue

1. Make the first user-facing response exactly `Checking.`
2. Read the issue and prove the problem against the current code before changing anything.
3. Fetch `origin`, then create a clean `codex/` branch from the latest `origin/master`. Keep unrelated work out of the branch.
4. Find the root cause, make the smallest complete fix, and add or update the narrowest useful regression check.
5. Run the relevant checks and inspect the final diff.
6. Run `scripts/setup-git.ps1` once for the clone, create a conventional commit, and let the post-commit hook push the branch and create its separate pull request.
7. Read the pull request back and report its URL, the fix, and the checks run.
8. Stop with the pull request open. Never merge it; the user reviews and merges it manually.
