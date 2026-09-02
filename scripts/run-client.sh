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
Latest=0
Root=""
VersionMatrix=""
RuntimeDirectory=""
DriverScenario=""
DriverOutputDirectory=""
DriverWorld=""
Interactive=0
declare -a GradleArgs=()

while [ $# -gt 0 ]; do
  case "$1" in
    -Target) Target="$2"; shift 2;;
    -Latest) Latest=1; shift;;
    -ResolveOnly) ResolveOnly=1; shift;;
    -Root) Root="$2"; shift 2;;
    -VersionMatrix) VersionMatrix="$2"; shift 2;;
    -RuntimeDirectory) RuntimeDirectory="$2"; shift 2;;
    -DriverScenario) DriverScenario="$2"; shift 2;;
    -DriverOutputDirectory) DriverOutputDirectory="$2"; shift 2;;
    -DriverWorld) DriverWorld="$2"; shift 2;;
    -Interactive) Interactive=1; shift;;
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

if [ "$Latest" -eq 1 ]; then run_name="run-latest"; else run_name="run"; fi
if [ -n "$RuntimeDirectory" ]; then mkdir -p "$RuntimeDirectory"; run="$(cd "$RuntimeDirectory" && pwd)"; else run="$root/versions/$Target/$run_name"; fi
if [ "$Target" = "1.20.1-forge" ]; then
  mods="$run/resolved-mods"
else
  mods="$run/mods"
fi
manifest="$mods/.ae2-crafting-time-run-mods.json"
mkdir -p "$mods"
if [ -n "$VersionMatrix" ]; then matrix="$VersionMatrix"; else matrix="$script_dir/run-client-versions.json"; fi
while IFS= read -r replaced; do provided+=("$replaced"); done < <(
  jq -r --arg target "$Target" '.[] | select(.id==$target) | .curseforge[]? | .replaces_project_id // empty' "$matrix")
if [ "$Latest" -eq 1 ]; then project_filter='.projects[]'; else project_filter='.projects[] | select(.compatible != false)'; fi
mapfile -t Projects < <(jq -r --arg target "$Target" ".[] | select(.id==\$target) | $project_filter | .project_id" "$matrix")
declare -A VersionPins=()
while IFS=$'\t' read -r project_id version_id; do
  VersionPins[$project_id]="$version_id"
done < <(jq -r --arg target "$Target" '.[] | select(.id==$target) | .compatible.versions[] | "\(.project_id)\t\(.version_id)"' "$matrix")

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

get_project_version() {
  local projectId="$1"
  if [ "$Latest" -eq 1 ]; then
    local game_enc="%5B%22$Game%22%5D" loader_enc="%5B%22$Loader%22%5D"
    curl -fsS "$api/project/$projectId/version?game_versions=$game_enc&loaders=$loader_enc" | jq -c '.[0]'
  else
    local versionId="${VersionPins[$projectId]:-}"
    if [ -z "$versionId" ]; then
      echo "Missing compatible version for Modrinth project $projectId" >&2
      exit 1
    fi
    curl -fsS "$api/version/$versionId"
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
  local projectId="$1"
  local is_provided=0
  for p in "${provided[@]}"; do
    [ "$p" = "$projectId" ] && is_provided=1
  done
  if [ "$is_provided" -eq 1 ]; then return; fi
  if [ -n "${visited[$projectId]:-}" ]; then return; fi
  visited[$projectId]=1

  local version_json; version_json="$(get_project_version "$projectId")"
  if [ -z "$version_json" ] || [ "$version_json" = "null" ]; then
    echo "No $Game $Loader version for Modrinth project $projectId" >&2
    exit 1
  fi

  local deps; deps="$(echo "$version_json" | jq -r '.dependencies[] | select(.dependency_type=="required") | "\(.project_id)\t\(if .version_id == null then "" else .version_id end)"')"
  while IFS=$'\t' read -r dpid dvid; do
    [ -z "$dpid" ] && continue
    [ "$dvid" = "null" ] && dvid=""
    install_project "$dpid"
  done <<< "$deps"

  local extra
  while IFS= read -r extra; do install_project "$extra"; done < <(
    jq -r --arg target "$Target" --arg project "$projectId" '.[] | select(.id==$target) | .projects[] | select(.project_id==$project) | .modrinth_dependencies[]?' "$matrix")

  local file_json; file_json="$(echo "$version_json" | jq -c '(.files[] | select(.primary==true)) // .files[0]')"
  if [ -z "$file_json" ] || [ "$file_json" = "null" ]; then
    echo "Modrinth version has no files" >&2
    exit 1
  fi
  install_file "$file_json"
}

