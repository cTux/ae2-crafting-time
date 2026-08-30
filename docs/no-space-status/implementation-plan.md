# No Space Status Implementation Plan

1. Add `NO_SPACE` to the covered pure-Java row-state resolver and test every
   stored, active, pending, and menu-flag boundary.
2. Read `CraftingCPUScreen`'s current `isCantStoreItems()` value directly in the
   status-table description and tooltip paths, with a safe false fallback.
3. Add `NO SPACE`, its explanation, and its suggestion to `TtcText` and matching
   English and Ukrainian translation keys.
4. Render the visible warning through the existing compact badge without
   changing AE2's title warning.
5. Verify no request, packet, compatibility, or saved-data file changed.
6. After the hook-created PR exists, run the development skill's relevant
   shared text/decision checks and all required target checks. Verify GitHub CI
   separately.
7. In development clients, fill writable ME storage, finish or cancel a CPU
   with retained contents, hover the stored row, then free space and confirm
   the status disappears without reopening the screen.

Complete when every acceptance criterion in `spec.md` has automated or recorded
cross-version evidence, all required checks pass, and no repository-owned
warning remains.
