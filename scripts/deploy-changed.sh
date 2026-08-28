#!/usr/bin/env bash
set -euo pipefail

# ---- argument parsing (PowerShell param block) ----
Deploy=0
DryRun=0
ReleaseType=""
ModrinthProjectId=""
CurseProjectId=""
Changelog=""
JavaHome=""
MatrixPath=""
StatePath=""
VersionPath=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -Deploy) Deploy=1; shift ;;
    -DryRun) DryRun=1; shift ;;
    -ReleaseType) ReleaseType="$2"; shift 2 ;;
    -ModrinthProjectId) ModrinthProjectId="$2"; shift 2 ;;
    -CurseProjectId) CurseProjectId="$2"; shift 2 ;;
    -Changelog) Changelog="$2"; shift 2 ;;
    -JavaHome) JavaHome="$2"; shift 2 ;;
    -MatrixPath) MatrixPath="$2"; shift 2 ;;
    -StatePath) StatePath="$2"; shift 2 ;;
    -VersionPath) VersionPath="$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$script_dir/.." && pwd)"
gradlew="${GRADLEW:-$root/gradlew}"

[[ -z "$MatrixPath" ]] && MatrixPath="$script_dir/release-matrix.json"
[[ -z "$StatePath" ]] && StatePath="$root/.release-state.json"
[[ -z "$VersionPath" ]] && VersionPath="$root/gradle.properties"

if [[ -n "$JavaHome" && -d "$JavaHome" ]]; then
  export JAVA_HOME="$JavaHome"
  export PATH="$JavaHome/bin:$PATH"
fi

# ---- helpers ----

git_run() {
  if ! git "$@"; then
    echo "git failed with exit code $?" >&2
    exit 1
  fi
}

curl_run() {
  if ! curl "$@"; then
    echo "curl failed with exit code $?" >&2
    exit 1
  fi
}

version_compare() {
  # echoes -1 if $1<$2, 0 if equal, 1 if $1>$2
  local a1 a2 a3 b1 b2 b3
  IFS=. read -r a1 a2 a3 <<< "$1"
  IFS=. read -r b1 b2 b3 <<< "$2"
  a1=${a1:-0}; a2=${a2:-0}; a3=${a3:-0}
  b1=${b1:-0}; b2=${b2:-0}; b3=${b3:-0}
  if (( a1 < b1 )); then echo -1; return; fi
  if (( a1 > b1 )); then echo 1; return; fi
  if (( a2 < b2 )); then echo -1; return; fi
  if (( a2 > b2 )); then echo 1; return; fi
  if (( a3 < b3 )); then echo -1; return; fi
  if (( a3 > b3 )); then echo 1; return; fi
  echo 0
}

resolve_entry() {
  local entry="$1"
  echo "$entry" | jq -c \
    --arg rt "$ReleaseType" \
    --arg env_rt "${RELEASE_TYPE:-}" \
    --arg mp "$ModrinthProjectId" \
    --arg env_mp "${MODRINTH_PROJECT_ID:-}" \
    --arg cp "$CurseProjectId" \
    --arg env_cp "${CURSEFORGE_PROJECT_ID:-}" \
    '
    .releaseType = (if $rt != "" then $rt
                    elif $env_rt != "" then $env_rt
                    else .releaseType end)
    | .modrinthProjectId = (if $mp != "" then $mp
                    elif $env_mp != "" then $env_mp
                    else (.modrinthProjectId // "") end)
    | .curseProjectId = (if $cp != "" then $cp
                    elif $env_cp != "" then $env_cp
                    else (.curseProjectId // "") end)
    '
}

assert_entry() {
  local entry="$1"
  local id; id="$(echo "$entry" | jq -r .id)"
  local req=("id" "module" "loader" "loaderName" "minecraftVersion" "projectDir" "modName" "initialVersion" "releaseType" "modrinthDependencies")
  local f
  for f in "${req[@]}"; do
    if [[ "$(echo "$entry" | jq -r --arg f "$f" 'has($f)')" != "true" ]]; then
      echo "Release entry is missing '$f'" >&2
      return 1
    fi
  done
  local iv; iv="$(echo "$entry" | jq -r .initialVersion)"
  if ! echo "$iv" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "$id initialVersion must be x.y.z" >&2
    return 1
  fi
  case "$(echo "$entry" | jq -r .releaseType)" in
    alpha|beta|release) ;;
    *) echo "$id releaseType must be alpha, beta, or release" >&2; return 1 ;;
  esac
  local n; n="$(echo "$entry" | jq '.["modrinthDependencies"] | length')"
  local j pid dtype
  for ((j=0; j<n; j++)); do
    pid="$(echo "$entry" | jq -r --argjson j "$j" '.["modrinthDependencies"][$j].project_id // empty')"
    dtype="$(echo "$entry" | jq -r --argjson j "$j" '.["modrinthDependencies"][$j].dependency_type // empty')"
    if [[ -z "$pid" ]]; then
      echo "$id has invalid Modrinth dependency metadata" >&2; return 1
    fi
    case "$dtype" in
      required|optional|incompatible|embedded) ;;
      *) echo "$id has invalid Modrinth dependency metadata" >&2; return 1 ;;
    esac
  done
}

