---
name: ask-matt
description: Ask which skill or flow fits your situation. A router over the skills in this repo.
metadata:
  upstream-repository: https://github.com/mattpocock/skills
  upstream-revision: 49dd158d1076134a641b33efb035946536778336
  upstream-path: skills/engineering/ask-matt
license: MIT
---

# Ask Matt

Help choose a skill for the current request. Read the candidate's `SKILL.md`
before recommending it. Recommend the shortest useful route, not the whole suite.

- Unclear feature behavior: `grill-with-docs` for a requested design interview;
  `to-spec` to turn decisions into the project issue and three planning documents.
- A feature too large to plan in one session: `wayfinder`, then `to-spec` when
  its decision map is settled. `to-tickets` splits independently verifiable work.
- Approved feature: `implement`; a whole ticket graph: `implement-spec`.
- Documented bug, port, or internal refactor: `ae2-crafting-time-dev`.
  Use `diagnosing-bugs` for difficult diagnosis and `tdd` for requested test-first work.
- Review a diff: `code-review`. Find refactoring candidates:
  `improve-codebase-architecture`. Design a chosen interface: `codebase-design`.
- Inspect incoming issues: `triage`. Prepare/update the hook-created PR: `pr`.
- Research a fact: `research`. Test a design idea: `prototype`. Clarify terms:
  `domain-modeling`. Explain the last answer: `wait-what`.
- Improve agent instructions: `writing-for-agents`; player docs and translations:
  `ae2-crafting-time-writing`. Reflect on a completed session: `retro`.
- Release/build distributions: `ae2-crafting-time-release`. Prepared client QA:
  `run-ae2-client-smoke`. Named modpack QA: `launch-prism-test-modpack`.
- Portable context: `handoff`; interview without files: `grill-me`; questions
  for another person: `to-questionnaire`; structured learning: `teach`.
- Human-only setup steps: `wizard`. Inspect this suite's configuration:
  `setup-matt-pocock-skills`. Long-running coordination: `chief-of-staff`.
- Design a recurring workflow: `loop-me`. Explore article material:
  `writing-fragments`; assemble it incrementally: `writing-beats` or `writing-shape`.

Stay in the current conversation unless the user requests a different chat.
Use [PHASE-BOUNDARIES.md](PHASE-BOUNDARIES.md) only when context management is
actually needed; handoffs and fresh sessions are not prerequisites for features.
