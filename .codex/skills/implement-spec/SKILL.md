---
name: implement-spec
description: Implement a multi-ticket AE2 spec in dependency order on one branch. The main agent owns implementation and final review.
metadata:
  upstream-repository: https://github.com/mattpocock/skills
  upstream-revision: 49dd158d1076134a641b33efb035946536778336
  upstream-path: skills/engineering/implement-spec
license: MIT
---

# Implement a Spec

Implement an entire approved spec whose tickets form a dependency graph. Read the
spec, each ticket, and the [implementation workflow](../implement/SKILL.md).
The goal is every acceptance criterion verified on one integration branch.

1. Reconcile the graph with the current code and linked planning documents.
   Preserve implementation holds; an unblocked ticket is not permission to remove
   `backlog`. Resolve missing product decisions before implementation.
2. Reuse a suitable task branch or create a `codex/` branch. Work the ready
   frontier in dependency order. The main agent implements and reviews each
   ticket using `implement`; do not reset, merge, or delete others' worktrees.
3. Keep each fix or feature one conventional commit. Let the existing hook push
   and create/update the PR before running local checks. The main agent reviews
   the complete integration diff, including interactions between tickets.
4. Follow the project PR workflow for the body, evidence, and current-head CI.
   Link the spec and tickets. Report pending checks and unresolved work honestly.
5. Implementation ends with a reviewed PR, not an automatic merge or issue
   closure. Merge, release, and closing implementation issues require their
   applicable authorization and verified completion conditions.

Use independent research or review assistance only when permitted. Implementation
ownership stays with the main agent; this adaptation does not mandate parallel
implementers or extra worktrees.