next_patch_version() {
  local v="$1"
  if ! echo "$v" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Version '$v' is not x.y.z" >&2
    return 1
  fi
  local a b c; IFS=. read -r a b c <<< "$v"
  echo "$a.$b.$((c+1))"
}

get_artifact_filename() {
  local entry="$1" version="$2"
  local modName loader mc
  modName="$(echo "$entry" | jq -r .modName)"
  loader="$(echo "$entry" | jq -r .loader)"
  mc="$(echo "$entry" | jq -r .minecraftVersion)"
  echo "$modName-$version-$loader-$mc.jar"
}

get_dev_version() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Version file does not exist: $path" >&2
    return 1
  fi
  local v
  v="$(grep -E '^modVersion=([0-9]+\.[0-9]+\.[0-9]+)$' "$path" | head -n1 | sed 's/^modVersion=//')"
  if [[ -z "$v" ]]; then
    echo "Version file must contain modVersion=x.y.z: $path" >&2
    return 1
  fi
  echo "$v"
}

set_dev_version() {
  local path="$1" version="$2"
  local tmp; tmp="$(mktemp)"
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" =~ ^modVersion= ]]; then
      echo "modVersion=$version" >> "$tmp"
    else
      echo "$line" >> "$tmp"
    fi
  done < "$path"
  mv "$tmp" "$path"
}

get_input_fingerprint() {
  local entry="$1"
  local projectDir; projectDir="$(echo "$entry" | jq -r .projectDir)"
  local paths=("build.gradle" "settings.gradle" "shared/src/main" "shared/build.gradle" "$projectDir/build.gradle" "$projectDir/src/main")
  local p full filelist content rel h
  filelist="$(mktemp)"
  for p in "${paths[@]}"; do
    full="$root/$p"
    if [[ ! -e "$full" ]]; then
      echo "Missing release input path: $p" >&2
      rm -f "$filelist"
      return 1
    fi
    find "$full" -type f | grep -vE '/(build|run|bin|logs)/' | sort -u
  done > "$filelist" || true

  content=""
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    rel="${f#"$root"/}"
    h="$(sha256sum "$f" | cut -d' ' -f1)"
    content+="$rel $h"$'\n'
  done < "$filelist"
  rm -f "$filelist"
  content="${content%$'\n'}"
  printf '%s' "$content" | sha256sum | cut -d' ' -f1
}

assert_changelog() {
  local text="$1"
  if ! printf '%s\n' "$text" | grep -Eq '^### (ADDED|FIXED|IMPROVED|DELETED|CHANGED)$'; then
    echo "Changelog must use human-readable ### ADDED, FIXED, IMPROVED, DELETED, or CHANGED categories" >&2
    return 1
  fi
  printf '%s\n' "$text"
}

