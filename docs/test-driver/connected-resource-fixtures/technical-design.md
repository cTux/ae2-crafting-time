# Connected resource fixture design

Tracks [#482](https://github.com/cTux/ae2-crafting-time/issues/482).
Source baseline: `b94d1a4264d0d299945178fb542d4afcc8ba2617`.
The [specification](spec.md) defines the boundary; this design defines the new
driver-only control contract. Nothing below changes a production protocol.

## Existing seams and ownership

`scripts/run-connected-dedicated-ui-smoke.ps1` validates source/launch/dependency
hashes, copies a schema-2 marked source, starts one loopback server and client,
archives evidence and owns process cleanup. It currently admits only CPU-list
and recurrence scenarios. Preserve those paths and add the two resource leaves.

The shared `DedicatedCpuScenario` dispatches CPU-list and recurrence branches;
26.1.2 compiles that shared dispatcher with its own `StandardCraftFixture` and
`ServerDriverPlatform`. Delegate the new resource branch to a driver-only
resource lifecycle class rather than copying the whole dispatcher. The shared
client scenario owns observation, ordinary locate input, screenshots and normal
disconnect/reconnect. It uses the same fixture operations on the integrated
server thread; connected mode invokes them through the control below.

`CpuListTtcControl` supplies directory/epoch and disposable-marker validation;
`RecurrentPlanControl` demonstrates bounded properties files and recipient-aware
acknowledgements. Add a small resource-specific control class, reusing the atomic
move/retry utility, without changing either existing wire contract. Pure parsing
and transition decisions stay Minecraft-free in the driver and its existing
core-test source set. No client classes may initialize on the dedicated server.

## Typed processing fixture

Extend the existing grid/CPU/provider setup seams with a resource fixture. Encode
one cobblestone input per output batch with `ServerDriverPlatform.processingPattern`;
overlap slot 1 uses one sand instead so accepted inputs identify each output.
An adjacent inventory accepts the real provider dispatch; record and consume
that input once before marking the matching output held. Release inserts the
retained `AEKey` through normal ME storage/crafting insertion, like the current
`StandardCraftFixture.pumpDelayed` path. Retain partial insertion remainder and
never synthesize a second output on a retried action. Fail on wrong key/count.

Each batch produces one item or one resource unit from `AEKey.getAmountPerUnit()`;
record the raw amount, which differs between Forge and Fabric fluids. Use an
item cell plus the appropriate fluid/chemical cell, and two native 256K CPUs
for overlap. Ordinary cases use only one CPU. Warm up each recipe by completing
two real batches before the measured held job, returning each batch 20 server
ticks after its dispatch; clear prior case samples first.
This establishes throughput without seeding delay state or sample maps.

Use AEItemKey stone, AEFluidKey water/lava, and target-local AppMek MekanismKey
oxygen/hydrogen. Forge uses GasStack; NeoForge 1.21.1 uses ChemicalStack. Optional
fixture factories load only after the selected scenario and mod presence checks.
Fabric/26.1.2 contain no Mekanism references in common initialization. Existing
`appmek-cpu` remains unchanged.

For a deterministic bucketless control, register the driver-only source fluid
`ae2craftingtime_test_driver:resource_fixture_fluid` on Forge 1.20.1 on both sides,
with no bucket or placed block and an explicit cyan tint over the water sprite.
It enters no world generation or production artifact. Use the same processing
flow and record its key, no-bucket assertion and appearance metadata. This
resolves the previously unnamed fixture without adding a player dependency.

## Resource control protocol

Use fixed `resource/command.properties` and `resource/state.properties` below
the runner's fresh control directory; no command contains a path, registry ID,
quantity, code, address or arbitrary coordinates. Atomic temporary-file replace
publishes each file. Limit each file to 64 KiB on reads and writes, rejecting
links, non-regular files and paths outside that control child. Read at most the
limit plus one byte so a size race cannot bypass the bound. No raw file dump in
errors. Missing files mean pending only before publication; invalid files fail.

Command schema `1` has exactly: `schema`, `epoch`, `scenario`, `player`,
`fixture`, `revision`, `sequence`, `action`, `case`, `slot`. Epoch/fixture are
runner/server-generated UUIDs, player is the fixed offline fixture UUID,
revision and sequence are positive signed longs, slot is 0 or 1, and enum strings
are at most 64 ASCII characters. Reject duplicate/unknown properties, bad numbers
and missing fields. `case` is one of `item`, `water`, `lava`, `bucketless`,
`fluid-overlap`, `oxygen`, `hydrogen`, `chemical-overlap`, with the target/scenario
restrictions from the spec. For actions without an output slot, require slot 0.

The initial state publishes fixture identity and revision 1. All commands must
match epoch, scenario, expected player, fixture and current revision. Except
for `create`, case must match the current server-owned case. Accept exactly
`lastAck + 1`; an identical decoded repeat of the last accepted command returns
its recorded acknowledgement without repeating work, including a reset's old
revision. Check that exact replay before the current-revision check. Reject conflicting
reuse, older or skipped sequences and wrong phases. Keep one in-flight action;
retries while its asynchronous operation runs reuse that operation. Ack only
after its postcondition holds. No sequence/revision wraparound is permitted.

| Action | Allowed state | Postcondition / next state |
| --- | --- | --- |
| `create` | READY or CLEAN | Prepare the selected case and warmup; submit measured job(s), observe real input dispatch; hold output / HELD |
| `release` | HELD | Release the selected active slot once; wait for completion; HELD if another slot remains, otherwise SETTLED |
| `cancel` | HELD | Cancel selected native CPU, discard its held output; wait for idle; HELD or SETTLED |
| `rejoin-prepare` | HELD | Record current session and arm one disconnect/reconnect; keep job held / REJOINING |
| `reconnect` | REJOINING | Require observed leave and return of the same player on a new connection; verify job unchanged / HELD |
| `reset` | SETTLED | Remove case jobs/inputs/patterns/samples and restore settings; increment revision / CLEAN |
| `complete` | CLEAN | Validate all mandatory case receipts, cleanup and final capture acknowledgement / COMPLETE |

After reset the acknowledgement names the accepted old revision and state carries
the new revision. State properties are `schema`, `epoch`, `scenario`, `player`,
`fixture`, `revision`, `ack`, `ackRevision`, `action`, `case`, `slot`, `phase`,
`failure`, `terminal`, `providers`, `jobs`. Terminal is an integer x/y/z triple;
providers is a JSON array of at most two such triples. Jobs is a JSON array of
at most two records:
slot, resource type/ID, key fingerprint, raw amount/held/released amounts, CPU/job
identity, dispatch count, busy/delayed flags and server tick. Serialize each
fixture key once with the target's AE2 generic-key encoding and SHA-256 those
immutable bytes for its fingerprint; retain the encoding and hash in evidence.
Never accept key data from the client. Terminal
positions are observations, not client-selected mutation targets.

Use the existing Ae2ctAlpha offline identity for these leaves. Match its UUID
and scenario on the server before preparing anything. Unexpected recipients
cannot advance the protocol. Menus may be closed, so bind actions to fixture/job
revision rather than menu IDs. The server tracks the actual leave/rejoin event;
a client-written reconnect command alone never proves reconnect.

## Launch, result and failure contract

Add explicit `-ResourceFixtureOnly` to the existing integrated/connected runner
entry points and propagate `ae2craftingtime.test.resourceFixtureOnly=true` to
the client and server. Permit it only with the two resource leaves. #482 admits
these leaves only in that explicit mode; ordinary icon-acceptance runs remain
unavailable until #376 supplies its assertions. Existing scenarios reject the
flag. No normal suite silently uses fixture-only mode.

Write `resource-fixture-evidence.json` schema 1 on both sides with
`fixtureResult: PASS|FAIL` and `productionIconAcceptance: NOT_RUN`. Include
SHA/graph/artifact identities, command receipts, server job facts, actual client
plate/output observations and screenshot names/hashes. The runner requires this
evidence in addition to existing driver/server results and preserves
`fixture-only` scope in its summary/archive. A known absent icon is recorded;
missing captures, absent delayed plates, incorrect lifecycle or failed cleanup
still fail. No diagnostic mode writes a full icon-acceptance PASS.

Preserve 20-second callback and 60-second active-progress watchdogs; allow up to
300 seconds for fixture setup and 40 minutes overall, with real case/phase
progress. Warmup/dispatch delays cannot manufacture progress indefinitely.
No automatic retry once a resource command has been accepted. Preserve existing
bounded pre-world startup retries only when no resource fixture/command exists
and the previous exact client and tasks have exited.

On failure, record the original error, cancel outstanding fixture jobs and
calculations, discard held outputs, remove forced chunks/blocks, restore settings,
then report cleanup outcome. Integrated cleanup runs on its server thread;
dedicated cleanup precedes halt. The runner still owns exact PID/start-time and
scheduled-task cleanup when callbacks fail. Preserve evidence and the immutable
source; do not broaden deletion or process targets. Crash cleanup may rely on
disposing the marked copy, but must record that live teardown did not run.
