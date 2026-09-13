package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.integration.IntegrationMixinPlugin;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/** Opt-in dedicated-server proof using the same real grid, CPU fixtures and dispatch observer. */
public final class DedicatedCpuScenario {
    private final String scenario = System.getProperty("ae2ct.testDriver.serverScenario");
    private final String target = System.getProperty("ae2ct.testDriver.serverTarget");
    private final Path output = Path.of(System.getProperty("ae2ct.testDriver.serverResult"));
    private final StandardCraftFixture gridFixture = new StandardCraftFixture();
    private final FixtureMarker origin = new FixtureMarker(1, "craft-plan", "ae2-crafting-time",
            "dedicated-disposable", new FixtureMarker.Position(0, 80, 0, "NORTH"), "minecraft:smooth_stone");
    private ServerPlayer player;
    private AddonCpuFixture<Object> addon;
    private Object placement;
    private ICraftingCPU cpu;
    private IGrid grid;
    private Future<ICraftingPlan> calculation;
    private String network;
    private String outputId;
    private boolean submitted;
    private boolean ready;
    private boolean done;
    private long connectedAck;
    private String connectedAction = "";
    private StandardCraftFixture connectedSecond;
    private StandardCraftFixture connectedLifecycle;
    private StandardCraftFixture connectedLarge;
    private boolean connectedPrepared;
    private boolean connectedValidated;
    private final Map<String, StandardCraftFixture> recurrentFixtures = new java.util.HashMap<>();
    private final Map<String, Long> recurrentAcks = new java.util.HashMap<>();
    private boolean recurrentSwapped;
    private boolean recurrentReplanned;
    private boolean recurrentDisconnected;
    private boolean recurrentComplete;
    private final Set<String> recurrentCaptured = new java.util.HashSet<>();
    private final Map<String, String> recurrentActions = new java.util.HashMap<>();
    private int recurrentBetaMenu;
    private long recurrentBetaRevision;
    private final long started = System.nanoTime();

