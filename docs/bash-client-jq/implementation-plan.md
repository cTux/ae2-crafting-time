# Bash client jq implementation plan

[Scope and status](spec.md) · [Design](technical-design.md)

1. Recheck both scripts' jq calls and implement the smallest consistent binary
   output boundary for Git Bash, leaving Linux jq arguments unchanged. Cover raw
   and compact output, direct launcher execution and self-test execution
   (JQ-1, JQ-2, JQ-3).
2. Extend the existing deterministic test fixtures with focused CRLF/LF coverage
   through that boundary. Assert representative target/project IDs, versions,
   filenames, URLs and hashes plus nonzero jq failure propagation. Include an
   LF-producing fixture that rejects `-b` to protect older Linux jq. Show that the
   regression detects the original boundary; retain all resolver assertions
   (JQ-3, JQ-4, JQ-5).
3. Review the complete diff and `git diff --check`. Follow the repository's
   conventional commit and hook-created PR ordering before running tests.
4. Run the focused regression and `bash scripts/test-run-client.sh` using both
   environments below. Record exact commands, executable versions, revision and
   outcomes, including the original-boundary regression failure (JQ-1 to JQ-5).
5. Review current-head GitHub checks separately, address actionable feedback,
   and link final verification evidence in the canonical scope status when the
   completion gate is satisfied.

## Available verification environments

Investigation on 2026-10-19 found Git for Windows Bash 5.3.15 and native jq 1.8.2
on `PATH`. Invoke Git's `bin/bash.exe` explicitly: Windows' default `bash.exe`
is a WSL launcher. Git Bash needs the approved unsandboxed execution path on
this host because the sandbox prevents MSYS named-object creation.

Existing Ubuntu WSL has Bash 5.3.9, jq 1.8.1, curl and sha512sum, and can read the
worktree. Run its Linux tools against that checkout; no provisioning is needed.
The self-test supplies fake Java, Gradle and curl commands and disposable files,
so it needs neither installed Minecraft profiles nor network downloads.

The current GitHub test workflow does not execute `test-run-client.sh`; its
Gradle/build results cannot substitute for these two platform runs. Do not add
a broad CI matrix or run Minecraft/VM smoke for this bounded shell change.
