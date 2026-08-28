#!/usr/bin/env bash
set -euo pipefail

for c in jq; do
  command -v "$c" >/dev/null 2>&1 || {
    echo "Required command '$c' not found. Install it (e.g. 'sudo apt-get install $c') and retry." >&2
    exit 1
  }
done

java_home="${1:-}"
matrix_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/release-matrix.json"
if [ $# -ge 2 ]; then
  matrix_path="$2"
fi

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -n "$java_home" ] && [ -d "$java_home" ]; then
  export JAVA_HOME="$java_home"
  export PATH="$java_home/bin:$PATH"
fi

tasks="$(jq -r '.[] | ":\(.module):distMod"' "$matrix_path" | tr '\n' ' ')"
if [ -z "${tasks// }" ]; then
  echo "No release entries found in $matrix_path" >&2
  exit 1
fi

dist="$root/dist"
mkdir -p "$dist"
rm -f "$dist"/*.jar

gradlew="$root/gradlew"
cd "$root"
"$gradlew" $tasks
