# Initial Crafting Status ETA Implementation Plan

1. Add a covered pure helper that distinguishes zero progress from measurable
   progress using AE2's start and remaining counters.
2. Use it in the existing mc1201 Crafting Status title hook to remove the native
   ETA before progress while preserving the can't-store warning and total TTC.
3. Cover equal, progressed, and defensive reversed counters in the shared unit
   test.
4. Run the repository Gradle tests and coverage gate after the implementation
   PR exists, then run the changed-scope prepared-client smoke and inspect the
   initial Crafting Status capture.
5. Review the final diff and current-head CI, merge the implementation PR, and
   verify issue #350 closes.
