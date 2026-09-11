# Active-order TTC sorting technical design

Implements the planned [specification](spec.md) for
[#387](https://github.com/cTux/ae2-crafting-time/issues/387).

## Research result

Research performed on 2026-09-11 against repository commit
`4dbd2c5b5b7b9b5709a780db397e1a47203308fa` and the upstream releases below.
This is source-level research, not a runtime compatibility or performance pass.

The feature is feasible without changing AE2's server list or the packet
format. Two changes are necessary: a shared display order for rendering and
hit testing, and a cache that retains independently refreshed background CPUs.
Adding only a comparator would sort mostly unknown values outside the viewport.

### Repository evidence

Paths use `com/ctux/ae2craftingtime` below each Java source root.

- `shared/src/mc1201/java/.../mc1201/mixin/CraftingCPUScreenMixin.java` and its
  `mc2612` counterpart own mode `2`, cycle `2 -> 0 -> 1 -> 2`, and sort only
  `CraftingStatusEntry` item rows through `TtcSort.copySorted`.
- Both `CPUSelectionListMixin` variants request six visible serials plus the
  selected serial at `updateBeforeRender` return. Badge drawing already looks
  up the particular row's serial.
- `shared/src/main/java/.../core/CpuTtcCache.java` caps the tracked views at 32,
  sends at most once per second, replaces the whole cache on each response,
  and expires the batch after three seconds. Rotating calls to it unchanged
  would discard earlier pages and invalidate in-flight responses on scrolling.
- `shared/src/mcCommon/java/.../mc1201/CpuTtcClient.java` binds the cache to an
  open menu. `ClientStats.totalTtcSeconds()` uses that same cache for the
  selected title, so changing cache semantics also affects the title.
- `CpuTtcPacketCodec` accepts at most 32 distinct positive serials.
  `CpuTtcRequestHandler` validates the live menu/container/grid and uses only
  existing listed CPU serials. `CpuTtcRateLimit` permits four packets and 128
  serials per player per second. Neither requires a visible-row restriction.
- The driver `CPUSelectionListObservationMixin` currently reconstructs cards
  from `menu.cpuList.cpus()`. That observation would become wrong after a
  widget-local sort; it must observe actual rendered rows instead.

### Upstream evidence

Each source below was fetched at its release commit. Minimum versions come
from module Gradle files; prepared versions come from
`scripts/run-client-versions.json`. Pins are evidence, not new dependency limits.

| Target / AE2 version | Widget source | Menu source |
| --- | --- | --- |
| Forge minimum 15.0.10 | [CPUSelectionList](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/bcdb7c040bc3badf24e3354381d6fe0fe6490592/src/main/java/appeng/client/gui/widgets/CPUSelectionList.java) | [CraftingStatusMenu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/bcdb7c040bc3badf24e3354381d6fe0fe6490592/src/main/java/appeng/menu/me/crafting/CraftingStatusMenu.java) |
| Forge prepared 15.4.10 | [CPUSelectionList](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/client/gui/widgets/CPUSelectionList.java) | [CraftingStatusMenu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/menu/me/crafting/CraftingStatusMenu.java) |
| Fabric minimum 15.0.10 | [CPUSelectionList](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/client/gui/widgets/CPUSelectionList.java) | [CraftingStatusMenu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/menu/me/crafting/CraftingStatusMenu.java) |
| Fabric prepared 15.1.0 | [CPUSelectionList](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/d6b0097eb7ba77d2de238059bcc3a53822196679/src/main/java/appeng/client/gui/widgets/CPUSelectionList.java) | [CraftingStatusMenu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/d6b0097eb7ba77d2de238059bcc3a53822196679/src/main/java/appeng/menu/me/crafting/CraftingStatusMenu.java) |
| NeoForge minimum 19.0.24 | [CPUSelectionList](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/client/gui/widgets/CPUSelectionList.java) | [CraftingStatusMenu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/menu/me/crafting/CraftingStatusMenu.java) |
| NeoForge prepared 19.2.17 | [CPUSelectionList](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/client/gui/widgets/CPUSelectionList.java) | [CraftingStatusMenu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/menu/me/crafting/CraftingStatusMenu.java) |
| NeoForge minimum/prepared 26.1.10-beta | [CPUSelectionList](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/client/java/appeng/client/gui/widgets/CPUSelectionList.java) | [CraftingStatusMenu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/menu/me/crafting/CraftingStatusMenu.java) |

All inspected widgets use six rows and read `CraftingCpuList.cpus()` in
`drawBackgroundLayer`, `hitTestCpu`, and `updateBeforeRender`. Tooltip and
`onMouseUp` both use `hitTestCpu`; clicks send `menu.selectCpu(cpu.serial())`.
The menu sorts named CPUs first, then name text, then serial, and owns selection
separately. Use its actual incoming order, not a local copy of that comparator.
The 26.1 drawing parameter is `GuiGraphicsExtractor`; older targets use
`GuiGraphics`. The list-access invocation itself is identical in these sources.

The inspected [AEBaseScreen render order](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/client/gui/AEBaseScreen.java#L264)
updates the screen before its widgets. The current title reads TTC during that
screen update, so freezing values only in the widget would be too late.

## Ownership and display flow

Keep the mode in the existing screen mixins. Add a narrow screen-to-widget
bridge through `CpuTtcClient.setSortMode(menu, mode)`; publish it on screen
initialization and every button cycle even when `status` is null. The client's
menu session starts at mode `2`. Closing the screen clears it; opening another
menu creates a new session. Do not let packet disable/clear reset the button's
mode or borrow another screen's mode.

Add a client-only `CPUSelectionListOrderMixin` in `shared/src/mcCommon`. It
owns the widget's immutable display list and wraps the invocation
`Lappeng/menu/me/crafting/CraftingStatusMenu$CraftingCpuList;cpus()Ljava/util/List;` within the three
native widget methods listed above. Use MixinExtras `@WrapOperation` with
`remap = false`; verify three matches in drawing, two in hit testing, and one
in the range update on the inspected versions. Do not
redirect the record globally or assign a sorted list back to `menu.cpuList`.
This shared mixin has no graphics argument, so the existing two drawing mixins
remain only for badge rendering. Move their duplicate open/refresh hooks into
the shared mixin to ensure one request coordinator.

At the existing screen `updateBeforeRender` HEAD, before calculating the title,
call `CpuTtcClient.beginFrame(menu)` for Crafting Status. Convert the full raw
list to `CpuView`s, observe job transitions, and freeze the valid seconds map
at one monotonic time. The title, sorter, and badges all read this map for the
frame; network callbacks update the backing cache for the next frame. Perform
the lifecycle observation and snapshot decisions in the pure cache, with the
common client only converting AE2 entries.

At the widget's `updateBeforeRender` HEAD:

1. Read the raw list and frozen values prepared for this frame, outside the
   wrapped native methods. Never use the already sorted list as the baseline.
2. Build a copy of the complete raw
   list. For mode `0` or unavailable channel, keep raw order. Otherwise use
   `TtcSort.copyPrioritizedSorted` with busy as priority, valid positive seconds
   as the key, and a zero fallback comparator. Stable sorting of the raw list
   preserves AE2 order for ties and unknowns. Idle keys are always empty.
3. Publish that display list for the frame. The native update clamps the
   existing offset against its unchanged length. At RETURN, derive priorities
   from the displayed six rows plus selected serial and ask the cache for the
   next bounded request. Keep list invalidation and request cadence separate.
4. Drawing, tooltip, and mouse-up read the same published list. Do not sort
   independently on each read. A mode/network event marks the next frame
   dirty; it does not replace the displayed order during input dispatch.

Capture the offset used by the actual draw. Wrap `Scrollbar.getCurrentScroll()`
inside `hitTestCpu` to use that captured offset, so a wheel event followed by a
click before the next render cannot target an unseen row. Suppress hits before
the first draw. The live scrollbar remains authoritative for the next frame.

Reuse raw entries, replacing the display snapshot when row data changes even
if serial order does not. Avoid repeated O(B log B) sorts on unchanged frames:
retain a revision for raw list/job changes, mode, accepted cache updates, and
expiry. Cache reads used by a comparator come from the same time snapshot;
expiry cannot change comparison results halfway through a sort.

If a row disappeared or its observed job generation changed after the last
draw, suppress that stale row's hit result until the next frame. Do not
substitute the new row at the old index. Sorting never calls `selectCpu`.
AE2 still owns automatic selection after removal and the cancel action.

## Bounded collection and freshness

Extend `core/CpuTtcCache` rather than adding another estimate cache. Its new
refresh input contains all current `CpuView`s, the ordered priority serials,
whether full-list collection is active, and monotonic time. Keep packet
`MAX_CPUS = 32`; it no longer caps the number of observed menu rows.

- In TTC modes, retain one value per current busy serial, with observed job
  generation, receive time, and expiry deadline. Track idle row identities as
  needed for validation, but no idle totals. Memory is O(the open AE2 list),
  cleared on close; there is no process-wide history of CPUs.
- Build each request from the selected serial, then visible serials, deduped
  (at most seven), followed by busy serials from a persistent round-robin queue
  until 32 entries are filled. Idle non-priority rows need no request.
- The queue is independent of TTC and scroll order. Preserve its position
  across estimates, renames, and mode direction changes. Remove departed/idle
  members; append newly busy members in raw AE2 order. Advance past visited
  members, including ones already included as priorities. Stop after one pass
  or a full packet. Never restart at the first CPU on every frame.
- Send at most one packet per 1,000 ms, with one outstanding request. Wait for
  its response or a 3,000 ms timeout before advancing again; no catch-up burst.
  A timed-out batch does not block subsequent queue members indefinitely.
  Preserve the existing four-packet/128-serial server limits and 32-entry codec.
- At least 25 background slots are available per request. For a stable set of
  `B` busy CPUs, `R = max(1, ceil(B / 25))` successful request opportunities
  cover every member even with changing priorities. This is an opportunity
  bound; delayed responses, packet loss, and continuous job churn do not have
  a promised wall-clock convergence time.
- In TTC modes set a received value's deadline to
  `receivedAt + max(3000, (R + 2) * 1000)` ms. Use long arithmetic. If `R`
  later shrinks, clamp existing deadlines to the shorter age bound; never
  extend an existing deadline when `R` grows. In AE2 mode retain only current
  visible/selected entries and clamp to `receivedAt + 3000` ms.
- Both badges and the selected title continue to read `CpuTtcClient.seconds`.
  Normal priority refresh is once a second, but its expiry bound matches the
  list cache. Large-list outage values can remain longer than three seconds;
  this deliberate change is specified, not disguised as real-time accuracy.

One request stores its exact serial set and job generations. Validate the
entire reply before installing any values: active menu session, outstanding
sequence, non-expired request, exact set, no duplicates, legal values. Replace
each requested value atomically, including explicit unknown results, while
retaining unrelated valid entries. Do not let an omitted entry preserve an old
value. For a requested serial whose generation changed in flight, discard its
result; valid unchanged members of the validated batch may still update.

Increment local generations on observed job output/amount changes, idle/busy
transitions, elapsed-time rollback, and removal/reappearance. Retain elapsed
observations for all menu rows, including off-screen ones. Replies for old
sessions, consumed sequences, or timed-out batches cannot resurrect values.
Do not claim detection of replacements invisible to AE2's client snapshot.
An AE2-mode transition prunes background results, including from an in-flight
TTC-mode request, and does not reset rate limits or sequence numbers.

## Server, protocol, and compatibility

The server still owns estimates through `ProfilerBridge.remainingJobSeconds`.
The client never reads profiler state, changes selection to fetch totals, or
requests another grid. Existing request/snapshot layouts and message IDs stay
unchanged: the server already answers any valid listed serial. No protocol
bump, persistence migration, registration change to packets, or rate increase
is necessary. Old clients retain visible-only requests; new clients can use an
older compatible server that already advertises this pair. Keep each loader's
existing handshake and Fabric channel-availability checks.

Register the new ordering mixin in the client list for all four targets. Keep
badge drawing in `mc1201` / `mc2612`; keep pure ordering and request decisions
in `shared/src/main/java`, with `mcCommon` converting AE2 data and binding the
screen. Preserve Fabric remapping of existing Minecraft-returning name hooks.
Build at minimum dependencies and verify the pinned prepared clients above;
source inspection alone does not prove Mixin application to transformed jars.

Native addon rows need no new adapter. Preserve unknown-estimate behavior and
optional dependencies; separate addon UIs and new addon support are excluded.
No locale keys change. Keep the current English/Ukrainian text and layout checks.

## Verification and alternatives

The [implementation plan](implementation-plan.md) maps C1-C7 to exact checks.
Crucially, capture serials from actual per-card render calls and compare those
with real hover/click actions. Do not use a second invocation of the production
sort helper as proof of correct rendering. Existing CPU-list expiry fixtures
with a small busy set keep their three-second expectation; add large-set cases
for the changed bound rather than weakening that coverage.

Rejected approaches:

- Sorting only the visible slice cannot bring an off-screen shortest job up.
- Reusing the one-batch cache unchanged loses previous pages.
- Raising packet limits or querying every CPU at once increases server burst
  work unnecessarily; round-robin requests use the existing security boundary.
- Patching the server comparator changes menu behavior for every client and
  makes a local display setting server state.
- Sorting independently in rendering and hit testing can select a different
  CPU when a response arrives between the two operations.
