package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

abstract class AddonCpuFixture<P> {
    private static final Map<String, String> FIXTURES = Map.of(
            "advancedae-cpu", "com.ctux.ae2craftingtime.testdriver.AdvancedAeFixture",
            "extendedae-cpu", "com.ctux.ae2craftingtime.testdriver.ExtendedAeFixture",
            "extendedae-plus-cpu", "com.ctux.ae2craftingtime.testdriver.ExtendedAePlusFixture",
            "bmaddon-cpu", "com.ctux.ae2craftingtime.testdriver.BmAddonFixture",
            "crazyae2addons-cpu", "com.ctux.ae2craftingtime.testdriver.CrazyAe2AddonsFixture",
            "megacells-cpu", "com.ctux.ae2craftingtime.testdriver.MegaCellsFixture",
            "neoeco-cpu", "com.ctux.ae2craftingtime.testdriver.NeoEcoFixture",
            "omnisequence-cpu", "com.ctux.ae2craftingtime.testdriver.OmniSequenceFixture");

    private CompletableFuture<P> placementFuture;
    private CompletableFuture<Boolean> setupFuture;

    static boolean supports(String scenario) {
        return "craft-plan".equals(scenario) || WirelessTerminalFixture.supports(scenario)
                || FIXTURES.containsKey(scenario);
    }

    static AddonCpuFixture<?> create(String scenario) {
        if ("craft-plan".equals(scenario) || WirelessTerminalFixture.supports(scenario)) {
            return null;
        }
        var fixtureClass = FIXTURES.get(scenario);
        if (fixtureClass == null) {
            throw new IllegalArgumentException("unsupported test-driver scenario: " + scenario);
        }
        try {
            return (AddonCpuFixture<?>) Class.forName(fixtureClass).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError error) {
            throw new IllegalStateException("cannot load test-driver fixture for " + scenario, error);
        }
    }

    final boolean setup(Minecraft minecraft, FixtureMarker marker) {
        var server = minecraft.getSingleplayerServer();
        var playerId = minecraft.player.getUUID();
        if (placementFuture == null) {
            placementFuture = server.submit(() -> place(server.getPlayerList().getPlayer(playerId), marker));
        }
        if (!placementFuture.isDone()) {
            return false;
        }
        if (setupFuture == null) {
            setupFuture = server.submit(() -> finish(
                    server.getPlayerList().getPlayer(playerId), placementFuture.join()));
        }
        if (!setupFuture.isDone()) {
            return false;
        }
        if (!setupFuture.join()) {
            setupFuture = null;
            return false;
        }
        return true;
    }

    final ICraftingCPU cpu(ServerPlayer player, IGrid grid) {
        return cpu(player, placementFuture.join(), grid);
    }

    final String outputId(FixtureMarker marker) {
        return outputId(placementFuture.join(), marker);
    }

    protected String outputId(P placement, FixtureMarker marker) {
        return marker.outputId();
    }

    protected abstract P place(ServerPlayer player, FixtureMarker marker);

    protected abstract boolean finish(ServerPlayer player, P placement);

    protected abstract ICraftingCPU cpu(ServerPlayer player, P placement, IGrid grid);
}
