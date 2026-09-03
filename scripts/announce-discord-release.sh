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

payloads="$(python3 -c '
import json, sys

release = json.load(sys.stdin)
name = release.get("name") or release["tag_name"]
content = "**AE2 Crafting Time " + name + "**\n" + release["html_url"] + "\n\n"
body = release.get("body") or ""
if body:
    content += body + "\n\n"
content += "**JAR downloads**\n" + "\n".join(
    "[" + asset["name"] + "](" + asset["browser_download_url"] + ")"
    for asset in release["assets"] if asset["name"].endswith(".jar")
)
while content:
    end, units = 0, 0
    for char in content:
        units += 2 if ord(char) > 0xffff else 1
        if units > 2000:
            break
        end += 1
    if end < len(content):
        for separator in ("\n\n", "\n"):
            boundary = content.rfind(separator, 0, end)
            if boundary >= 0:
                end = boundary + len(separator)
                break
    print(json.dumps({"content": content[:end], "allowed_mentions": {"parse": []}}))
    content = content[end:]
' <<<"$release_json")"

separator='?'
[[ "$DISCORD_WEBHOOK_URL" == *\?* ]] && separator='&'
part=0
while IFS= read -r payload; do
  part=$((part + 1))
  if ! response="$(curl --silent --show-error --fail-with-body \
    -H "Content-Type: application/json" \
    --data-binary "$payload" \
    "${DISCORD_WEBHOOK_URL}${separator}wait=true")"; then
    printf '%s\n' "$response" >&2
    echo "Discord part $part failed; inspect earlier confirmed message IDs before retrying." >&2
    exit 1
  fi
  if ! message_id="$(jq -er '.id | strings | select(test("^[0-9]+$"))' <<<"$response")"; then
    echo "Discord part $part has no confirmed message ID; inspect delivery before retrying." >&2
    exit 1
  fi
  echo "Discord part $part confirmed: message $message_id"
done <<<"$payloads"
echo "Discord announcement complete: $part part(s)."
