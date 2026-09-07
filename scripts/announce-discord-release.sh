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

cf_dir="$(mktemp -d)"
trap 'rm -rf "$cf_dir"' EXIT
while IFS= read -r pid; do
  [[ -z "$pid" ]] && continue
  if ! cf_resp="$(curl --silent --show-error --fail-with-body "https://api.cfwidget.com/minecraft/mc-mods/$pid")"; then
    echo "CurseForge lookup failed for project $pid; continuing without exact CF links." >&2
    continue
  fi
  printf '%s' "$cf_resp" >"$cf_dir/$pid.json"
done < <(jq -r '.[].curseProjectId // empty' "$matrix_path" | sort -u)

export MATRIX_PATH="$matrix_path"
export CF_DIR="$cf_dir"

payloads="$(python3 -c '
import json, os, re, sys

release = json.load(sys.stdin)
matrix_path = os.environ.get("MATRIX_PATH", "scripts/release-matrix.json")
cf_dir = os.environ.get("CF_DIR", "")
try:
    with open(matrix_path, encoding="utf-8") as fh:
        matrix = json.load(fh)
except (OSError, ValueError):
    matrix = []
if not isinstance(matrix, list):
    matrix = []

cf_files, cf_projects = {}, {}
if cf_dir and os.path.isdir(cf_dir):
    for fn in os.listdir(cf_dir):
        try:
            with open(os.path.join(cf_dir, fn), encoding="utf-8") as fh:
                data = json.load(fh)
        except (OSError, ValueError):
            continue
        for item in data.get("files", []) or []:
            file_name, url = item.get("name"), item.get("url")
            if file_name and url:
                cf_files[file_name] = url
        proj = (data.get("urls") or {}).get("curseforge")
        pid, _ = os.path.splitext(fn)
        if proj and pid:
            cf_projects[pid] = proj.rstrip("/") + "/files/all"

def match_entry(asset_name):
    for entry in matrix:
        if not isinstance(entry, dict):
            continue
        mod = entry.get("modName")
        loader = entry.get("loader")
        mc = entry.get("minecraftVersion")
        if not (mod and loader and mc):
            continue
        prefix, suffix = mod + "-", "-" + loader + "-" + mc + ".jar"
        if asset_name.startswith(prefix) and asset_name.endswith(suffix):
            version = asset_name[len(prefix):-len(suffix)]
            parts = version.split(".")
            if len(parts) == 3 and all(part.isdigit() for part in parts):
                return entry, version, loader, mc
    return None, None, None, None

rows = []
for asset in release["assets"]:
    asset_name = asset.get("name", "")
    if not asset_name.endswith(".jar"):
        continue
    row = "[" + asset_name + "](" + asset.get("browser_download_url", "") + ")"
    entry, version, loader, mc = match_entry(asset_name)
    cf_url, mr_url = None, None
    if entry is None:
        print("warning: no release-matrix entry matches JAR " + asset_name + "; posting GitHub link only", file=sys.stderr)
    else:
        mr_id = entry.get("modrinthProjectId")
        if mr_id and version and loader and mc:
            mr_url = "https://modrinth.com/mod/" + str(mr_id) + "/version/" + version + "-" + loader + "-" + mc
        cf_url = cf_files.get(asset_name)
        if cf_url is None:
            pid = entry.get("curseProjectId")
            if pid is not None:
                cf_url = cf_projects.get(str(pid))
    links = []
    if cf_url:
        links.append("[CF](" + cf_url + ")")
    if mr_url:
        links.append("[MR](" + mr_url + ")")
    if links:
        row += " (" + ", ".join(links) + ")"
    rows.append(row)

name = release.get("name") or release["tag_name"]
content = "**AE2 Crafting Time " + name + "**\n" + release["html_url"] + "\n\n"
body = release.get("body") or ""
if body:
    content += body + "\n\n"
content += "**JAR downloads**\n" + "\n".join(rows)
image_line = re.compile(r"(?m)^!\[[^\]\r\n]*\]\(https?://[^\s)\r\n]+\)[ \t]*(?:\r?\n|$)")
segments, start = [], 0
for match in image_line.finditer(content):
    segments.append((content[start:match.start()], 4))
    segments.append((match.group(0), 0))
    start = match.end()
segments.append((content[start:], 4))

for segment, flags in segments:
    while segment:
        end, units = 0, 0
        for char in segment:
            units += 2 if ord(char) > 0xffff else 1
            if units > 2000:
                break
            end += 1
        if end < len(segment):
            for separator in ("\n\n", "\n"):
                boundary = segment.rfind(separator, 0, end)
                if boundary >= 0:
                    end = boundary + len(separator)
                    break
        print(json.dumps({"content": segment[:end], "allowed_mentions": {"parse": []}, "flags": flags}))
        segment = segment[end:]
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
