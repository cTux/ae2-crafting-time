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
    private final long started = System.nanoTime();

    public void tick(MinecraftServer server) {
        if (done) return;
        try {
            if (!server.isDedicatedServer()) throw new IllegalStateException("Dedicated test requires a dedicated server");
            if (System.nanoTime() - started > java.util.concurrent.TimeUnit.MINUTES.toNanos(5)) {
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