    public void tick(MinecraftServer server) {
        if (done) return;
        try {
            if (!server.isDedicatedServer()) throw new IllegalStateException("Dedicated test requires a dedicated server");
            if (System.nanoTime() - started > java.util.concurrent.TimeUnit.MINUTES.toNanos(timeoutMinutes(scenario))) {
                throw new IllegalStateException("Dedicated CPU timeout: " + scenario + " " + DispatchObservation.snapshot());
            }
            step(server);
        } catch (Exception | LinkageError failure) {
            finish(server, "FAIL", failure.toString());
            failure.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void step(MinecraftServer server) throws Exception {
        var level = server.overworld();
        if (scenario.equals("startup-only")) {
            finish(server, "PASS", "");
            return;
        }
        if (scenario.equals("cpu-list-total-ttc-connected")) {
            stepConnected(server, level);
            return;
        }
        if (scenario.equals("recurrent-plan-connected")) {
            stepRecurrentConnected(server, level);
            return;
        }
        if (player == null) {
            if (!Set.of("advancedae-cpu", "advancedae-read-recovery", "lightningtech-cpu", "neoeco-cpu", "neoeco-fastpath-cpu").contains(scenario)) {
                throw new IllegalArgumentException("Unsupported dedicated scenario: " + scenario);
            }
            AdapterSmokePolicy.verify(target, scenario.equals("advancedae-read-recovery") ? "advancedae-cpu" : scenario,
                    "en_us", IntegrationMixinPlugin.snapshot());
            String loader = target.endsWith("-forge") ? "minecraftforge" : "neoforged.neoforge";
            player = (ServerPlayer) Class.forName("net." + loader + ".common.util.FakePlayerFactory")
                    .getMethod("get", ServerLevel.class, GameProfile.class).invoke(null, level,
                            new GameProfile(UUID.fromString("a27ed489-e2c1-4fa8-a110-ab742882a210"), "AdapterSmoke"));
            // Keep this disposable fixture ticking without a connected graphical client.
            for (int x = 2; x <= 6; x++) for (int z = -2; z <= 2; z++) level.setChunkForced(x, z, true);
            addon = (AddonCpuFixture<Object>) (scenario.equals("advancedae-read-recovery")
                    ? new NativeCpuFixture() : AddonCpuFixture.create(scenario));
        }
        if (!gridFixture.prepare(player, origin)) return;
        var terminal = gridFixture.terminal;
        var marker = new FixtureMarker(1, "craft-plan", "ae2-crafting-time", "dedicated-disposable",
                new FixtureMarker.Position(terminal.getX(), terminal.getY(), terminal.getZ(), "NORTH"), origin.outputId());
        if (placement == null) { placement = addon.place(player, marker); return; }
        if (!ready) { ready = addon.finish(player, placement); if (!ready) return; }
        if (grid == null) {
            grid = ((IInWorldGridNodeHost) level.getBlockEntity(terminal)).getGridNode(Direction.NORTH).getGrid();
            network = ProfilerBridge.networkId(grid);
            outputId = addon.outputId(placement, marker);
            if (scenario.equals("advancedae-read-recovery")) {
                var host = (appeng.api.storage.ITerminalHost) ((appeng.api.parts.IPartHost)
                        level.getBlockEntity(terminal)).getPart(Direction.NORTH);
                var previousMenu = player.containerMenu;
                player.containerMenu = new appeng.menu.me.crafting.CraftingStatusMenu(1, player.getInventory(), host);
                try {
                    for (int attempt = 0; attempt < 2; attempt++) {
                        var context = com.ctux.ae2craftingtime.mc1201.StatsRequestContext.current(player);
                        if (context.grid() != grid || context.craftingCpu() != null
                                || !com.ctux.ae2craftingtime.mc1201.IntegrationLog.disabled("advanced_ae", "selected-cpu")) {
                            throw new IllegalStateException("Selected-CPU recovery did not retain the real grid");
                        }
                    }
                } finally { player.containerMenu = previousMenu; }
            }
        }
        if (cpu == null) { cpu = addon.cpu(player, placement, grid); if (cpu == null) return; }
        if (calculation == null) {
            var key = AEItemKey.of(scenario.equals("neoeco-fastpath-cpu") ? Items.BIRCH_PLANKS : Items.SMOOTH_STONE);
            grid.getStorageService().getInventory().extract(key, Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty());
            ProfilerBridge.clearStats(new ProfileKey(network, outputId));
            // AE2 discovers recipes through the requester's grid node; a player-only source has none.
            var source = IActionSource.ofMachine(gridFixture.cpu(player));
            calculation = grid.getCraftingService().beginCraftingCalculation(level,
                    () -> source,
                    key, scenario.equals("neoeco-fastpath-cpu") ? 64 : 1, CalculationStrategy.REPORT_MISSING_ITEMS);
            return;
        }
        if (!calculation.isDone()) return;
        if (!submitted) {
            var plan = calculation.get();
            if (plan.simulation()) {
                var missing = new java.util.ArrayList<String>();
                for (var item : plan.missingItems()) missing.add(item.getKey().getId() + "=" + item.getLongValue());
                throw new IllegalStateException("Dedicated plan is missing inputs: " + missing);
            }
            DispatchObservation.watch(network, outputId);
            var submission = grid.getCraftingService().submitJob(plan, null, cpu, false, IActionSource.ofPlayer(player));
            if (!submission.successful()) throw new IllegalStateException("Dedicated job rejected: " + submission);
            submitted = true;
        }
        gridFixture.pump(player, true);
        var observed = DispatchObservation.snapshot();
        if (observed.finishes() == 0) return;
        addon.verifyDispatch(observed);
        AdapterSmokePolicy.verifyCpu(target, scenario, observed);
        if (ProfilerBridge.stats(new ProfileKey(network, outputId)).map(stats -> stats.sampleCount()).orElse(0) == 0) {
            throw new IllegalStateException("Dedicated job did not record a fresh sample");
        }
        finish(server, "PASS", "");
    }

    static int timeoutMinutes(String value) {
        return value.endsWith("-connected") ? 40 : 5;
    }

    private void stepRecurrentConnected(MinecraftServer server, ServerLevel level) {
        if (!connectedValidated) { CpuListTtcControl.validateDisposableServer(Path.of(""), target); connectedValidated = true; }
        var roles = target.equals("1.21.1-neoforge") ? java.util.List.of("alpha", "beta") : java.util.List.of("alpha");
        if (recurrentComplete && recurrentCaptured.containsAll(roles)
                && server.getPlayerList().getPlayers().isEmpty()) {
            finish(server, "PASS", "");
            return;
        }
        for (var role : roles) {
            var expectedName = role.equals("alpha") ? "Ae2ctAlpha" : "Ae2ctBeta";
            var rolePlayer = server.getPlayerList().getPlayers().stream().filter(value -> value.getName().getString().equals(expectedName)).findFirst().orElse(null);
            if (rolePlayer == null) {
                if (recurrentComplete && recurrentCaptured.contains(role)) continue;
                if (role.equals("alpha") && recurrentReplanned) {
                    recurrentDisconnected = true;
                    if (roles.contains("beta")) {
                        var beta = playerFor(server, "beta");
                        if (!(beta.containerMenu instanceof appeng.menu.me.crafting.CraftConfirmMenu menu)
                                || menu.containerId != recurrentBetaMenu
                                || ((com.ctux.ae2craftingtime.mc1201.RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision() != recurrentBetaRevision)
                            throw new IllegalStateException("Beta did not retain its open plan during Alpha reconnect");
                    }
                }
                return;
            }
            var fixture = recurrentFixtures.computeIfAbsent(role, ignored -> role.equals("alpha") ? gridFixture : gridFixture.secondGrid());
            var expectedUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + expectedName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (!rolePlayer.getUUID().equals(expectedUuid)) throw new IllegalStateException("Unexpected offline role identity");
            if (fixture.terminal == null) { fixture.cpuListScenario = false; fixture.recurrentPlan = role.equals("alpha"); fixture.missingPlanInput = true; fixture.unprofiledPlan = true; }
            if (!fixture.prepare(rolePlayer, origin)) return;
            var command = RecurrentPlanControl.command(role);
            var ack = recurrentAcks.getOrDefault(role, 0L);
            if (command.epoch().equals(CpuListTtcControl.epoch()) && command.role().equals(role)
                    && command.player().equals(rolePlayer.getUUID().toString()) && command.sequence() > ack) {
                var commandPhase = RecurrentCampaign.phase(recurrentSwapped, recurrentReplanned, recurrentComplete);
                var allowed = RecurrentCampaign.allows(role, command.action(), commandPhase,
                        RecurrentCampaign.turn(roles, recurrentActions, commandPhase), recurrentDisconnected);
                if (allowed && rolePlayer.containerMenu instanceof appeng.menu.me.crafting.CraftConfirmMenu menu
                        && menu.getPlan() != null) {
                    boolean flagged = menu.getPlan().getEntries().stream().anyMatch(entry ->
                            ((com.ctux.ae2craftingtime.mc1201.RecurrentPlanEntry) entry).ae2craftingtime$recurrent());
                    if (flagged != fixture.recurrentPlan) throw new IllegalStateException("Server recurrence outcome differs for " + role);
                    recurrentAcks.put(role, command.sequence());
                    recurrentActions.put(role, command.action());
                    if (role.equals("beta") && command.action().equals("swapped")) {
                        recurrentBetaMenu = menu.containerId;
                        recurrentBetaRevision = ((com.ctux.ae2craftingtime.mc1201.RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision();
                    }
                    if (command.action().equals("captured")) recurrentCaptured.add(role);
                    System.out.println("AE2CT recurrence recipient=" + rolePlayer.getUUID() + " menu=" + menu.containerId
                            + " revision=" + ((com.ctux.ae2craftingtime.mc1201.RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision()
                            + " role=" + role + " action=" + command.action() + " recurrent=" + flagged);
                }
            }
        }
        var initial = roles.stream().allMatch(role -> "initial".equals(RecurrentPlanControl.command(role).action())
                && recurrentAcks.getOrDefault(role, 0L) == RecurrentPlanControl.command(role).sequence());
        if (initial && !recurrentSwapped) {
            for (var role : roles) recurrentFixtures.get(role).setRecurrent(playerFor(server, role), !role.equals("alpha"));
            recurrentSwapped = true;
        }
        var swapped = recurrentSwapped && roles.stream().allMatch(role -> "swapped".equals(RecurrentPlanControl.command(role).action())
                && recurrentAcks.getOrDefault(role, 0L) == RecurrentPlanControl.command(role).sequence());
        recurrentReplanned |= swapped;
        var alphaRejoin = RecurrentPlanControl.command("alpha");
        var rejoined = "rejoined".equals(alphaRejoin.action())
                && recurrentAcks.getOrDefault("alpha", 0L) == alphaRejoin.sequence();
        recurrentComplete |= recurrentReplanned && recurrentDisconnected && rejoined;
        var phase = RecurrentCampaign.phase(recurrentSwapped, recurrentReplanned, recurrentComplete);
        var turn = RecurrentCampaign.turn(roles, recurrentActions, phase);
        for (var role : roles) {
            var name = role.equals("alpha") ? "Ae2ctAlpha" : "Ae2ctBeta";
            boolean online = server.getPlayerList().getPlayers().stream().anyMatch(value -> value.getName().getString().equals(name));
            if (!RecurrentCampaign.publish(online, recurrentComplete, recurrentCaptured.contains(role))) continue;
            var command = RecurrentPlanControl.command(role); var fixture = recurrentFixtures.get(role); var rolePlayer = playerFor(server, role);
            RecurrentPlanControl.publish(role, recurrentAcks.getOrDefault(role, 0L), command.action(), phase,
                    fixture.terminal, rolePlayer.getUUID(), fixture.recurrentPlan, turn);
        }
    }

    private static ServerPlayer playerFor(MinecraftServer server, String role) {
        var name = role.equals("alpha") ? "Ae2ctAlpha" : "Ae2ctBeta";
        return server.getPlayerList().getPlayers().stream().filter(value -> value.getName().getString().equals(name)).findFirst().orElseThrow();
    }

    private void stepConnected(MinecraftServer server, ServerLevel level) {
        if (!connectedValidated) {
            CpuListTtcControl.validateDisposableServer(Path.of(""), target);
            connectedValidated = true;
        }
        player = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (player == null) return;
        if (!connectedPrepared) {
            gridFixture.cpuListScenario = true;
            if (!gridFixture.prepare(player, origin) || !gridFixture.prepareCpuListJobs(player)) return;
            connectedPrepared = true;
        }
        var command = CpuListTtcControl.command();
        var active = connectedSecond == null ? gridFixture : connectedSecond;
        if (command.epoch().equals(CpuListTtcControl.epoch()) && command.sequence() > connectedAck) {
            boolean complete = switch (command.action()) {
                case "partial" -> { gridFixture.makeCpuListPartial(player); yield true; }
                case "restore" -> { gridFixture.restoreCpuListSamples(player); yield true; }
                case "rename" -> { gridFixture.renameCpuList(player); yield true; }
                case "finish" -> gridFixture.finishFirstCpu(player);
                case "cancel" -> { gridFixture.cancelSecondCpu(player); yield true; }
                case "replace" -> gridFixture.replaceSecondCpu(player);
                case "restart" -> gridFixture.restartSecondCpu(player);
                case "remove" -> { gridFixture.removeThirdCpu(player); yield true; }
                case "second-grid" -> {
                    if (connectedSecond == null) connectedSecond = gridFixture.secondGrid();
                    if (!connectedSecond.prepare(player, origin) || !connectedSecond.prepareCpuListJobs(player)) yield false;
                    connectedSecond.renameCpuList(player);
                    connectedLifecycle = connectedSecond;
                    yield true;
                }
                case "large-grid" -> {
                    if (connectedLarge == null) connectedLarge = gridFixture.largeCpuGrid();
                    if (!connectedLarge.prepare(player, origin) || !connectedLarge.prepareCpuListJobs(player)) yield false;
                    connectedSecond = connectedLarge;
                    yield true;
                }
                case "return-second" -> {
                    connectedSecond = connectedLifecycle;
                    if (connectedSecond == null) yield false;
                    player.teleportTo(connectedSecond.terminal.getX() + 0.5, connectedSecond.terminal.getY() - 1,
                            connectedSecond.terminal.getZ() - 2.5);
                    yield true;
                }
                case "rejoin-prepare", "relaunch-prepare" -> {
                    active.refreshCpuIdentities(player);
                    yield active.prepare(player, origin);
                }
                case "reconnect" -> true;
                case "complete" -> {
                    connectedAck = command.sequence();
                    connectedAction = command.action();
                    CpuListTtcControl.publish(connectedAck, connectedAction, "complete", active.terminal,
                            active.cpuListServerEstimates(player), active.cpuListServerState(player));
                    finish(server, "PASS", "");
                    yield false;
                }
                default -> command.action().isEmpty();
            };
            if (!complete) return;
            connectedAck = command.sequence();
            connectedAction = command.action();
            active = connectedSecond == null ? gridFixture : connectedSecond;
        }
        CpuListTtcControl.publish(connectedAck, connectedAction,
                connectedSecond == null ? "first-grid" : "second-grid",
                active.terminal, active.cpuListServerEstimates(player), active.cpuListServerState(player));
    }

    static void refreshCpuIdentitiesForReconnect(StandardCraftFixture fixture) {
        fixture.refreshCpuIdentities();
    }

    private void finish(MinecraftServer server, String result, String error) {
        done = true;
        try {
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                    "target", target, "scenario", scenario, "result", result, "error", error,
                    "adapters", IntegrationMixinPlugin.snapshot(), "dispatch", DispatchObservation.snapshot(),
                    "finishedAt", java.time.Instant.now().toString())));
        } catch (Exception failure) { throw new IllegalStateException("Cannot save dedicated test evidence", failure); }
        finally { server.halt(false); }
    }
}
