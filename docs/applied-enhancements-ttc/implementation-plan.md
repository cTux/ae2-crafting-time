# Implementation plan

Status and acceptance: [spec](spec.md). Design: [technical design](technical-design.md).

1. Add covered shared AelisPlanTtc quantity, layout and partial-total logic.
2. Register the Forge-only released API contract and optional screen/layout
   mixins. Reuse existing requests, display options, colors and badge helpers.
3. Add a focused aelis-plan fixture and assertion/screenshot contract. Cover
   contract selection, absent/wrong API/side/target, and remapped presentation.
4. Document behavior and limits in dependencies, English/Ukrainian GuideME and
   wiki text. Keep source/API and runtime support claims separate.
5. Review the full change; commit before local tests so the hook updates the PR.
   Run shared coverage and focused adapter tests, runner checks for changed
   dispatch rules, enabled-AELIS smoke, native controls and absent-addon startup.
   Inspect screenshots and archive failures as well as passes. Keep unrun
   acceptance cells explicit. No merge or release without separate authorization.
