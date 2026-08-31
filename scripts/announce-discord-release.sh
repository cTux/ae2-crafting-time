#!/usr/bin/env bash
set -euo pipefail

: "${DISCORD_WEBHOOK_URL:?DISCORD_WEBHOOK_URL is required}"
: "${GH_TOKEN:?GH_TOKEN is required}"
: "${RELEASE_ID:?RELEASE_ID is required}"
: "${REPOSITORY:?REPOSITORY is required}"

matrix_path="${MATRIX_PATH:-scripts/release-matrix.json}"
expected_jars="$(jq length "$matrix_path")"
release_json=
jar_count=0

for attempt in {1..12}; do
  release_json="$(gh api "repos/$REPOSITORY/releases/$RELEASE_ID")"
  jar_count="$(jq '[.assets[] | select(.name | endswith(".jar"))] | length' <<<"$release_json")"
  if ((jar_count == expected_jars)); then
    break
  fi
  sleep 5
done

if ((jar_count != expected_jars)); then
  echo "Expected $expected_jars JARs, found $jar_count; announcement not sent." >&2
  exit 1
fi

release_name="$(jq -r '.name // .tag_name' <<<"$release_json")"
release_url="$(jq -r '.html_url' <<<"$release_json")"
jars="$(jq -r '
  [.assets[]
    | select(.name | endswith(".jar"))
    | "[\(.name)](\(.browser_download_url))"]
  | join("\n")
' <<<"$release_json")"

printf -v content \
  '**AE2 Crafting Time %s**\n%s\n\n**JAR downloads**\n%s' \
  "$release_name" "$release_url" "$jars"
jq -n --arg content "$content" '{content: $content}' |
  curl --fail-with-body \
    -H "Content-Type: application/json" \
    --data-binary @- \
    "$DISCORD_WEBHOOK_URL"
