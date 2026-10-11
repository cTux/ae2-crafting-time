---
name: prototype
description: Build a throwaway interactive experiment for an AE2 state model or UI idea. Not production code or Minecraft smoke verification.
metadata:
  upstream-repository: https://github.com/mattpocock/skills
  upstream-revision: 49dd158d1076134a641b33efb035946536778336
  upstream-path: skills/engineering/prototype
license: MIT
---

## AE2 project rules

For this Java mod, use a standalone HTML artifact for state or layout exploration;
there is no production web route to modify. Follow UI.md for standalone layout
variants and match actual Minecraft constraints. A prototype is not
client-startup, remapping, or visual-smoke evidence. Keep it outside production
source sets. Use `codex/prototype-<name>` if a preservation branch is needed and
follow the normal hook workflow. Promotion to production requires all normal checks.


# Prototype

A prototype is **throwaway code that answers a question**. The question decides the shape.

## Pick a branch

Identify which question is being answered, using the user's prompt, the surrounding code, or by asking if the user is around:

- **"Does this logic / state model feel right?"** → [LOGIC.md](LOGIC.md). Build a single shareable HTML file (free-play buttons plus tabbed guided walkthroughs) that pushes the state machine through cases that are hard to reason about on paper, and that a non-developer can drive.
- **"What should this look like?"** → [UI.md](UI.md). Generate several radically different UI variations in one standalone HTML artifact, switchable via a URL hash and a floating bottom bar.

The two branches produce very different artifacts, so getting this wrong wastes the whole prototype. If the question is genuinely ambiguous and the user isn't reachable, default to whichever branch better matches the surrounding code (a backend module → logic; a page or component → UI) and state the assumption at the top of the prototype.

## Rules that apply to both

1. **Throwaway from day one, and clearly marked as such.** Save the prototype in the task artifact directory or an explicitly requested location outside production source sets, with an obvious prototype name.
2. **Trivial to run.** Both UI and logic prototypes are single HTML files the user can open directly in a browser.
3. **No persistence by default.** State lives in memory. Persistence is the thing the prototype is _checking_, not something it should depend on. If the question explicitly involves a database, hit a scratch DB or a local file with a clear "PROTOTYPE, wipe me" name.
4. **Skip the polish.** No tests, no error handling beyond what makes the prototype _runnable_, no abstractions. The point is to learn something fast.
5. **Surface the state.** After every action (logic) or on every variant switch (UI), print or render the full relevant state so the user can see what changed.
6. **Capture it when done.** Deliver the standalone prototype and record the question, verdict, and remaining uncertainty. Preserve the artifact at its reported location; use a throwaway branch only if requested. Link it from an authorized issue update when useful. Implement the selected design in production only when the user requests implementation; that is a separate development task with its normal checks.
