#!/usr/bin/env bash
set -euo pipefail

for c in jq curl; do
  command -v "$c" >/dev/null 2>&1 || {
    echo "Required command '$c' not found. Install it (e.g. 'sudo apt-get install $c') and retry." >&2
    exit 1
  }
done

api="https://api.modrinth.com/v2"

Target=""
ResolveOnly=0
Root=""
declare -a GradleArgs=()

while [ $# -gt 0 ]; do
  case "$1" in
    -Target) Target="$2"; shift 2;;
    -ResolveOnly) ResolveOnly=1; shift;;
    -Root) Root="$2"; shift 2;;
    *) GradleArgs+=("$1"); shift;;
  esac
done

valid_targets=("1.20.1-forge" "1.20.1-fabric" "1.21.1-neoforge" "26.1.2-neoforge")
ok=0
for t in "${valid_targets[@]}"; do
  [ "$t" = "$Target" ] && ok=1
done
if [ "$ok" -ne 1 ]; then
  echo "Target must be one of: ${valid_targets[*]}" >&2
  exit 1
fi

case "$Target" in
  1.20.1-forge)
    Module="mc_1_20_1_forge"; Game="1.20.1"; Loader="forge"
    LoaderMetadata="https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
    LoaderPrefix="1.20.1-"; LoaderProperty="runtimeForge1201Version"; Ae2Property="runtimeAe2Forge1201Version"
    ;;
  1.20.1-fabric)
    Module="fabric_1_20_1"; Game="1.20.1"; Loader="fabric"
    LoaderMetadata="https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml"
    LoaderPrefix=""; LoaderProperty="runtimeFabricLoader1201Version"; Ae2Property="runtimeAe2Fabric1201Version"
    ;;
  1.21.1-neoforge)
    Module="mc_1_21_1_neoforge"; Game="1.21.1"; Loader="neoforge"
    LoaderMetadata="https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    LoaderPrefix="21.1."; LoaderProperty="runtimeNeoForge1211Version"; Ae2Property="runtimeAe2NeoForge1211Version"
    ;;
  26.1.2-neoforge)
    Module="mc_26_1_2_neoforge"; Game="26.1.2"; Loader="neoforge"
    LoaderMetadata="https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    LoaderPrefix="26.1.2."; LoaderProperty="runtimeNeoForge2612Version"; Ae2Property="runtimeAe2NeoForge2612Version"
    ;;
esac

provided=("XxWD5pD3" "P7dR8mSH")

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -z "$Root" ]; then
  root="$(cd "$script_dir/.." && pwd)"
else
  root="$Root"
fi

run="$root/versions/$Target/run"
if [ "$Target" = "1.20.1-forge" ]; then
  mods="$run/resolved-mods"
else
  mods="$run/mods"
fi
manifest="$mods/.ae2-crafting-time-run-mods.json"
mkdir -p "$mods"
mapfile -t Projects < <(jq -r --arg target "$Target" \
  '.[] | select(.id==$target) | .modrinthDependencies[] | select(.dependency_type=="optional") | .project_id' \
  "$root/scripts/release-matrix.json")
if [ "$Loader" != "fabric" ]; then Projects+=("Ck4E7v7R"); fi
Projects+=("u6dRKJwZ")

if [ "$Target" = "1.20.1-forge" ]; then
  legacyMods="$run/mods"
  oldManifest="$legacyMods/.ae2-crafting-time-run-mods.json"
  if [ -f "$oldManifest" ]; then
    while IFS= read -r fn; do
      [ -z "$fn" ] && continue
      rm -f "$legacyMods/$(basename "$fn")"
    done < <(jq -r '.[]' "$oldManifest")
    rm -f "$oldManifest"
  fi
fi

declare -A visited=()
declare -a managed=()

get_compatible_version() {
  local projectId="$1" versionId="${2:-}"
  if [ -n "$versionId" ]; then
    curl -fsS "$api/version/$versionId"
  else
    local game_enc="%5B%22$Game%22%5D"
    local loader_enc="%5B%22$Loader%22%5D"
    curl -fsS "$api/project/$projectId/version?game_versions=$game_enc&loaders=$loader_enc" | jq -c '.[0]'
  fi
}

get_latest_maven_version() {
  local url="$1" prefix="$2"
  curl -fsS "$url" \
    | grep -o '<version>[^<]*</version>' \
    | sed 's/<version>//; s|</version>||' \
    | grep "^$prefix" \
    | tail -n 1
}

get_sha512() { sha512sum "$1" | awk '{print $1}'; }

