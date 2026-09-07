#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT

cat >"$test_dir/matrix.json" <<'JSON'
[
  {"id": "test-forge", "modName": "ae2-crafting-time", "loader": "forge", "minecraftVersion": "1.20.1", "modrinthProjectId": "MR123", "curseProjectId": "CF123"},
  {"id": "test-fabric", "modName": "ae2-crafting-time", "loader": "fabric", "minecraftVersion": "1.20.1", "modrinthProjectId": "MR123", "curseProjectId": "CF123"}
]
JSON
cat >"$test_dir/cfwidget.json" <<'JSON'
{"urls": {"curseforge": "https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time"},
 "files": [
   {"name": "ae2-crafting-time-1.1.1-forge-1.20.1.jar", "url": "https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/111"},
   {"name": "ae2-crafting-time-1.1.1-fabric-1.20.1.jar", "url": "https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/222"}
 ]}
JSON
gh() { cat "$test_dir/release.json"; }
sleep() { :; }
curl() {
  local payload='' url=''
  while (($#)); do
    case "$1" in
      --data-binary) payload="$2"; shift ;;
      https://*) url="$1" ;;
    esac
    shift
  done
  if [[ "$url" == *'api.cfwidget.com'* ]]; then
    cat "$test_dir/cfwidget.json"
    return 0
  fi
  [[ "$url" == *'wait=true' ]] || return 1
  printf '%s\n' "$payload" >>"$test_dir/payloads.jsonl"
  local count
  count="$(wc -l <"$test_dir/payloads.jsonl")"
  if [[ "$count" -eq "${fail_at:-0}" ]]; then
    echo '{"message":"test failure"}'
    return 22
  fi
  if [[ "$count" -eq "${unconfirmed_at:-0}" ]]; then
    echo '{}'
  else
    printf '{"id":"%s"}\n' "$count"
  fi
}

export DISCORD_WEBHOOK_URL=https://discord.example/webhook
export GH_TOKEN=test-token
export RELEASE_ID=1
export REPOSITORY=cTux/ae2-crafting-time
export MATRIX_PATH="$test_dir/matrix.json"

for case_name in short multiline image empty null exact over unicode long; do
  python3 - "$test_dir" "$case_name" <<'PY'
import json, pathlib, sys
path, case = pathlib.Path(sys.argv[1]), sys.argv[2]
release = {
    "name": "1.1.1", "html_url": "https://example/release",
    "assets": [
        {"name": "ae2-crafting-time-1.1.1-forge-1.20.1.jar", "browser_download_url": "https://example/forge.jar"},
        {"name": "ae2-crafting-time-1.1.1-fabric-1.20.1.jar", "browser_download_url": "https://example/fabric.jar"},
        {"name": "sources.zip", "browser_download_url": "https://example/sources.zip"},
    ],
}
prefix = "**AE2 Crafting Time 1.1.1**\nhttps://example/release\n\n"
forge_row = "[ae2-crafting-time-1.1.1-forge-1.20.1.jar](https://example/forge.jar) ([CF](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/111), [MR](https://modrinth.com/mod/MR123/version/1.1.1-forge-1.20.1))"
fabric_row = "[ae2-crafting-time-1.1.1-fabric-1.20.1.jar](https://example/fabric.jar) ([CF](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/222), [MR](https://modrinth.com/mod/MR123/version/1.1.1-fabric-1.20.1))"
suffix = "**JAR downloads**\n" + forge_row + "\n" + fabric_row
exact = 2000 - len(prefix + "\n\n" + suffix)
body = {
    "short": "### FIXED\n\n- Clearer status.",
    "multiline": "### ADDED\n\n- First line.\n- @everyone <@123> <@&456>\n\n### FIXED\n\n- Last line.\n",
    "image": "### FIXED\n\n- Clearer status. ([#308](https://github.com/cTux/ae2-crafting-time/issues/308))\n\n![Crafting status showing the LOCKED provider warning](https://github.com/cTux/ae2-crafting-time/releases/download/release-1.2.2/locked-en-us.png)",
    "empty": "", "null": None,
    "exact": "x" * exact, "over": "x" * (exact + 1),
    "unicode": "### ADDED\n\n" + "Немає енергії 😀\n" * 400,
    "long": "x" * 6500,
}[case]
release["body"] = body
(path / "release.json").write_text(json.dumps(release), encoding="utf-8")
(path / "expected.txt").write_text(prefix + (body + "\n\n" if body else "") + suffix, encoding="utf-8")
PY
  : >"$test_dir/payloads.jsonl"
  (source "$root/scripts/announce-discord-release.sh") >"$test_dir/output.log"
  python3 - "$test_dir" "$case_name" <<'PY'
import json, pathlib, sys
path, case = pathlib.Path(sys.argv[1]), sys.argv[2]
payloads = [json.loads(line) for line in (path / "payloads.jsonl").read_text().splitlines()]
assert all(0 < len(p["content"].encode("utf-16-le")) // 2 <= 2000 for p in payloads), case
assert all(p["allowed_mentions"] == {"parse": []} for p in payloads), case
image_payloads = [p for p in payloads if p["flags"] == 0]
if case == "image":
    assert len(image_payloads) == 1, case
    assert image_payloads[0]["content"].strip() == "![Crafting status showing the LOCKED provider warning](https://github.com/cTux/ae2-crafting-time/releases/download/release-1.2.2/locked-en-us.png)", case
    assert all(p["flags"] == 4 for p in payloads if p not in image_payloads), case
else:
    assert not image_payloads and all(p["flags"] == 4 for p in payloads), case
joined = "".join(p["content"] for p in payloads)
assert joined == (path / "expected.txt").read_text(encoding="utf-8"), case
assert joined.count("https://example/forge.jar") == joined.count("https://example/fabric.jar") == 1
assert "sources.zip" not in joined
assert joined.count("https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/111") == 1
assert joined.count("https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/222") == 1
assert joined.count("https://modrinth.com/mod/MR123/version/1.1.1-forge-1.20.1") == 1
assert joined.count("https://modrinth.com/mod/MR123/version/1.1.1-fabric-1.20.1") == 1
assert "[ae2-crafting-time-1.1.1-forge-1.20.1.jar](https://example/forge.jar) ([CF](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/111), [MR](https://modrinth.com/mod/MR123/version/1.1.1-forge-1.20.1))" in joined
assert "[ae2-crafting-time-1.1.1-fabric-1.20.1.jar](https://example/fabric.jar) ([CF](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time/files/222), [MR](https://modrinth.com/mod/MR123/version/1.1.1-fabric-1.20.1))" in joined
assert len(payloads) == 1 if case in {"short", "multiline", "empty", "null", "exact"} else len(payloads) > 1
output = (path / "output.log").read_text()
assert output.count("confirmed: message") == len(payloads)
assert "announcement complete" in output
PY
done

# A long body needs several parts. Stop on failed or unconfirmed delivery.
for failure in failed unconfirmed; do
  fail_at=0
  unconfirmed_at=0
  if [[ "$failure" == failed ]]; then fail_at=2; else unconfirmed_at=2; fi
  : >"$test_dir/payloads.jsonl"
  if (source "$root/scripts/announce-discord-release.sh") >"$test_dir/output.log" 2>&1; then
    echo "A $failure part should stop delivery." >&2
    exit 1
  fi
  [[ "$(wc -l <"$test_dir/payloads.jsonl")" -eq 2 ]]
  ! grep -q 'announcement complete' "$test_dir/output.log"
  grep -q 'part 1 confirmed: message 1' "$test_dir/output.log"
done
fail_at=0
unconfirmed_at=0

jq '.assets = [.assets[0]]' "$test_dir/release.json" >"$test_dir/partial.json"
mv "$test_dir/partial.json" "$test_dir/release.json"
: >"$test_dir/payloads.jsonl"
if (source "$root/scripts/announce-discord-release.sh" >/dev/null 2>&1); then
  echo "Partial JAR set should not be announced." >&2
  exit 1
fi
[[ ! -s "$test_dir/payloads.jsonl" ]]

# A JAR with no release-matrix entry still announces with its GitHub link and warns.
python3 - "$test_dir" <<'PY'
import json, pathlib, sys
path = pathlib.Path(sys.argv[1])
release = {
    "name": "1.1.1", "html_url": "https://example/release",
    "body": "### FIXED\n\n- Clearer status.",
    "assets": [
        {"name": "ae2-crafting-time-1.1.1-forge-1.20.1.jar", "browser_download_url": "https://example/forge.jar"},
        {"name": "ae2-crafting-time-1.1.1-fabric-1.20.1.jar", "browser_download_url": "https://example/fabric.jar"},
        {"name": "mystery-9.9.9.jar", "browser_download_url": "https://example/mystery.jar"},
    ],
}
(path / "release.json").write_text(json.dumps(release), encoding="utf-8")
matrix = json.loads((path / "matrix.json").read_text(encoding="utf-8"))
matrix.append({"id": "test-bare"})
(path / "matrix.json").write_text(json.dumps(matrix), encoding="utf-8")
PY
: >"$test_dir/payloads.jsonl"
(source "$root/scripts/announce-discord-release.sh") >"$test_dir/output.log" 2>"$test_dir/stderr.log"
python3 - "$test_dir" <<'PY'
import json, pathlib, sys
path = pathlib.Path(sys.argv[1])
payloads = [json.loads(line) for line in (path / "payloads.jsonl").read_text().splitlines()]
assert all(0 < len(p["content"].encode("utf-16-le")) // 2 <= 2000 for p in payloads)
assert all(p["allowed_mentions"] == {"parse": []} for p in payloads)
assert all(p["flags"] == 4 for p in payloads)
joined = "".join(p["content"] for p in payloads)
assert "[mystery-9.9.9.jar](https://example/mystery.jar)" in joined
mystery_row = [line for line in joined.splitlines() if "mystery-9.9.9.jar" in line][0]
assert "[CF]" not in mystery_row and "[MR]" not in mystery_row
assert joined.count("https://example/mystery.jar") == 1
assert joined.count("https://example/forge.jar") == joined.count("https://example/fabric.jar") == 1
assert "mystery-9.9.9.jar" in (path / "stderr.log").read_text(encoding="utf-8")
output = (path / "output.log").read_text()
assert "announcement complete" in output
PY

echo "Discord release announcement tests passed."
