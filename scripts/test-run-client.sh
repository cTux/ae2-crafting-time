#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="$script_dir/run-client.sh"
jq -e --slurpfile release "$script_dir/release-matrix.json" '
  ([.[].id] == [$release[0][].id]) and
  all(.[]; ([.projects[].project_id] | length) == ([.projects[].project_id] | unique | length)) and
  all(.[]; ([.compatible.versions[].project_id] | length) == ([.compatible.versions[].project_id] | unique | length)) and
  all(.[]; ([.compatible.versions[].project_id] as $locks | all(.projects[] | select(.compatible != false); .project_id as $id | $locks | index($id))))
' "$script_dir/run-client-versions.json" >/dev/null
temp="$(mktemp -d)"
bin_dir="$(mktemp -d)"
trap 'rm -rf "$temp" "$bin_dir"' EXIT
sha="$(printf 'test mod' | sha512sum | awk '{print $1}')"
mod_version="$(sed -n 's/^modVersion=//p' "$script_dir/../gradle.properties" | head -n 1)"
printf 'modVersion=%s\n' "$mod_version" > "$temp/gradle.properties"
cat > "$temp/gradlew" <<GRADLE_EOF
#!/usr/bin/env bash
set -euo pipefail
[ -z "\${AE2CT_DRIVER_BUILD_FAIL:-}" ] || exit 9
mkdir -p "\$(dirname "\$0")/build/test-driver"
printf driver > "\$(dirname "\$0")/build/test-driver/ae2-crafting-time-$mod_version-forge-1.20.1-test-driver.jar"
printf driver > "\$(dirname "\$0")/build/test-driver/ae2-crafting-time-$mod_version-fabric-1.20.1-test-driver.jar"
GRADLE_EOF
chmod +x "$temp/gradlew"

cat > "$bin_dir/curl" <<'CURL_EOF'
#!/usr/bin/env bash
set -euo pipefail
if [ "${2:-}" = "-o" ]; then
  if [ -n "${AE2CT_BAD_DOWNLOAD:-}" ]; then printf '\x00' > "$3"; else printf 'test mod' > "$3"; fi
  exit 0
fi
url="${2:-}"
case "$url" in
  *minecraftforge*) printf '<metadata><versioning><versions><version>1.20.1-1</version><version>1.20.1-99</version></versions></versioning></metadata>'; exit 0;;
  *fabric-loader*) printf '<metadata><versioning><versions><version>0.1.0</version><version>0.99.0</version></versions></versioning></metadata>'; exit 0;;
  *neoforged*) printf '<metadata><versioning><versions><version>21.1.1</version><version>21.1.99</version><version>26.1.2.1</version><version>26.1.2.100</version></versions></versioning></metadata>'; exit 0;;
