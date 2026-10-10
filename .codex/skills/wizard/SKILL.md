---
name: wizard
description: Create a guided setup script for steps only a human can perform. Not steps the agent can execute directly.
metadata:
  upstream-repository: https://github.com/mattpocock/skills
  upstream-revision: 49dd158d1076134a641b33efb035946536778336
  upstream-path: skills/engineering/wizard
license: MIT
---

## AE2 project rules

On Windows verify Git Bash is available before using template.sh. Otherwise
write the equivalent focused PowerShell procedure using native APIs. Inspect
configuration names without reading existing secret values. Mask secret input,
exclude generated secret files from Git, and keep credentials out of logs and
chat. Authoring a wizard does not authorize running irreversible steps or writing
CI secrets. Set `WIZARD_REPO` to the explicitly intended `OWNER/REPO` before
GitHub writes; never derive it from the caller's directory. The Bash dotenv writer
requires verified mode 0600 and rejects symlinks. On Windows or filesystems
without POSIX permissions, use a native PowerShell procedure that establishes
and verifies an owner-only ACL before storing credentials. Preserve existing
release approval and Git hook workflows.


# Wizard

A **wizard** is a bash script that walks a human, step by step, through a manual procedure that's tedious to do by hand and tedious to re-explain to an AI every time. It opens each URL, says exactly what to click and copy, captures the values, writes them where they belong (`.env`, GitHub secrets), confirms at every stage, and shows how many stages are left. It might configure third-party services, run a one-off migration, or move the project from one state to another.

The delightful UX is already solved by [template.sh](template.sh): stage-by-stage progress, confirmation gates, cross-platform URL opening (including WSL), hidden secret entry, idempotent `.env` upserts, `gh secret`/`gh variable` writes, and a closing summary. **Your job is only to scope the procedure and author its stages.** The library above the `STAGES` marker is identical in every wizard; that consistency is the point: never hand-edit it.

A wizard is ephemeral by default: built for one run, saved to a scratch or `scripts/` path, deleted when the job's done. Commit it only when the user wants a repeatable setup path that should live in the repo.

## Process

### 1. Scope the procedure

Work out every manual step the human must take and every value that gets captured along the way. Read the repo first, don't ask cold:

- For setup: `.env`, `.env.example`, `.env.*`, `README`, `docker-compose*`, framework config, and `.github/workflows/*` (every `secrets.*` / `vars.*` reference is a value the wizard must produce).
- For a migration or transition: the current state, the target state, and the irreversible actions between them.

The template accepts single-line values only. Exclude multiline credentials, including
PEM keys and formatted JSON, from its prompts and dotenv persistence. For those
values, author a separate file-based procedure using the destination tool's file
input; never paste them into `ask_secret` or flatten them into one line.

Then show the user the ordered list of stages and the values each produces, and confirm: they may add, drop, or reorder.

**Done when:** every stage is named in order, and for each captured value you know (a) where the human gets it, (b) where it's written (`.env`, a GitHub secret, both, or nowhere; some stages are pure actions), and (c) whether it's secret (hidden entry) or public.

### 2. Map each stage's journey

For each stage, write the precise path a human follows: which URL to open, what to do there, where a value is shown, which variable it fills: e.g. "Dashboard → Developers → API keys → Reveal test key → copy". Where you don't actually know the current UI or the exact command, say so and ask the user or check the docs: never invent steps that may not exist.

**Done when:** every stage traces to concrete instructions a stranger could follow.

### 3. Author the wizard

Copy `template.sh` to the target path. Replace the example stage with one `stage` per step, in dependency order. Use the library helpers: `stage`, `say`/`step`, `open_url`, `ask`/`ask_secret`, `write_env`, `set_secret`/`set_var`, `pause`/`confirm`. Set `TOTAL_STAGES` to the number of stages you wrote.

Hold the bar the template sets: open the URL before asking for its value, use `ask_secret` for anything secret, `write_env` every persisted value, `set_secret` only the values CI actually needs, and `confirm` before any irreversible action. Each `stage` clears the screen so only the current step is visible: keep a stage to one focused task so nothing the human needs scrolls away. Don't touch the library above the marker.

### 4. Verify and hand off

Set executable permissions with `chmod +x <script>` before committing.
For repository-owned changes, commit first and wait for the post-commit hook to
create or update the PR before running any local verification below. Link a
repeatable setup script from the README in that same commit. Scratch-only
procedures remain task artifacts.

- `bash -n <script>`; run `shellcheck` if available.
- After changing the template library, run `python test-template.py /path/to/bash`
  from this skill directory. It uses only temporary dummy values and no external services.
- Don't run it end-to-end yourself: it opens browsers and blocks on human input. Trace it statically instead: every value from step 1 is captured and lands where step 1 said, and every `set_secret` name exactly matches a `secrets.*` reference in CI.
- Tell the user how to run it. The default `.env` belongs to the Git project
  containing the script. For a scratch script outside that project, supply an
  explicit absolute `ENV_FILE` path to the intended project's ignored file.
