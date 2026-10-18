# Codecov recovery design

Scope and status: [spec](spec.md).

## Evidence

At base `c20cd24cb9c1b3f4581e858c42a89007b45b837f`,
[the Tests workflow](../../.github/workflows/test.yml) has one Codecov call,
disabled by `if: ${{ false }}` and an outage comment.

[Run 37282324288](https://github.com/cTux/ae2-crafting-time/actions/runs/37282324288),
attempt 2, passed Gradle tests and JaCoCo on October 5, 2026, then failed at
08:25 UTC downloading `https://cli.codecov.io/latest/linux/codecov` with curl 35.
The missing signature file caused the subsequent integrity check to fail.
Codecov's [incident report](https://status.codecov.com/incidents/n8kwr2rr2v6v)
identifies an expired SSL certificate and records resolution at 09:15 UTC.
The matching timing supports that service outage as the original cause; the
runner log alone does not identify certificate expiry.

On October 18, HTTPS HEAD requests to the binary, `.SHA256SUM`, and
`.SHA256SUM.sig` endpoints returned HTTP 200 with TLS verification result 0.
This proves reachability from the investigating host, not a verified download
or successful runner upload.

## Flow and change

PR or master push -> Ubuntu runner and Java 21 -> existing script checks ->
`test jacocoTestReport` -> Codecov v7 with OIDC -> Linux CLI download -> GPG
signature and SHA256 checks -> coverage upload.

Remove only the outage comment and unconditional skip. Preserve the action
inputs and job permissions. The upstream
[action](https://github.com/codecov/codecov-action/blob/303a32d7a59b442fa8d48b6a1cc6825c09c847a5/action.yml)
and [wrapper](https://github.com/codecov/codecov-action/blob/303a32d7a59b442fa8d48b6a1cc6825c09c847a5/dist/codecov.sh)
show the default integrity checks; this was the resolved v7 revision inspected
during investigation, not a proposed pin.
[The Gradle build](../../build.gradle) owns report generation and shared 100%
line/branch thresholds. No Gradle, application, fixture, or loader changes are
needed. Existing TLS, signature, checksum, authentication, and upload failures
must remain visible failures rather than being suppressed.

## Verification feasibility

The existing Ubuntu workflow supplies the actual action environment and OIDC
permission. Local Windows endpoint checks cannot replace that environment;
`gpg` was not on the local PATH. No new test infrastructure is needed.

The original reported head has the uploader enabled. Its October 5 run was
within GitHub's 30-day rerun window at investigation time.
[GitHub reruns](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs)
retain the original SHA/ref, so this recovery check is distinct from validation
of the changed workflow. Mutable upstream action tags and service state can
still differ from the original attempt. Current-head CI is the final proof.

Minecraft smoke cannot exercise this workflow-only change. Review the YAML diff
and the real runner logs instead; do not add a test that merely repeats the YAML.