esac
id="$(printf '%s' "$url" | sed -E 's#.*/version/([^/?]+).*#\1#')"
project="$(printf '%s' "$url" | sed -E 's#.*/project/([^/]+)/version.*#\1#')"
version="latest"
if [[ "$url" == */version/* ]]; then
  project="$id"
  case "$id" in
    7KVs6HMQ) project="XxWD5pD3"; version="15.4.10";;
    kywcQ25B) project="XxWD5pD3"; version="15.1.0";;
    kfyIqgJ6) project="XxWD5pD3"; version="19.2.17";;
    pK0VDmDU) project="XxWD5pD3"; version="26.1.10-beta";;
    xhLT3C5f) project="P7dR8mSH"; version="0.92.11+1.20.1";;
    *) version="$id";;
  esac
fi
if [ "$project" = "XxWD5pD3" ] && [ "$version" = "latest" ]; then
  case "$url" in *26.1.2*) version="26.99.0-beta";; *1.21.1*) version="19.99.0";; *) version="15.99.0";; esac
elif [ "$project" = "P7dR8mSH" ] && [ "$version" = "latest" ]; then version="0.99.0+1.20.1"; fi
json="{\"id\":\"$id\",\"version_number\":\"$version\",\"dependencies\":[{\"project_id\":\"XxWD5pD3\",\"version_id\":\"old-pin\",\"dependency_type\":\"required\"}],\"files\":[{\"filename\":\"$project.jar\",\"hashes\":{\"sha512\":\"${AE2CT_TEST_SHA512}\"},\"url\":\"https://example.invalid/$project.jar\",\"primary\":true}]}"
if [[ "$url" == */project/* ]]; then printf '[%s]' "$json"; else printf '%s' "$json"; fi
CURL_EOF
chmod +x "$bin_dir/curl"
export AE2CT_TEST_SHA512="$sha" PATH="$bin_dir:$PATH"
test_matrix="$temp/run-client-versions.json"
jq --arg hash "$sha" 'walk(if type == "object" and has("sha512") then .sha512 = $hash else . end) |
  map(if .id == "1.20.1-forge" then
    .projects |= map(if .project_id == "E6BFl96N" then .modrinth_dependencies = ["matrix-extra", "matrix-extra", "XxWD5pD3"] else . end) |
    .projects += [{project_id: "matrix-extra", compatible: false, modrinth_dependencies: ["E6BFl96N"]}] |
    .compatible.versions += [{project_id: "matrix-extra", version_id: "matrix-extra", version: "test"}]
  else . end)' "$script_dir/run-client-versions.json" > "$test_matrix"

assert_line() { printf '%s\n' "$1" | grep -qxF "$2" || { echo "Missing '$2'" >&2; exit 1; }; }

while IFS= read -r target; do
  if [ "$target" = "1.20.1-forge" ]; then
    mkdir -p "$temp/versions/$target/run/resolved-mods"
    printf stale > "$temp/versions/$target/run/resolved-mods/ae2-crafting-time-old-forge-1.20.1-test-driver.jar"
  fi
  output="$("$script" -Target "$target" -Root "$temp" -VersionMatrix "$test_matrix" -ResolveOnly 2>&1)"
  assert_line "$output" "profile compatible"
  if [ "$target" = "1.20.1-forge" ]; then
    assert_line "$output" 'mod applied-botanics-forge-1.5.2.jar'
    assert_line "$output" 'mod BpFDhV66.jar'
    assert_line "$output" 'mod matrix-extra.jar'
    [ "$(printf '%s\n' "$output" | grep -xcF 'mod matrix-extra.jar')" -eq 1 ]
    if printf '%s\n' "$output" | grep -qxF 'mod ByiqRpj3.jar'; then echo 'Original and fork loaded together' >&2; exit 1; fi
  fi
  output="$("$script" -Target "$target" -Root "$temp" -VersionMatrix "$test_matrix" -Latest -ResolveOnly 2>&1)"
  assert_line "$output" "profile latest"
  if [ "$target" = "1.20.1-forge" ]; then
    assert_line "$output" 'mod applied-botanics-forge-1.5.2.jar'
    assert_line "$output" 'mod pfjLUfGv.jar'
    if printf '%s\n' "$output" | grep -qxF 'mod 545hUrw9.jar'; then echo 'Original and fork loaded together' >&2; exit 1; fi
  fi
  if [ "$target" = "1.20.1-forge" ]; then leaf="resolved-mods"; else leaf="mods"; fi
  [ -f "$temp/versions/$target/run/$leaf/.ae2-crafting-time-run-mods.json" ]
  [ -f "$temp/versions/$target/run-latest/$leaf/.ae2-crafting-time-run-mods.json" ]
  if [ "$target" = "1.20.1-forge" ] || [ "$target" = "1.20.1-fabric" ]; then
    loader="${target#1.20.1-}"
    driver="ae2-crafting-time-$mod_version-$loader-1.20.1-test-driver.jar"
    [ -f "$temp/versions/$target/run/$leaf/$driver" ]
    [ -f "$temp/versions/$target/run-latest/$leaf/$driver" ]
    [ ! -f "$temp/versions/$target/run/$leaf/ae2-crafting-time-old-forge-1.20.1-test-driver.jar" ]
  elif find "$temp/versions/$target" -name '*test-driver.jar' -print -quit | grep -q .; then
    echo "Driver leaked into $target" >&2
    exit 1
  fi
done < <(jq -r '.[].id' "$script_dir/run-client-versions.json")

custom_runtime="$temp/custom-runtime"
"$script" -Target 1.20.1-forge -Root "$temp" -VersionMatrix "$test_matrix" -RuntimeDirectory "$custom_runtime" -ResolveOnly >/dev/null
[ -f "$custom_runtime/resolved-mods/ae2-crafting-time-$mod_version-forge-1.20.1-test-driver.jar" ]

export AE2CT_DRIVER_BUILD_FAIL=1
if out="$("$script" -Target 1.20.1-forge -Root "$temp" -VersionMatrix "$test_matrix" -ResolveOnly 2>&1)"; then
  echo "Expected test-driver build failure" >&2
  exit 1
fi
unset AE2CT_DRIVER_BUILD_FAIL

bad="$temp/versions/1.20.1-forge/run/resolved-mods/flhDmaU7.jar"
rm -f "$bad"
export AE2CT_BAD_DOWNLOAD=1
if out="$("$script" -Target 1.20.1-forge -Root "$temp" -VersionMatrix "$test_matrix" -ResolveOnly 2>&1)"; then echo "Expected hash mismatch" >&2; exit 1; fi
printf '%s\n' "$out" | grep -q 'Hash mismatch for' || { echo "$out" >&2; exit 1; }
echo "run-client checks passed"
