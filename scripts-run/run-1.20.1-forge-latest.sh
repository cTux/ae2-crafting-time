#!/usr/bin/env bash
set -euo pipefail
dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$dir/../scripts/run-client.sh" -Target 1.20.1-forge -Latest "$@"
