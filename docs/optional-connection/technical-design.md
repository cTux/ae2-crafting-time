# Optional connection design

See the [scope and status](spec.md). Investigation base:
`37bfb5430d6dc0392aa7ba03ce02973a91707be8`.

## Current failure paths

Each target owns `versions/<target>/src/main/java/com/ctux/ae2craftingtime/mc1201/StatsNetwork.java`.
Forge uses protocol `23` with strict equality predicates. Both NeoForge targets
use required registrar `22` and directional play registrations. Fabric registers
twelve named channels; only three server sends and the CPU-request caller check
availability. These source facts establish missing guards, not a runtime matrix pass.

There are six payloads each way. Client requests originate in `ClientStatsRequests`,
`CpuTtcRequests`, `StatsChatMessages`, `ProviderLocateClick`,
`ClientOptionsRuntime.syncWarningPreference` and `OptionsSession`. The first two
bypass `StatsNetwork` on Forge and NeoForge.

Server replies originate in stats/CPU/locate handlers. Unsolicited traffic comes
from login and settings synchronization (`ServerOptionsRuntime`), provider
highlighting (`ProfilerBridge` and `DelayedNotificationServer`),
`CraftConfirmMenuMixin` including the Forge SRG counterpart, and
`StoredVariantMenuState`. All server custom sends use `StatsNetwork`.

`shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/ClientOptionsRuntime.java`
enables features when the server-options snapshot is absent. That absence currently
means both waiting for a snapshot and no supporting server. It is not a capability.
The confirm/status table mixins also read `COMPACT_STATUS_AMOUNTS` directly from
local configuration, bypassing the effective feature check.

Logout resets server options and some CPU/highlight state. `ClientStats.CACHE`,
its private network-amount map and request cooldowns lack a common session reset.
Recurrence and stored-variant client state belong to the active menu.

## Smallest shared correction

Use each loader's negotiated channel information, without a new probe payload.
Forge must accept the loader's absent-channel sentinel while retaining exact
compatibility for present peers. NeoForge must register these payloads as optional.
Fabric must check the exact channel being sent. Verify API signatures against the
resolved loader source before editing; preserve platform distinctions.

Route every outbound path through guarded network adapters, including stats and
CPU requests that currently bypass them. Check support before encoding or sending.
Server guards are per recipient; one unsupported client must not suppress another
client's updates or server profiling.

Expose connection support to the shared client runtime. Keep any independently
testable compatibility and state decisions in `shared/src/main/java`, with thin
Minecraft/loader adapters. Effective feature checks must require support, and
direct renderer preferences must use those effective checks. Do not change saved
preferences to implement fallback. CPU support must use the same connection fact.

Reset client connection state at login and disconnect through one shared lifecycle
entry. Include stats, network amounts, CPU cache/order, highlights, server options
and request cooldowns. Menu-owned state dies with its menu. Work queued by packet
handlers must remain tied to the receiving connection; discard old-connection
callbacks after a disconnect or server switch. Re-evaluate negotiated availability
when the loader has established it; never persist an unsupported result from an
earlier login phase forever. Login warning preferences must be sent only when safe.

Server `ProfilerBridge` collection, persistence and existing authorization remain
independent of client support. Wire fields and NBT do not change. Chat delivered
through native Minecraft is distinct from custom payload traffic; the matrix must
check that unsupported clients are not offered broken custom client actions.

## Verification boundary

The existing connected runner requires both a production JAR and the test-driver
JAR. Its scenario allowlist has no installation-mode scenario. Forge and Fabric
test-driver metadata require the exact production mod. Removing the production
JAR while keeping this driver is not a valid absent-mod test.

Reuse the four prepared native clients, sealed dedicated-server provisioning and
existing test-driver artifact. The issue's requested matrix includes the focused
test-tool changes below. No standalone mod, network protocol or general test
framework is needed.

### Focused connection observation mode

Add a system-property-selected observation mode to the existing driver. Leave its
normal scenarios unchanged. In this mode, do not start automatic UI navigation or
resource scenarios. In Forge's `TestDriverMod`, skip the otherwise unconditional
`ResourceFixtureFluid.register`: those custom registry entries would make the
observer itself incompatible with a driver-absent peer. Use only native AE2,
items and fluids in the disposable fixture. Inspect driver metadata and all other
registrations for the same requirement on every target.

Use test-driver mixins into `StatsNetwork` to record entry attempts and the actual
loader send invocation reached after the production guard. The latter is the
assertion boundary: Forge `SimpleChannel.send/sendToServer`, Fabric
`ServerPlayNetworking.send/ClientPlayNetworking.send`, and NeoForge packet
distributor sends. Count packet type, direction and connection/recipient separately.
After request consolidation no bypass may remain. Bind injections to exact
invocation descriptors with required injection counts so a missing observation
seam fails loading rather than silently returning zero. Hooks must not cancel,
replace or approve production sends.

Write a local receipt with target, tested artifact identities, connection epoch,
observer-ready event, bounded activity/flush markers, attempted counts and actual
send counts. A missing or stale receipt fails. Exercise every outbound overload
against the connected unsupported peer from a local driver probe, using valid
bounded payloads; actual sends must be zero. Keep the probe out of production.
Both-installed control traffic must produce nonzero actual counts, qualifying
the observer; existing scenarios cover server authorization and packet semantics.
Do not count entry to a guarded wrapper as transmission. Add deterministic receipt
validation and require all twelve packet types in the source/bytecode seam audit.

Only installed sides need observation: they are the only processes capable of
emitting Crafting Time payloads. Absent sides omit both production and driver.
For neither-installed, retain complete inventories on both sides, successful
login and native AE2 screen/craft evidence; there is no Crafting Time sender to
instrument. Never label an absent observer's missing file a zero-send receipt.

### Native UI and fixture procedure

Use VNC screenshots and input for absent clients, and the same manual checkpoints
for installed clients in observation mode. Capture maximized English plan/status
screens with fixed GUI settings, including totals, rows, amounts and controls.
Compare client-only to neither-installed using the same native AE2 fixture and
configuration. Existing UI scenarios remain the supported-feature regression
checks; manual captures are reviewed evidence, not automatic passes.

Prepare the native AE2 grid once in a report-owned disposable world using existing
fixture construction with both sides installed. Stop cleanly, verify the world
contains no driver-specific registry content, then clone that baseline for the
four cells. Never copy a running world. Absent-server cells load only native
dependencies; they need neither driver control files nor profiler state.
Server-only history is demonstrated by a real craft and a later supported client
reading the saved history after restart.

Extend only the existing connected runner's installation staging and observation
mode. Select client/server installation independently, retain per-side inventories,
launch native prepared clients without driver scenario arguments on absent sides,
and use explicit manual-checkpoint receipts plus bounded waits instead of waiting
for nonexistent driver endpoints. Reuse source seals, Java checks, loopback-only
binding, report ownership, launch/stop handling and artifact hashing. Add a
validated explicit guest-address option to existing SSH dispatch if Tools lookup
remains unavailable; retain normal lookup as the default.
