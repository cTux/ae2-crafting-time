#!/usr/bin/env bash
set -euo pipefail

for c in jq curl; do
  command -v "$c" >/dev/null 2>&1 || {
    echo "Required command '$c' not found. Install it (e.g. 'sudo apt-get install $c') and retry." >&2
    exit 1
  }
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="$script_dir/run-client.sh"

temp="$(mktemp -d)"
bin_dir="$(mktemp -d)"
trap 'rm -rf "$temp" "$bin_dir"' EXIT

sha="$(printf 'test mod' | sha512sum | awk '{print $1}')"

cat > "$bin_dir/curl" <<'CURL_EOF'
#!/usr/bin/env bash
set -euo pipefail

if [ "${2:-}" = "-o" ]; then
  out="$3"
  if [ -n "${AE2CT_BAD_DOWNLOAD:-}" ]; then
    printf '\x00' > "$out"
  else
    printf 'test mod' > "$out"
  fi
  exit 0
fi

url="${2:-}"

case "$url" in
  *minecraftforge*)
    cat <<'XML'
<metadata><versioning><versions><version>1.20.1-1</version><version>1.20.1-99</version></versions></versioning></metadata>
XML
    exit 0
    ;;
  *fabric-loader*)
    cat <<'XML'
<metadata><versioning><versions><version>0.1.0</version><version>0.99.0</version></versions></versioning></metadata>
XML
    exit 0
    ;;
  *neoforged*)
    cat <<'XML'
<metadata><versioning><versions><version>21.1.1</version><version>21.1.99</version><version>26.1.2.1</version><version>26.1.2.99</version></versions></versioning></metadata>
XML
    exit 0
    ;;
esac

project="$(printf '%s' "$url" | sed -E 's#.*/project/([^/]+)/version.*#\1#')"
version="1.0.0"
older_version="0.0.1"
filename="$project.jar"
older_filename="$project.jar"
vtype="release"
case "$project" in
  XxWD5pD3)
    case "$url" in
      *26.1.2*) version="26.99.0-beta"; vtype="beta";;
      *1.21.1*) version="19.99.0";;
      *) version="15.99.0";;
    esac;;
  P7dR8mSH) version="0.99.0+1.20.1";;
  udZtKfzP)
    version="20.4.2"; older_version="20.3.0"
    filename="$project-20.4.2.jar"; older_filename="$project-20.3.0.jar";;
esac

sha="${AE2CT_TEST_SHA512:-}"

cat <<JSON
[
  {
    "version_type": "$vtype",
    "version_number": "$version",
    "dependencies": [
      { "project_id": "XxWD5pD3", "version_id": "", "dependency_type": "required" }
    ],
    "files": [
      {
        "filename": "$filename",
        "hashes": { "sha512": "$sha" },
        "url": "https://example.invalid/$project.jar",
        "primary": true
      }
    ]
  },
  {
    "version_type": "release",
    "version_number": "$older_version",
    "dependencies": [],
    "files": [
      {
        "filename": "$older_filename",
        "hashes": { "sha512": "$sha" },
        "url": "https://example.invalid/$project.jar",
        "primary": true
      }
    ]
  }
]
JSON
CURL_EOF
chmod +x "$bin_dir/curl"

export AE2CT_TEST_SHA512="$sha"
export PATH="$bin_dir:$PATH"

assert_line() {
  local text="$1" expected="$2"
  if ! printf '%s\n' "$text" | grep -qxF "$expected"; then
    echo "Missing exact line '$expected' in output:" >&2
    echo "$text" >&2
    exit 1
  fi
}

cases=(
  "1.20.1-forge|runtime loader 1.20.1-99|runtime ae2 15.99.0|mod udZtKfzP-20.3.0.jar|mod ArHeh5Fz.jar"
  "1.20.1-fabric|runtime loader 0.99.0|runtime fabric-api 0.99.0+1.20.1"
  "1.21.1-neoforge|runtime loader 21.1.99|runtime ae2 19.99.0|runtime ae2 group org.appliedenergistics"
  "26.1.2-neoforge|runtime loader 26.1.2.99|runtime ae2 26.99.0-beta"
)

IFS='|'
for case in "${cases[@]}"; do
  read -ra parts <<< "$case"
  target="${parts[0]}"
  output="$("$script" -Target "$target" -Root "$temp" -ResolveOnly 2>&1)"
  assert_line "$output" "${parts[1]}"
  assert_line "$output" "${parts[2]}"
  if [ -n "${parts[3]:-}" ]; then
    assert_line "$output" "${parts[3]}"
  fi
  if [ -n "${parts[4]:-}" ]; then
    assert_line "$output" "${parts[4]}"
  fi
done
unset IFS

managed_dir="$temp/versions/1.20.1-forge/run/resolved-mods"
rm -f "$managed_dir/Ck4E7v7R.jar"
export AE2CT_BAD_DOWNLOAD=1
if out="$("$script" -Target "1.20.1-forge" -Root "$temp" -ResolveOnly 2>&1)"; then
  echo "Expected a hash mismatch" >&2
  echo "$out" >&2
  exit 1
fi
if ! printf '%s\n' "$out" | grep -q 'Hash mismatch for'; then
  echo "Expected 'Hash mismatch for' in output:" >&2
  echo "$out" >&2
  exit 1
fi
unset AE2CT_BAD_DOWNLOAD

echo "run-client checks passed"
