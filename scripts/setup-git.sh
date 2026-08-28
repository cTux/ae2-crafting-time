#!/usr/bin/env bash
set -euo pipefail

command -v git >/dev/null 2>&1 || {
  echo "Required command 'git' not found. Install it and retry." >&2
  exit 1
}

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$root" config core.hooksPath .githooks
git -C "$root" config push.autoSetupRemote true
echo "Automatic push and pull-request creation after every commit are enabled."