for projectId in "${Projects[@]}"; do install_project "$projectId"; done
while IFS= read -r dependency; do
  [ -z "$dependency" ] && continue
  while IFS= read -r projectId; do install_project "$projectId"; done < <(echo "$dependency" | jq -r '.modrinth_dependencies[]?')
  if [ "$Latest" -eq 1 ]; then file="$(echo "$dependency" | jq -c '.latest')"; else file="$(echo "$dependency" | jq -c '.compatible')"; fi
  file_id="$(echo "$file" | jq -r '.file_id')"
  filename="$(echo "$file" | jq -r '.filename')"
  group="${file_id:0:4}"; rest="${file_id:4}"
  file="$(echo "$file" | jq -c --arg url "https://mediafilez.forgecdn.net/files/$group/$rest/$filename" '. + {url:$url, hashes:{sha512:.sha512}}')"
  install_file "$file"
done < <(jq -c --arg target "$Target" '.[] | select(.id==$target) | .curseforge[]?' "$matrix")

if [ "$Latest" -eq 1 ]; then
  loaderVersion="$(get_latest_maven_version "$LoaderMetadata" "$LoaderPrefix")"
  ae2Version_json="$(curl -fsS "$api/project/XxWD5pD3/version?game_versions=%5B%22$Game%22%5D&loaders=%5B%22$Loader%22%5D" | jq -c '.[0]')"
else
  loaderVersion="$(jq -r --arg target "$Target" '.[] | select(.id==$target) | .compatible.loader_version' "$matrix")"
  ae2VersionId="$(jq -r --arg target "$Target" '.[] | select(.id==$target) | .compatible.ae2_version_id' "$matrix")"
  ae2Version_json="$(curl -fsS "$api/version/$ae2VersionId")"
fi
ae2Version="$(echo "$ae2Version_json" | jq -r '.version_number')"
runtimeArgs=("-P$LoaderProperty=$loaderVersion" "-P$Ae2Property=$ae2Version" "-PruntimeRunDirectory=$run")
if [ "$Latest" -eq 1 ]; then echo "profile latest"; else echo "profile compatible"; fi
echo "runtime loader $loaderVersion"
echo "runtime ae2 $ae2Version"

if [ "$Target" = "1.20.1-fabric" ]; then
  if [ "$Latest" -eq 1 ]; then
    fabricApiVersion_json="$(curl -fsS "$api/project/P7dR8mSH/version?game_versions=%5B%22$Game%22%5D&loaders=%5B%22$Loader%22%5D" | jq -c '.[0]')"
  else
    fabricApiVersionId="$(jq -r --arg target "$Target" '.[] | select(.id==$target) | .compatible.fabric_api_version_id' "$matrix")"
    fabricApiVersion_json="$(curl -fsS "$api/version/$fabricApiVersionId")"
  fi
  fabricApiVersion="$(echo "$fabricApiVersion_json" | jq -r '.version_number')"
  runtimeArgs+=("-PruntimeFabricApi1201Version=$fabricApiVersion")
  echo "runtime fabric-api $fabricApiVersion"
fi
if [ "$Target" = "1.21.1-neoforge" ]; then
  runtimeArgs+=("-PruntimeAe2NeoForge1211Group=org.appliedenergistics")
  runtimeArgs+=("-PruntimeLatestNeoForge1211")
  echo "runtime ae2 group org.appliedenergistics"
fi
if [ -n "$DriverScenario" ]; then
  if { [ "$Target" != "1.20.1-forge" ] && [ "$Target" != "1.20.1-fabric" ]; } || [ -z "$DriverOutputDirectory" ] || [ -z "$DriverWorld" ]; then
    echo "Test-driver scenarios require Forge or Fabric 1.20.1, an output directory, and a disposable world" >&2
    exit 1
  fi
  if [ "$Latest" -eq 1 ]; then driver_profile="latest"; else driver_profile="compatible"; fi
  runtimeArgs+=("-PtestDriverScenario=$DriverScenario" "-PtestDriverProfile=$driver_profile"
    "-PtestDriverOutput=$DriverOutputDirectory" "-PtestDriverWorld=$DriverWorld")
  [ "$Interactive" -eq 1 ] && runtimeArgs+=("-PtestDriverInteractive=true")
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

if [ "$Target" = "1.20.1-forge" ] || [ "$Target" = "1.20.1-fabric" ]; then
  mod_version="$(sed -n 's/^modVersion=//p' "$root/gradle.properties" | head -n 1)"
  [ -n "$mod_version" ] || { echo "Missing modVersion in gradle.properties" >&2; exit 1; }
  driver_name="ae2-crafting-time-$mod_version-$Loader-1.20.1-test-driver.jar"
  "$root/gradlew" ":$Module:testDriverJar" "${runtimeArgs[@]}" "${GradleArgs[@]}"
  driver_artifact="$root/build/test-driver/$driver_name"
  [ -f "$driver_artifact" ] || { echo "Missing exact test-driver artifact $driver_artifact" >&2; exit 1; }
  for file in "$mods"/ae2-crafting-time-*-$Loader-1.20.1-test-driver.jar; do
    [ -e "$file" ] || continue
    [ "$(basename "$file")" = "$driver_name" ] || rm -f "$file"
  done
  cp -f "$driver_artifact" "$mods/$driver_name"
  managed+=("$driver_name")
  echo "mod $driver_name"
fi

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
