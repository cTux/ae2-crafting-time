# Bash client jq output

Status: in-progress

Scope: Restore Bash client resolution and its self-test with native Windows jq.

Issue: [#743](https://github.com/cTux/ae2-crafting-time/issues/743)

Planning: [Reviewed plan](implementation-plan.md)

Implementation: [PR #745](https://github.com/cTux/ae2-crafting-time/pull/745)

Verification: The complete resolver self-test passed with Git Bash 5.3.15 and
native jq 1.8.2, and with Ubuntu WSL Bash 5.3.9 and jq 1.8.1. A disposable
copy without the launcher's jq boundary failed on a CR-contaminated project ID.
The remaining gate is review of the final PR diff and required GitHub checks
for the current head.

## Behavior to restore

`scripts/test-run-client.sh` must run in Git Bash with native Windows jq on
`PATH`, without a user-supplied wrapper. Native jq's CRLF output currently leaves
a carriage return in the first target ID, so the launcher rejects it. Other
client IDs, filenames and scalar reads have the same boundary.

Keep Linux jq behavior and existing resolver checks. This covers all four
targets in `scripts/run-client-versions.json`, both compatible and latest
profiles. It changes no dependency pins, PowerShell launchers, game code,
published artifacts or world data. Release/deploy Bash scripts are outside scope.

## Acceptance

| ID | Required result |
| --- | --- |
| JQ-1 | The complete existing resolver self-test passes with Git Bash and native Windows jq, without an external wrapper. |
| JQ-2 | The same self-test passes with Linux Bash and jq. |
| JQ-3 | Audit every jq read in both client scripts; target/project IDs, version pins, filenames, URLs, hashes and JSON sentinel comparisons receive no CR from output newline conversion. Preserve actual JSON string content. |
| JQ-4 | A focused regression exercises native-style CRLF output and ordinary LF output through the production boundary, checks representative scalar/list values, and proves jq failures still propagate. It fails against the unfixed boundary. |
| JQ-5 | Existing compatible/latest selection, dependency closure, hash validation, Java selection, driver installation and managed-file cleanup assertions remain intact. |

Completion requires reviewed implementation, passing focused and resolver checks
on both platforms, and the applicable current-head GitHub checks. Minecraft UI
smoke is unnecessary for this shell-output fix.