install_file() {
  local file_json="$1"
  local filename; filename="$(echo "$file_json" | jq -r '.filename')"
  local url; url="$(echo "$file_json" | jq -r '.url')"
  local expected; expected="$(echo "$file_json" | jq -r '.hashes.sha512')"
  local dest="$mods/$filename"
  local actual=""
  if [ -f "$dest" ]; then actual="$(get_sha512 "$dest")"; fi
  if [ ! -f "$dest" ] || [ "$actual" != "$expected" ]; then
    local download="$dest.download"
    curl -fsS -o "$download" "$url"
    local dlhash; dlhash="$(get_sha512 "$download")"
    if [ "$dlhash" != "$expected" ]; then
      rm -f "$download"
      echo "Hash mismatch for $filename" >&2
      exit 1
    fi
    mv -f "$download" "$dest"
  fi
  managed+=("$filename")
  echo "mod $filename"
}

install_project() {
  local projectId="$1" versionId="${2:-}"
  local is_provided=0
  for p in "${provided[@]}"; do
    [ "$p" = "$projectId" ] && is_provided=1
  done
  if [ "$is_provided" -eq 1 ]; then return; fi
  if [ -n "${visited[$projectId]:-}" ]; then return; fi
  visited[$projectId]=1

  local version_json; version_json="$(get_compatible_version "$projectId" "$versionId")"
  if [ -z "$version_json" ] || [ "$version_json" = "null" ]; then
    echo "No $Game $Loader version for Modrinth project $projectId" >&2
    exit 1
  fi

  local deps; deps="$(echo "$version_json" | jq -r '.dependencies[] | select(.dependency_type=="required") | "\(.project_id)\t\(if .version_id == null then "" else .version_id end)"')"
  while IFS=$'\t' read -r dpid dvid; do
    [ -z "$dpid" ] && continue
    [ "$dvid" = "null" ] && dvid=""
    install_project "$dpid" "$dvid"
  done <<< "$deps"

  local file_json; file_json="$(echo "$version_json" | jq -c '(.files[] | select(.primary==true)) // .files[0]')"
  if [ -z "$file_json" ] || [ "$file_json" = "null" ]; then
    echo "Modrinth version has no files" >&2
    exit 1
  fi
  install_file "$file_json"
}

for projectId in "${Projects[@]}"; do
  install_project "$projectId"
done

loaderVersion="$(get_latest_maven_version "$LoaderMetadata" "$LoaderPrefix")"
ae2Version_json="$(get_compatible_version "XxWD5pD3" "")"
ae2Version="$(echo "$ae2Version_json" | jq -r '.version_number')"
runtimeArgs=("-P$LoaderProperty=$loaderVersion" "-P$Ae2Property=$ae2Version")
echo "runtime loader $loaderVersion"
echo "runtime ae2 $ae2Version"

if [ "$Target" = "1.20.1-fabric" ]; then
  fabricApiVersion_json="$(get_compatible_version "P7dR8mSH" "")"
  fabricApiVersion="$(echo "$fabricApiVersion_json" | jq -r '.version_number')"
  runtimeArgs+=("-PruntimeFabricApi1201Version=$fabricApiVersion")
  echo "runtime fabric-api $fabricApiVersion"
fi
if [ "$Target" = "1.21.1-neoforge" ]; then
  runtimeArgs+=("-PruntimeAe2NeoForge1211Group=org.appliedenergistics")
  runtimeArgs+=("-PruntimeLatestNeoForge1211")
  echo "runtime ae2 group org.appliedenergistics"
fi

if [ "$Target" = "1.20.1-forge" ]; then
  for filename in "${managed[@]}"; do
    rm -f "$legacyMods/$filename"
  done
fi

declare -a previous=()
if [ -f "$manifest" ]; then
  while IFS= read -r fn; do
    [ -z "$fn" ] && continue
    previous+=("$fn")
  done < <(jq -r '.[]' "$manifest")
fi
for fn in "${previous[@]}"; do
  found=0
  for m in "${managed[@]}"; do
    [ "$m" = "$fn" ] && found=1
  done
  if [ "$found" -eq 0 ]; then
    rm -f "$mods/$(basename "$fn")"
  fi
done
for pattern in "ae2ct-*.jar" "jei-*.jar"; do
  for f in "$mods"/$pattern; do
    [ -e "$f" ] || continue
    keep=0
    for m in "${managed[@]}"; do
      [ "$m" = "$(basename "$f")" ] && keep=1
    done
    if [ "$keep" -eq 0 ]; then
      rm -f "$f"
    fi
  done
done

if [ "${#managed[@]}" -eq 0 ]; then
  echo "[]" > "$manifest"
else
  printf '%s\n' "${managed[@]}" | jq -R . | jq -s . > "$manifest"
fi

echo "mod AE2 Crafting Time (Gradle source set :$Module)"

if [ "$ResolveOnly" -ne 1 ]; then
  gradlew="$root/gradlew"
  "$gradlew" ":$Module:runClient" "${runtimeArgs[@]}" "${GradleArgs[@]}"
  exit $?
fi