format_changelog() {
  local subjects="$1"
  local tmpdir; tmpdir="$(mktemp -d)"
  : > "$tmpdir/ADDED"
  : > "$tmpdir/FIXED"
  : > "$tmpdir/IMPROVED"
  : > "$tmpdir/DELETED"
  : > "$tmpdir/CHANGED"
  local type text category line re
  re='^([a-z]+)(\([^)]+\))?!?:[[:space:]]*(.*)$'
  while IFS= read -r subject; do
    [[ -z "$subject" ]] && continue
    type=""
    text="$subject"
    if [[ "$text" =~ $re ]]; then
      type="${BASH_REMATCH[1]}"
      text="${BASH_REMATCH[3]}"
    fi
    text="$(echo "$text" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    if [[ "$text" =~ ^([Dd]elete|[Dd]rop|[Rr]emove)[[:space:]] ]]; then
      category="DELETED"
    else
      case "$type" in
        feat) category="ADDED" ;;
        fix) category="FIXED" ;;
        perf) category="IMPROVED" ;;
        *) category="CHANGED" ;;
      esac
    fi
    if [[ -n "$text" ]]; then
      text="$(echo "$text" | sed 's/^\(.\)/\U\1/')"
      if ! [[ "$text" =~ [.!?]$ ]]; then
        text="$text."
      fi
      echo "- $text" >> "$tmpdir/$category"
    fi
  done <<< "$subjects"

  local out=""
  local cat
  for cat in ADDED FIXED IMPROVED DELETED CHANGED; do
    if [[ -s "$tmpdir/$cat" ]]; then
      if [[ -n "$out" ]]; then
        out+=$'\n'$'\n'
      fi
      out+="### $cat"$'\n'$'\n'"$(cat "$tmpdir/$cat")"
    fi
  done
  echo "$out"
  rm -rf "$tmpdir"
}

get_entry_changelog() {
  local entry="$1" previous="$2"
  if [[ -n "$Changelog" ]]; then
    assert_changelog "$Changelog"
    return $?
  fi
  if [[ "$previous" = "null" ]] || [[ -z "$(echo "$previous" | jq -r '.commit // empty')" ]]; then
    assert_changelog "$(echo "$entry" | jq -r '.changelog // empty')"
    return $?
  fi
  local prev_commit; prev_commit="$(echo "$previous" | jq -r '.commit')"
  local projectDir; projectDir="$(echo "$entry" | jq -r .projectDir)"
  local subjects
  subjects="$(git log "$prev_commit..HEAD" --format=%s -- \
    "build.gradle" "settings.gradle" "shared/src/main" "shared/src/mc1201" \
    "$projectDir/build.gradle" "$projectDir/src/main")" || true
  if [[ -z "$subjects" ]]; then
    assert_changelog "$(echo "$entry" | jq -r '.changelog // empty')"
    return $?
  fi
  format_changelog "$subjects"
}

publish_modrinth() {
  local entry="$1" version="$2" jarPath="$3" notes="$4"
  local pid; pid="$(echo "$entry" | jq -r '.modrinthProjectId // empty')"
  [[ -n "$pid" ]] || return 0
  if [[ -z "${MODRINTH_TOKEN:-}" ]]; then
    echo "MODRINTH_TOKEN is required for Modrinth upload" >&2
    return 1
  fi
  local name; name="$(get_artifact_filename "$entry" "$version")"
  name="${name%.jar}"
  local data; data="$(echo "$entry" | jq -c \
    --arg name "$name" \
    --arg vn "$(echo "$entry" | jq -r .id)-$version" \
    --arg notes "$notes" \
    '{
      name: $name,
      version_number: $vn,
      changelog: $notes,
      dependencies: .modrinthDependencies,
      game_versions: [.minecraftVersion],
      version_type: .releaseType,
      loaders: [.loader],
      featured: false,
      status: "listed",
      requested_status: "listed",
      project_id: .modrinthProjectId,
      file_parts: ["file"],
      primary_file: "file"
    }')"
  local dataPath; dataPath="$(mktemp)"
  printf '%s' "$data" > "$dataPath"
  curl_run -sS --fail-with-body \
    -H "Authorization: $MODRINTH_TOKEN" \
    -H "User-Agent: ctux/ae2-crafting-time-release-script" \
    -F "data=<$dataPath;type=application/json" \
    -F "file=@$jarPath;type=application/java-archive" \
    "https://api.modrinth.com/v2/version"
  rm -f "$dataPath"
}

