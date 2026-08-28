#!/usr/bin/env bash
set -euo pipefail

for c in git gh; do
  command -v "$c" >/dev/null 2>&1 || {
    echo "Required command '$c' not found. Install it (e.g. 'sudo apt-get install $c') and retry." >&2
    exit 1
  }
done

root="$(git rev-parse --show-toplevel)"
cd "$root"

branch="$(git symbolic-ref --quiet --short HEAD || true)"
if [ -z "$branch" ]; then
  echo "Automatic push skipped: detached HEAD." >&2
  exit 0
fi

base="${CODEX_PR_BASE:-master}"
dry_run="${CODEX_HOOK_DRY_RUN:-0}"

if [ "$dry_run" != "1" ]; then
  git push --set-upstream origin "$branch"
fi

if [ "$branch" = "$base" ]; then
  exit 0
fi

if [ "$dry_run" != "1" ]; then
  if ! command -v gh >/dev/null 2>&1; then
    echo "Automatic PR failed: GitHub CLI is not installed" >&2
    exit 1
  fi
  set +e
  url="$(gh pr view "$branch" --json url --jq .url 2>/dev/null)"
  pr_found=$?
  set -e
  if [ $pr_found -eq 0 ] && [ -n "$url" ]; then
    echo "PR already open: $url"
    exit 0
  fi
fi

title="$(git log --reverse --format=%s "origin/$base..HEAD" | head -n 1)"
commits="$(git log --reverse --format='- %s (%h)' "origin/$base..HEAD")"
files="$(git diff --name-only "origin/$base...HEAD" | sed 's/.*/- `&`: Changed by this branch./')"

body="## Why?

Keep changes on \`$branch\` reviewable and ready to merge without a manual PR step.

## What?

$commits

## Where?

$files

## Verification

- Not run by the automatic PR hook; rely on commit-specific local checks and GitHub checks.

## Skills used

- None recorded."

if [ "$dry_run" = "1" ]; then
  echo "dry-run PR title: $title"
  echo "$body"
  exit 0
fi

body_path="$(mktemp)"
printf '%s\n' "$body" > "$body_path"
gh pr create --base "$base" --head "$branch" --title "$title" --body-file "$body_path"
rm -f "$body_path"
