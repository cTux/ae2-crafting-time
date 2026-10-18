#!/usr/bin/env bash
set -euo pipefail

for c in jq curl git; do
  command -v "$c" >/dev/null 2>&1 || {
    echo "Required command '$c' not found. Install it (e.g. 'sudo apt-get install $c') and retry." >&2
    exit 1
  }
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="$script_dir/deploy-changed.sh"
root_dir="$(cd "$script_dir/.." && pwd)"

statePath="$(mktemp -t ae2-crafting-time-release-test-state.XXXXXX.json)"
versionPath="$statePath.version"
printf 'modVersion=1.0.4' > "$versionPath"

bin_dir="$(mktemp -d)"
trap 'rm -f "$statePath" "$versionPath"; rm -rf "$bin_dir"' EXIT

cat > "$bin_dir/gradlew" <<'GRADLEW_EOF'
#!/usr/bin/env bash
set -euo pipefail
module=""
version=""
for a in "$@"; do
  case "$a" in
    :*:distMod) module="${a#:}"; module="${module%:distMod}";;
    -PmodVersion=*) version="${a#-PmodVersion=}";;
  esac
done
root="$PWD"
matrix="$root/scripts/release-matrix.json"
if [ -n "$module" ] && [ -f "$matrix" ]; then
  entry="$(jq -c --arg m "$module" '.[] | select(.module==$m)' "$matrix")"
  modName="$(printf '%s' "$entry" | jq -r '.modName')"
  loader="$(printf '%s' "$entry" | jq -r '.loader')"
  mc="$(printf '%s' "$entry" | jq -r '.minecraftVersion')"
  jar="dist/${modName}-${version}-${loader}-${mc}.jar"
  mkdir -p "$root/dist"
  : > "$root/$jar"
fi
exit 0
GRADLEW_EOF
chmod +x "$bin_dir/gradlew"

cat > "$bin_dir/curl" <<'CURL_EOF'
#!/usr/bin/env bash
exit 0
CURL_EOF
chmod +x "$bin_dir/curl"

export PATH="$bin_dir:$PATH"
export GRADLEW="$bin_dir/gradlew"

assert_contains() {
  local text="$1" pattern="$2" msg="$3"
  if ! printf '%s\n' "$text" | grep -qF -- "$pattern"; then
    echo "$msg" >&2
    echo "--- output ---" >&2
    echo "$text" >&2
    exit 1
  fi
}

assert_not_contains() {
  local text="$1" pattern="$2" msg="$3"
  if printf '%s\n' "$text" | grep -qE -- "$pattern"; then
    echo "$msg" >&2
    echo "--- output ---" >&2
    echo "$text" >&2
    exit 1
  fi
}

releaseDryRun="$("$script" \
  -StatePath "$statePath" \
  -VersionPath "$versionPath" \
  -Deploy \
  -DryRun \
  -ModrinthProjectId test-project \
  -CurseProjectId 1591476 2>&1)"
assert_contains "$releaseDryRun" 'dry-run GitHub Release: 1.0.4' \
  "Release dry run did not create the expected GitHub Release metadata"
assert_contains "$releaseDryRun" '### FIXED' \
  "Release dry run did not create a categorized human-readable changelog"
assert_contains "$releaseDryRun" 'The total TTC now sits in the crafting status header, so it no longer overlaps the action buttons.' \
  "Release dry run did not create a categorized human-readable changelog"
assert_contains "$releaseDryRun" 'dry-run next development version: 1.0.5' \
  "Release dry run did not advance the development version"
while IFS=$'\t' read -r row_id dependencies; do
  dependencies="${dependencies%$'\r'}"
  actual="$(printf '%s\n' "$releaseDryRun" | awk -v row="$row_id" '
    index($0, "dry-run deploy " row ":") == 1 { active=1; next }
    /^dry-run deploy / { active=0 }
    active && /^dry-run Modrinth dependencies:/ { sub(/\r$/, ""); print; exit }
  ')"
  [[ "$actual" == "dry-run Modrinth dependencies: $dependencies" ]] || {
    echo "Release dry run did not preserve $row_id Modrinth dependencies: $actual" >&2
    exit 1
  }