publish_curseforge() {
  local entry="$1" version="$2" jarPath="$3" notes="$4"
  local pid; pid="$(echo "$entry" | jq -r '.curseProjectId // empty')"
  [[ -n "$pid" ]] || return 0
  if [[ -z "${CURSEFORGE_TOKEN:-}" ]]; then
    echo "CURSEFORGE_TOKEN is required for CurseForge upload" >&2
    return 1
  fi
  local name; name="$(get_artifact_filename "$entry" "$version")"
  name="${name%.jar}"
  local metadata; metadata="$(echo "$entry" | jq -c \
    --arg notes "$notes" \
    --arg name "$name" \
    '{
      changelog: $notes,
      changelogType: "text",
      displayName: $name,
      gameVersionNames: [.minecraftVersion, .loaderName, "Client", "Server"],
      releaseType: .releaseType,
      isMarkedForManualRelease: false
    }')"
  local metaPath; metaPath="$(mktemp)"
  printf '%s' "$metadata" > "$metaPath"
  curl_run -sS --fail-with-body \
    -H "X-Api-Token: $CURSEFORGE_TOKEN" \
    -F "metadata=<$metaPath;type=application/json" \
    -F "file=@$jarPath;type=application/java-archive" \
    "https://minecraft.curseforge.com/api/projects/$pid/upload-file"
  rm -f "$metaPath"
}

publish_github_release() {
  local releases_json="$1" sourceCommit="$2"
  local stamp; stamp="$(date -u +%Y%m%d-%H%M%S)"
  local tag="release-$stamp"
  local title; title="$(echo "$releases_json" | jq -r '.[0].version')"
  local notes; notes="$(echo "$releases_json" | jq -r '
    map("## " + (.entry.modName + "-" + .version + "-" + .entry.loader + "-" + .entry.minecraftVersion + ".jar") + "\n\n" + .changelog)
    | join("\n\n")')"

  if [[ "$DryRun" = "1" ]]; then
    echo "dry-run GitHub Release: $title"
    local assets; assets="$(echo "$releases_json" | jq -r '.[].jarPath | sub(".*/";"")' | paste -sd ', ' -)"
    echo "dry-run GitHub assets: $assets"
    echo "$notes"
    return 0
  fi

  local notesPath; notesPath="$(mktemp)"
  printf '%s\n' "$notes" > "$notesPath"
  local args=()
  args+=(release create "$tag")
  local i n; n="$(echo "$releases_json" | jq 'length')"
  for ((i=0; i<n; i++)); do
    args+=("$(echo "$releases_json" | jq -r ".[$i].jarPath")")
  done
  args+=(--target "$sourceCommit" --title "$title" --notes-file "$notesPath")
  if echo "$releases_json" | jq -e 'any(.[]; .entry.releaseType == "alpha" or .entry.releaseType == "beta")' >/dev/null; then
    args+=(--prerelease)
  fi
  if ! gh "${args[@]}"; then
    echo "GitHub Release creation failed" >&2
    rm -f "$notesPath"
    exit 1
  fi
  rm -f "$notesPath"
}

# ---- main ----

if [[ -f "$MatrixPath" ]]; then
  matrix="$(jq -c . "$MatrixPath")"
else
  matrix="[]"
fi
if [[ -f "$StatePath" ]]; then
  state="$(jq -c . "$StatePath")"
else
  state="{}"
fi
developmentVersion="$(get_dev_version "$VersionPath")"

