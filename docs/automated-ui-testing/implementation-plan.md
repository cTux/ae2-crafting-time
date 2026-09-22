# Automated UI Testing Implementation Plan

Lifecycle: see scope status and evidence for [base workflow](spec.md) and [unattended qualification](spec.md#unattended-evidence-gate).

The runner, evidence gate, shared-world suites, change selection, and prepared
client qualification are complete. Their current contracts live in the
[specification](spec.md) and [technical design](technical-design.md); merged pull
requests retain the implementation history.

## Enforce newest-adapter and English-only smoke policy

Implement [SP-01 through SP-04](spec.md#smoke-policy):

1. Reuse the adapter catalogue and selection snapshot to choose required direct
   cases per target. Record older adapters as policy skips. Add a focused newest
   adapter fixture only when compatible pins exercise an older contract.
2. Set and verify `en_us` in the shared and 26.1.2 drivers. Remove language
   switching and Ukrainian screenshot requirements while keeping static English
   and Ukrainian translation checks.
3. Cover selection mismatch, older-only graphs, newest-fixture setup failure,
   and language enforcement with the existing runner and driver tests.
4. Run the required newest-adapter smoke in English on applicable targets. Record
   selected IDs, artifacts, language, screenshots, and policy skips.

Done means every required target verifies its newest supported adapter in
English, older variants keep non-smoke coverage, and no duplicate language or
old-adapter campaign remains in the completion gate.
