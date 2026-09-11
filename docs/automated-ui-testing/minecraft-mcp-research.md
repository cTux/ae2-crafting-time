# Minecraft MCP research

Research for [#382](https://github.com/cTux/ae2-crafting-time/issues/382),
checked against primary sources on 2026-09-11. Published artifacts and source
claims below are not runtime qualification: no candidate was installed, launched,
or benchmarked against AE2 Crafting Time.

## Candidate comparison

Compatibility is against the four prepared targets. `Yes` means an exact
Minecraft version and loader artifact was published; it does not mean the
artifact works with this repository's mod graph or AE2 screens.

| Candidate | Control and actions | 1.20.1 Forge | 1.20.1 Fabric | 1.21.1 NeoForge | 26.1.2 NeoForge | Installation, transport, license and maintenance | Trust boundary |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [Numen](https://github.com/Dwinovo/minecraft-numen/blob/009d1291348b656e712f76ac59b4c1766534583e/README_EN.md) | An MCP-controlled companion uses player-like movement, blocks and containers. This does not control or capture the owner's rendered AE2 GUI. | Yes | Yes | Yes | Yes | Beta `v0.1.3` releases publish the exact [1.20.1](https://github.com/Dwinovo/minecraft-numen/releases/tag/v0.1.3-1.20.1-beta), [1.21.1](https://github.com/Dwinovo/minecraft-numen/releases/tag/v0.1.3-1.21.1-beta) and [26.1.2](https://github.com/Dwinovo/minecraft-numen/releases/tag/v0.1.3-26.1.2-beta) artifacts. The mod embeds an HTTP MCP server; multiplayer also needs Numen server-side, and Fabric needs Fabric API. Code is LGPL-3.0, its API is MIT, and assets are all-rights-reserved. The inspected commit is from 2026-09-11. [Server details](https://github.com/Dwinovo/minecraft-numen/blob/009d1291348b656e712f76ac59b4c1766534583e/docs/mcp-server.md) | Disabled by default; normally loopback port 8765 with a generated token and a 300-second call deadline. LAN exposure is optional and increases the boundary. |
| [langyo Minecraft Mod MCP](https://github.com/langyo/minecraft-mod-mcp/blob/98e76563c170ae0bbcb252a8564bc3d173b27a9d/README.md) | Best GUI match: screenshots, screen/widget inspection, clicks, typing, keys, scrolling and screen-method calls. It still has no repository fixture or scenario integration. | Yes | Yes | Yes | Yes | Exact artifacts for all four targets were attached to the inspected [development release](https://github.com/langyo/minecraft-mod-mcp/releases/tag/dev%2F98e76563c170ae0bbcb252a8564bc3d173b27a9d). A client mod exposes HTTP/SSE to a Node 20+ stdio bridge. Package licensing is MIT or Apache-2.0; the repository also offers CC0. The inspected commit is from 2026-09-08. | The inspected server [binds to `0.0.0.0`](https://github.com/langyo/minecraft-mod-mcp/blob/98e76563c170ae0bbcb252a8564bc3d173b27a9d/packages/common/src/main/java/xyz/langyo/minecraft/mcp/common/McpHttpServer.java#L73-L103), permits wildcard CORS, and its command handler has no authentication guard. The README's localhost examples therefore do not establish a loopback-only boundary. |
| [MCP Bridge / MCP Forge](https://www.curseforge.com/minecraft/mc-mods/mcp-bridge) | Admin/world commands and events. The publisher page does not establish owner-screen navigation, visible-state reading, screenshots, or GUI clicks. | No | No | No | No | The only listed file is Forge 1.21.1, while this matrix uses NeoForge at 1.21.1. It is a client/server mod using HTTP POST on port 8765. The page marks it all-rights-reserved and was last updated 2025-12-27; that date alone does not prove active maintenance. | The publisher documents a loopback endpoint and bearer authentication; no source was found to verify binding. Its default token is `changeme`; it must be replaced. Admin/world tools are broader than the smoke need. |
| [yuniko Mineflayer MCP](https://github.com/yuniko-software/minecraft-mcp-server/blob/240c8cec337ce152cc9e058ebdef511055808406/README.md) | A separate Mineflayer bot performs movement, inventory, crafting, combat and chat. It does not render or operate the prepared client's AE2 GUI. | Unverified | Unverified | Unverified | Unverified | Apache-2.0, Node 20.10+ and stdio. It needs a LAN or server connection and a separate bot identity. The source documents Minecraft 1.21.11, not any exact prepared combination; loader/modded handshakes are unqualified. The inspected commit is from 2026-04-04. | Adds a network player and server/LAN access. Its permissions and account lifecycle differ from the owner client under test. |
| [mcpfabric](https://github.com/Etoryx/mcpfabric/blob/1881470282f2c893a6aedc06390bff5984694e04/README.md) | Client control includes native screenshots and input; server mode offers admin control. The source does not establish AE2 widget semantics or repository fixtures. | No | No | No | No | MIT, Node 20+, Fabric Loader 0.19.3+ and Fabric API. It supports Fabric 1.21.1 through 1.21.11, 26.1–26.1.2 and 26.2, so none matches the exact loader/version pairs here. The inspected commit is from 2026-07-30; no GitHub release was present in the inspected metadata. | Loopback port 25599 and bearer authentication by default, with stdio or HTTP externally. It grants operator-level control and warns against changing those defaults. [Security policy](https://github.com/Etoryx/mcpfabric/blob/1881470282f2c893a6aedc06390bff5984694e04/SECURITY.md) |

The documented action coverage is narrower than a complete smoke run:

| Candidate | Launch/attachment and fixture loading | State, logs and results beyond the GUI actions above |
| --- | --- | --- |
| Numen | Connect to the installed mod's endpoint in a running game; no prepared-client launcher or marked-fixture loader established. | Companion/world and machine-content queries; action outcomes are not repository assertions or game-log classification. |
| langyo | The linked README documents a launcher CLI and attachment to the running mod; neither is qualified for the prepared CodexVM clients or marked fixtures. | Player/world/debug queries, MCP logs and SSE events, plus JSON command responses. MCP logs are not proof of complete game-log inspection. |
| MCP Forge | Connect to the running mod's endpoint; no client launcher or marked-fixture loader established. | Structured world/player tools and recent chat/events; no repository result or game-log contract established. |
| yuniko | Launches a separate bot and connects it to a server; no owner-client launch/attachment or marked-fixture loader established. | Bot game-state tools; no owner-client game-log or smoke-result contract established. |
| mcpfabric | Connects to a running client/server mod; no prepared-client launcher or marked-fixture loader established. | World/player/status queries, chat/events and command-output capture. Screenshots show visible state, but screen navigation and GUI-widget clicks are not established by its movement/interact tools. |

These are tool responses, not qualified structured smoke results. None of the
candidates covers client launch or attachment, marked fixture-world
copy/loading, repository scenario selection, semantic assertions, evidence
archiving, log classification, and exact-process cleanup as one qualified flow.
Only langyo and mcpfabric document real client screenshots and input. Numen,
MCP Forge and the Mineflayer server expose companion, world, admin or bot control;
those actions are not evidence that a player saw or used the expected AE2 screen.

## Recommendation

Do not add another Minecraft MCP to the normal smoke path. The current
[matrix runner](../../scripts/run-ui-smoke-matrix.ps1) already owns target and
scenario selection, build/stage/dispatch, result validation, archiving and
process lifecycle. The test driver owns fixture readiness, deterministic UI
actions, semantic assertions and screenshots. An agent-facing replacement would
duplicate that logic and make timing and failure diagnosis less deterministic.
Adopting langyo would also require fixing its network boundary and maintaining
another mod/Node bridge across four targets; mcpfabric adds loader ports before
qualification. Neither cost is justified by an observed coordination saving.

The repository also already has an authenticated, interactive-only MCP endpoint
for [1.20.1 through 1.21.1](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/InteractiveMcpServer.java)
and [26.1.2](../../versions/26.1.2-neoforge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/InteractiveMcpServer.java).
It exposes state, screen, UI snapshot, screenshot, logs and quit over streamable
HTTP. It uses a generated bearer token, loopback ephemeral port, request/response
limits, an allowlist and bounded scheduling. It deliberately does not launch a
client, load a world, click controls, or start scenarios. Normal deterministic
execution should keep that ownership split.

There is no measured runtime saving. The runner's two-second completion poll
bounds detection latency to roughly 0–2 seconds per dependency graph, excluding
I/O and scheduling; an MCP cannot speed Minecraft loading, rendering, crafting,
fixture reset or assertions. Existing scenarios already run without controller
decisions, so any saving would be limited to controller round trips around a run
and could be offset by MCP transport and agent decisions. A percentage claim is
not supported without a matched benchmark.

## Conditional proof of concept

No proof of concept is justified by this research alone. If future measurements
show repeated diagnostic controller delays, open a separate implementation issue
and first qualify the existing endpoint against one `1.20.1-forge` interactive
`craft-plan` leaf using a guest-local direct client. Keep the driver's original
results, logs, semantic assertions and screenshots, and verify wrong-token,
oversized-request, timeout and process-stop failures. Guest loopback is not host
loopback, so any tunnel would need a separately reviewed, narrow boundary.

Only if that measurement shows a useful gap should a host facade expose the
existing runner's plan, start, status and result operations with opaque run IDs.
It must not accept arbitrary shell commands or world paths, and it must not copy
scenario logic out of the driver. Runtime qualification across all four prepared
targets and any benchmark belong to that follow-up issue.