cd "$root"

if [[ "$Deploy" = "1" && "$DryRun" != "1" && -n "$(git status --porcelain)" ]]; then
  echo "Commit or stash all changes before creating a release" >&2
  exit 1
fi

sourceCommit="$(git rev-parse HEAD)"

plans_json="[]"
count="$(echo "$matrix" | jq 'length')"
for ((i=0; i<count; i++)); do
  entry="$(echo "$matrix" | jq -c ".[$i]")"
  entry="$(resolve_entry "$entry")"
  if ! assert_entry "$entry"; then
    exit 1
  fi
  id="$(echo "$entry" | jq -r .id)"
  fingerprint="$(get_input_fingerprint "$entry")"
  previous="$(echo "$state" | jq -c --arg id "$id" '.[$id] // null')"
  if [[ "$previous" = "null" ]]; then
    currentVersion="$(echo "$entry" | jq -r .initialVersion)"
  else
    currentVersion="$(echo "$previous" | jq -r .version)"
  fi

  changed="false"
  if [[ "$previous" = "null" ]] || [[ "$(echo "$previous" | jq -r .fingerprint)" != "$fingerprint" ]]; then
    changed="true"
  fi

  if [[ "$changed" = "false" ]]; then
    echo "skip $id: unchanged at $currentVersion"
  fi

  if [[ "$changed" = "true" && "$Deploy" = "1" && -z "$(echo "$entry" | jq -r '.modrinthProjectId // empty')" ]]; then
    echo "$id has no Modrinth project id. Set it in the matrix or MODRINTH_PROJECT_ID." >&2
    exit 1
  fi
  if [[ "$changed" = "true" && "$Deploy" = "1" && -z "$(echo "$entry" | jq -r '.curseProjectId // empty')" ]]; then
    echo "$id has no CurseForge project id. Set it in the matrix or CURSEFORGE_PROJECT_ID." >&2
    exit 1
  fi

  if [[ "$changed" = "true" && "$previous" != "null" ]]; then
    cmp="$(version_compare "$developmentVersion" "$currentVersion")"
    if [[ "$cmp" = "-1" || "$cmp" = "0" ]]; then
      echo "Development version $developmentVersion must be newer than released $id $currentVersion" >&2
      exit 1
    fi
  fi

  if [[ "$changed" = "true" ]]; then
    version="$developmentVersion"
  else
    version="$currentVersion"
  fi

  filename="$(get_artifact_filename "$entry" "$version")"
  jarPath="$root/dist/$filename"

  changelog=""
  if [[ "$changed" = "true" ]]; then
    if ! changelog="$(get_entry_changelog "$entry" "$previous")"; then
      echo "Failed to resolve changelog for $id" >&2
      exit 1
    fi
  fi

  el="$(jq -nc \
    --argjson entry "$entry" \
    --arg id "$id" \
    --arg version "$version" \
    --arg fp "$fingerprint" \
    --arg jar "$jarPath" \
    --arg cl "$changelog" \
    --argjson changed "$changed" \
    '{entry:$entry,id:$id,version:$version,fingerprint:$fp,jarPath:$jar,changelog:$cl,changed:$changed}')"
  plans_json="$(echo "$plans_json" | jq -c --argjson el "$el" '. + [$el]')"
done

releases_json="$(echo "$plans_json" | jq -c 'map(select(.changed))')"
rc="$(echo "$releases_json" | jq 'length')"

if [[ "$Deploy" = "1" && "$rc" -gt 0 ]]; then
  builds_json="$plans_json"
else
  builds_json="$releases_json"
fi

