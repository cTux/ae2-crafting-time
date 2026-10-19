# Bash client jq boundary

[Scope and status](spec.md)

## Current path and failure

At base `f9521feb92b95a63992d8480add726ca8b61ced9`,
`scripts/test-run-client.sh` reads matrix target IDs with `IFS= read -r target`
and passes each to `scripts/run-client.sh`. Native Windows jq emits CRLF;
`read -r` removes LF but retains CR. The launcher's exact target comparison
rejects the resulting value. Command substitution and `mapfile -t` also retain
that CR. The issue and [merged PR #741](https://github.com/cTux/ae2-crafting-time/pull/741)
record the failure and a successful temporary `jq.exe -b` workaround.

The self-test invokes the shared launcher for all targets and both profiles,
then checks error paths. The launcher resolves matrix and API dependencies,
verifies downloaded hashes, reconciles managed files, installs the exact driver,
and optionally starts Gradle. No other executable Bash-launcher caller was found.

## Affected reads

| Location in `scripts/run-client.sh` | Values consumed |
| --- | --- |
| Lines 114-121 | Replaced/project IDs and compatible version pins |
| Lines 127-130, 283-286 | Legacy and current managed filenames |
| Lines 166-168 | Download filename, URL and expected hash |
| Lines 203-232 | Required/matrix dependency IDs, CurseForge file IDs and filenames |
| Lines 238-255 | Loader, AE2 and Fabric API IDs/version strings |
| Compact JSON reads | Objects and exact `null` sentinel comparisons |

Line numbers refer to the base above. The self-test also reads jq output for
target enumeration and creates its temporary matrix.

## Smallest fix

Request binary output consistently at the jq invocation boundary in both client
scripts on Git Bash, covering every invocation rather than trimming each
consumer. Leave Linux invocations unchanged: although the available Linux jq
1.8.1 accepts `-b --version`, the [upstream jq 1.6 option parser](https://github.com/jqlang/jq/blob/jq-1.6/src/main.c)
has no binary option. Unconditionally adding `-b` would silently raise the Linux
requirement. Native Windows jq 1.8.2 advertises `-b` and is the verified affected
environment. Cover the platform distinction and an LF-producing jq fixture
that rejects `-b`; do not add a jq minimum for Linux.

Reuse a small shared boundary only if it reduces duplication; do not introduce
a general command framework or a user configuration requirement.

Prevent text-mode newline conversion without deleting CR characters from actual
JSON values. Preserve jq's arguments, standard input, standard error and exit
status. Keep existing `set -euo pipefail` behavior and validation. Failure inside
existing process substitutions is not a new error-handling redesign.

Use the existing self-test's temporary directory and command-fixture pattern for
the regression. Do not touch real client directories or call remote APIs. The
test must verify externally consumed values, not merely inspect source text.