done < <(jq -r '.[] | [.id, (.modrinthDependencies | map(.project_id + ":" + .dependency_type) | join(", "))] | @tsv' "$script_dir/release-matrix.json")
if [ "$(printf '%s\n' "$releaseDryRun" | grep -cF 'dry-run CurseForge relations: applied-energistics-2:requiredDependency, guideme:optionalDependency')" -ne 3 ] || [ "$(printf '%s\n' "$releaseDryRun" | grep -cF 'data-energistics:optionalDependency')" -ne 1 ]; then
  echo "Release dry run did not scope GuideME CurseForge relations to supported targets" >&2
  exit 1
fi

first="$("$script" -StatePath "$statePath" -VersionPath "$versionPath" 2>&1)"
assert_contains "$first" 'build 1.20.1-forge: 1.0.4' "First release run did not build 1.20.1-forge"
assert_contains "$first" 'build 1.21.1-neoforge: 1.0.4' "First release run did not build 1.21.1-neoforge"
assert_contains "$first" 'build 1.20.1-fabric: 1.0.4' "First release run did not build 1.20.1-fabric"
assert_contains "$first" 'build 26.1.2-neoforge: 1.0.4' "First release run did not build 26.1.2-neoforge"

bom="$(head -c 3 "$statePath")"
if [ "$bom" = "$(printf '\xef\xbb\xbf')" ]; then
  echo "Release JSON must be UTF-8 without a BOM" >&2
  exit 1
fi

partialStatePath="$statePath.partial"
cp "$statePath" "$partialStatePath"
old_commit="$(git -C "$root_dir" rev-list --max-parents=0 HEAD)"
jq --arg c "$old_commit" \
  '.["1.20.1-fabric"].fingerprint="changed" | .["1.20.1-fabric"].commit=$c' \
  "$partialStatePath" > "$partialStatePath.tmp" && mv "$partialStatePath.tmp" "$partialStatePath"
printf 'modVersion=1.0.5' > "$versionPath"

partial="$("$script" \
  -StatePath "$partialStatePath" \
  -VersionPath "$versionPath" \
  -Deploy \
  -DryRun \
  -ModrinthProjectId test-project \
  -CurseProjectId 1591476 2>&1)"
assert_contains "$partial" 'dry-run GitHub Release: 1.0.5' \
  "Partial release did not publish only the affected jar at the development version"
assert_contains "$partial" 'dry-run GitHub tag: release-1.0.5' \
  "Partial release did not use a deterministic version tag"
assert_contains "$partial" 'dry-run GitHub assets: ae2-crafting-time-1.0.4-forge-1.20.1.jar, ae2-crafting-time-1.0.5-fabric-1.20.1.jar, ae2-crafting-time-1.0.4-neoforge-1.21.1.jar, ae2-crafting-time-1.0.4-neoforge-26.1.2.jar' \
  "Partial release did not attach every latest jar to GitHub"
assert_contains "$partial" 'dry-run Modrinth version: 1.0.5-fabric-1.20.1' \
  "Partial release did not use a version-first Modrinth version number"
assert_contains "$partial" 'dry-run CurseForge versions: 1.20.1, Fabric, Client, Server' \
  "Partial release did not include CurseForge environment versions"
assert_contains "$partial" '### ADDED' "Generated changelog did not categorize and humanize conventional commits"
assert_contains "$partial" '### FIXED' "Generated changelog did not categorize and humanize conventional commits"
assert_not_contains "$partial" '^- (feat|fix)' \
  "Generated changelog did not categorize and humanize conventional commits"

rm -f "$partialStatePath"

second="$("$script" -StatePath "$statePath" -VersionPath "$versionPath" 2>&1)"
assert_contains "$second" 'skip 1.20.1-forge: unchanged at 1.0.4' "Second release run did not skip unchanged 1.20.1-forge"
assert_contains "$second" 'skip 1.21.1-neoforge: unchanged at 1.0.4' "Second release run did not skip unchanged 1.21.1-neoforge"
assert_contains "$second" 'skip 1.20.1-fabric: unchanged at 1.0.4' "Second release run did not skip unchanged 1.20.1-fabric"
assert_contains "$second" 'skip 26.1.2-neoforge: unchanged at 1.0.4' "Second release run did not skip unchanged 26.1.2-neoforge"

echo "release script check passed"
