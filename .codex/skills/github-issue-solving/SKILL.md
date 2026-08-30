---
name: github-issue-solving
description: Check and fix a GitHub issue in this repository, then open its own pull request. Use when the user asks to investigate or solve an issue.
---

# Fix A GitHub Issue

1. Read the issue and prove the problem against the current code before changing
   anything.
2. Follow `AGENTS.md` for the clean branch, commit, hook-created PR, and
   validation order. Keep unrelated work out of the branch.
3. Fix the shared root cause and add the narrowest useful regression check.
4. Read the pull request back and report its URL, the fix, and checks that
   actually ran.
5. Stop with the pull request open. Never merge it; the user reviews and merges
   it manually.
