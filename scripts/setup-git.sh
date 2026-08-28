#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$root" config core.hooksPath .githooks
git -C "$root" config push.autoSetupRemote true
echo "Automatic push and pull-request creation after every commit are enabled."
