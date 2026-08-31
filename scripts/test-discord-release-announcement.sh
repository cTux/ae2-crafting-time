#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT

printf '[{}, {}]\n' >"$test_dir/matrix.json"

gh() {
  cat <<'JSON'
{
  "name": "1.0.9",
  "html_url": "https://github.com/cTux/ae2-crafting-time/releases/tag/test",
  "assets": [
    {"name": "forge.jar", "browser_download_url": "https://example/forge.jar"},
    {"name": "fabric.jar", "browser_download_url": "https://example/fabric.jar"},
    {"name": "sources.zip", "browser_download_url": "https://example/sources.zip"}
  ]
}
JSON
}

curl() {
  cat >"$test_dir/payload.json"
}

export DISCORD_WEBHOOK_URL=https://discord.example/webhook
export GH_TOKEN=test-token
export RELEASE_ID=1
export REPOSITORY=cTux/ae2-crafting-time
export MATRIX_PATH="$test_dir/matrix.json"

source "$root/scripts/announce-discord-release.sh"

expected=$'**AE2 Crafting Time 1.0.9**\nhttps://github.com/cTux/ae2-crafting-time/releases/tag/test\n\n**JAR downloads**\n[forge.jar](https://example/forge.jar)\n[fabric.jar](https://example/fabric.jar)'
jq -e --arg expected "$expected" '.content == $expected' "$test_dir/payload.json" >/dev/null

gh() {
  cat <<'JSON'
{"name":"1.0.9","html_url":"https://example/release","assets":[{"name":"forge.jar","browser_download_url":"https://example/forge.jar"}]}
JSON
}
sleep() { :; }
if (source "$root/scripts/announce-discord-release.sh" >/dev/null 2>&1); then
  echo "Partial JAR set should not be announced." >&2
  exit 1
fi

echo "Discord release announcement test passed."
