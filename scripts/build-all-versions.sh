#!/usr/bin/env bash
set -euo pipefail

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
