# UI smoke evidence

Keep screenshot evidence for named modpacks and prepared version clients using
the same archive layout:

For prepared Forge clients, use `scripts/invoke-ui-smoke-codexvm.ps1 -Scenario suite`
for the full compatible graph and its 25 implemented scenarios.

For multiple scenarios on the same installed mod graph, launch Minecraft once.
Run the suite sequentially with a fresh disposable world per case, capturing each
case's screenshots before advancing. Retain the suite plan, one process ID,
ordered timestamps, and overall result alongside the per-mod evidence. A crash
or failed case leaves later cases `NOT_RUN`; do not hide it with automatic retries.
Different mod graphs or incompatible original/fork artifacts require separate runs.

```text
E:/games/mc-instances/.codex-test-results/ui-smoke/
  <modpacks|clients>/<pack-release-or-target>/<UTC-run-id>/
    report.md
    <mod-id>/<scenario>/<attempt>/
      <checkpoint>.png
      result.json
      latest.log
```

Use a new timestamped run directory; never replace an earlier run or failed
attempt. Runtime `build/ui-smoke` folders are temporary, not the final archive.
Copy the evidence before cleaning a runtime or removing a worktree.
For a single-launch suite, keep shared client logs once in `logs/` and link them
from every case's record instead of copying the same log for each integration.

Capture each distinct UI checkpoint for every tested integration: its screen,
TTC row and total, tooltip, sort modes, and post-craft result where applicable.
Map every requested check in `report.md` to its screenshot and semantic result.
A frame may support several checks only when it visibly shows each one. For a
server-only check such as a new profiling sample, keep the structured assertion
and a screenshot of the resulting UI; do not claim the image proves server state.

Record pass, fail, blocked, or not tested for every requested integration point.
Capture the current failure screen when possible. Never reuse another run's
image as evidence or mark missing driver coverage as a pass.

Record the exact pack/project/release or client target/profile, Minecraft and
loader versions, enabled mod inventory, tested commit, production and driver
JAR hashes, scenario, attempt, and timestamps. Preserve logs and result files
beside the screenshots, but exclude account data, tokens, and unrelated worlds.
Inspect the saved images and link the archive report in the final response.