bc="$(echo "$builds_json" | jq 'length')"
for ((i=0; i<bc; i++)); do
  bel="$(echo "$builds_json" | jq -c ".[$i]")"
  bid="$(echo "$bel" | jq -r .id)"
  bver="$(echo "$bel" | jq -r .version)"
  bent="$(echo "$bel" | jq -c .entry)"
  bchg="$(echo "$bel" | jq -r .changed)"
  bjp="$(echo "$bel" | jq -r .jarPath)"
  if [[ "$bchg" = "true" ]]; then
    label="build"
  else
    label="build latest"
  fi
  echo "$label $bid: $bver"
  if [[ "$DryRun" != "1" ]]; then
    module="$(echo "$bent" | jq -r .module)"
    "$gradlew" ":$module:distMod" "-PmodVersion=$bver"
    if [[ ! -f "$bjp" ]]; then
      echo "Expected jar was not created: $bjp" >&2
      exit 1
    fi
  fi
done

if [[ "$Deploy" = "1" && "$rc" -gt 0 ]]; then
  for ((i=0; i<rc; i++)); do
    rel="$(echo "$releases_json" | jq -c ".[$i]")"
    rentry="$(echo "$rel" | jq -c .entry)"
    rver="$(echo "$rel" | jq -r .version)"
    rjar="$(echo "$rel" | jq -r .jarPath)"
    rcl="$(echo "$rel" | jq -r .changelog)"
    rid="$(echo "$rel" | jq -r .id)"
    if [[ "$DryRun" = "1" ]]; then
      echo "dry-run deploy $rid: $rjar"
      echo "dry-run Modrinth version: $(echo "$rentry" | jq -r .id)-$rver"
      deps="$(echo "$rentry" | jq -r '.["modrinthDependencies"] | map("\(.project_id):\(.dependency_type)") | join(", ")')"
      echo "dry-run Modrinth dependencies: $deps"
      echo "dry-run CurseForge versions: $(echo "$rentry" | jq -r .minecraftVersion), $(echo "$rentry" | jq -r .loaderName), Client, Server"
    else
      publish_modrinth "$rentry" "$rver" "$rjar" "$rcl"
      publish_curseforge "$rentry" "$rver" "$rjar" "$rcl"
    fi
  done
  publish_github_release "$releases_json" "$sourceCommit"
fi

if [[ "$Deploy" = "1" && "$rc" -gt 0 ]]; then
  next="$(next_patch_version "$developmentVersion")"
  if [[ "$DryRun" = "1" ]]; then
    echo "dry-run next development version: $next"
  else
    set_dev_version "$VersionPath" "$next"
  fi
fi

if [[ "$DryRun" != "1" ]]; then
  for ((i=0; i<rc; i++)); do
    rel="$(echo "$releases_json" | jq -c ".[$i]")"
    rid="$(echo "$rel" | jq -r .id)"
    rver="$(echo "$rel" | jq -r .version)"
    rfp="$(echo "$rel" | jq -r .fingerprint)"
    rjar="$(echo "$rel" | jq -r .jarPath)"
    relpath="${rjar#"$root"/}"
    iso="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    val="$(jq -nc --arg version "$rver" --arg fp "$rfp" --arg jar "$relpath" --arg commit "$sourceCommit" --arg ts "$iso" '{version:$version,fingerprint:$fp,jar:$jar,commit:$commit,updatedAt:$ts}')"
    state="$(echo "$state" | jq -c --arg id "$rid" --argjson v "$val" '.[$id]=$v')"
  done
  echo "$state" | jq . > "$StatePath"
fi

if [[ "$Deploy" = "1" && "$DryRun" != "1" && "$rc" -gt 0 ]]; then
  case "$StatePath" in
    "$root"/*) ;;
    *) echo "Release state must be inside the repository: $StatePath" >&2; exit 1 ;;
  esac
  case "$VersionPath" in
    "$root"/*) ;;
    *) echo "Version file must be inside the repository: $VersionPath" >&2; exit 1 ;;
  esac
  stateRel="${StatePath#"$root"/}"
  versionRel="${VersionPath#"$root"/}"
  git_run add -- "$stateRel" "$versionRel"
  versions_str="$(echo "$releases_json" | jq -r 'map("\(.id) \(.version)") | join(", ")')"
  git_run commit -m "chore(release): $versions_str"
  git_run push
fi
