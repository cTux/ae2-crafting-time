---
name: ae2-crafting-time-writing
description: Write AE2 Crafting Time docs and player-facing text. Use for skills, changelogs, translations, issue forms, metadata, and store descriptions.
---

# AE2 Crafting Time Writing

Write like a technically capable 25-year-old maintainer: casual, direct, and
clear. The age is a voice target, not something to mention in the text.

Treat the GitHub issue list as the source of truth. Find the matching issue
before starting repository writing, or create one when none exists. Remove
secrets, personal data, private paths, and private server details before
creating or updating an issue.
For each issue you create, add at least one `area/<context>` label, exactly one
`priority/<low|medium|high>` label, and exactly one
`effort/<low|medium|high>` label. Reuse an existing label or create the missing
label before opening the issue.

## Voice

- Use normal words, short sentences, and contractions where they sound natural.
- Talk to players as `you`. Use `I` when cTux is speaking about the project.
- Be confident about verified facts and honest about limits or unknowns.
- Skip corporate wording, marketing hype, generic AI phrases, forced slang,
  emojis, and jokes that will age badly.
- Keep technical docs relaxed but precise. Casual never means vague.

## Keep The Facts Exact

- Preserve commands, identifiers, versions, paths, placeholders, links, and
  compatibility boundaries unless the task changes them.
- Keep security, release, data-loss, and testing requirements unambiguous.
- Do not turn an observation into a guarantee or make an optional integration
  sound required.

## Match The Text

- **Docs:** lead with what the reader needs, explain the reason briefly, and use
  headings that sound natural instead of formal report language.
- **Skills:** use concise imperative instructions. Keep every `must`, `never`,
  permission boundary, and stopping condition intact.
- **Changelogs and release copy:** follow the release rules in `AGENTS.md` and
  `docs/release.md`. Include only player-visible features, fixes, and behavior
  changes, and end each GitHub and Discord release-note item with its linked
  source GitHub issue. Leave out implementation details, tests, refactors,
  tooling, and build work. Include one image when reviewed smoke evidence exists
  for a player-visible change; otherwise include none. Choose the highest-effort player-visible
  feature or fix and crop its reviewed smoke-test evidence to the relevant area
  instead of using a full screenshot. Exclude account data, tokens, chat,
  server addresses, coordinates, and unrelated worlds.
- **Translations:** write natural UI text for that locale, keep it short, and
  preserve every placeholder and control name. Update English and Ukrainian
  together.
- **CurseForge, Modrinth, and loader descriptions:** explain the problem the mod
  solves, its useful features, compatibility, dependencies, and unofficial
  status without overselling it.
- **AI disclosure:** be open about AI help and the maintainer's Java experience,
  while making the review, testing, maintainability, scalability, and reuse
  standards clear. Do not imply that unreviewed vibe-coding is acceptable.
- **PR descriptions:** follow the active PR skill. Never paste commit messages,
  diffs, or logs as the explanation.

## Before Finishing

Read the result once as a person, not a parser. Remove stiff phrases such as
`designed to`, `leverages`, `provides a robust`, and `seamless` when plain
language says the same thing. Then verify the file format and, for translations,
matching keys and placeholders.
