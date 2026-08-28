#!/usr/bin/env bash
set -euo pipefail
dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$dir/../scripts/run-client.sh" -Target 26.1.2-neoforge "$@"
