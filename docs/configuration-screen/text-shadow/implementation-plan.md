# Mod Text Shadow Plan

Lifecycle and criteria: [specification](spec.md). Source map: [technical design](technical-design.md).

## 1. Add the client option

Add `TEXT_SHADOW` / `textShadow` to `OptionFeature` as Client / Appearance. Reuse default-on feature storage, copy/reset, and TOML serialization. Add matching English and Ukrainian labels. Include the feature in Appearance pagination and section reset, adjusting appearance input indices and preserving validation when the screen rebuilds (A1, A3).

## 2. Connect rendering

Read the setting independently of server profiling through `ClientOptionsRuntime`. Update both table adapters and every owned direct draw in the design's surface map. Preserve the original shadow flag for native table lines, use the setting in the scaled branch, and leave layout/colors/backgrounds unchanged (A2, A4). Recheck all production text draw callers and both source families before review.

## 3. Add focused regression coverage

Extend existing core option/config tests for default On, missing/malformed values, Off save/load, copy isolation and reset. Cover both option values against both native shadow inputs and mod ownership in a small shared policy test if a pure decision is introduced. Keep shared line/branch coverage at 100% using the existing JUnit/JaCoCo setup (A2-A4).

Review the Appearance adapter's mixed row indexing, validation, draft/cancel behavior, section reset, and pagination; add a focused boundary test in the existing test home where practical. State any unexercised UI boundary explicitly. Validate locale key/placeholder parity (A1, A3, A5).

## 4. Deliver and check

Follow the repository's branch and hook ordering: review the implementation first, then create one conventional commit whose hook opens the PR. Run no local tests before that PR exists.

After PR creation, run the selected option/config/policy tests plus shared JaCoCo verification/report. Build the four affected modules (`mc_1_20_1_forge`, `fabric_1_20_1`, `mc_1_21_1_neoforge`, `mc_26_1_2_neoforge`) using their existing Gradle tasks. Inspect current-head GitHub tests, coverage, and build checks separately. Bind every result to the implementation head (A5).

Do not launch Minecraft, run prepared-client smoke, or start CodexVM in this run. The user excluded smoke. Retain the issue's visual acceptance criterion unchanged and report A6 as unverified for this run. Record the visual follow-up for later verification without treating this run's omission as a permanent blocker to issue closure. A future authorized visual pass should establish both shadow settings at supported GUI scales on all four targets; check its profiles and launch prerequisites before execution.

## Evidence and completion

| Criteria | Evidence for this run |
| --- | --- |
| A1 | Appearance row/reset/validation review and any focused adapter checks; identify UI limits |
| A2 | Shadow decision tests plus complete draw-call/counterpart review |
| A3 | Config/model regression tests and session/reset adapter review |
| A4 | Native-input tests and review of client-only access and unchanged layout/color code |
| A5 | Locale validation, four-target builds, shared coverage and current-head CI |
| A6 | Deferred; no smoke or readability evidence in this run |

Deliver the reviewed implementation and report the actual automated results and deferred visual criterion. Do not claim A6 passed without visual evidence; record any follow-up needed without making this run's deferred smoke a permanent closure condition.
