package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftConfirmMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

abstract class AddonCpuFixture<P> {
    private static final Map<String, String> FIXTURES = Map.ofEntries(
            Map.entry("advancedae-cpu", "com.ctux.ae2craftingtime.testdriver.AdvancedAeFixture"),
            Map.entry("extendedae-cpu", "com.ctux.ae2craftingtime.testdriver.ExtendedAeFixture"),
            Map.entry("extendedae-plus-cpu", "com.ctux.ae2craftingtime.testdriver.ExtendedAePlusFixture"),
            Map.entry("bmaddon-cpu", "com.ctux.ae2craftingtime.testdriver.BmAddonFixture"),
            Map.entry("crazyae2addons-cpu", "com.ctux.ae2craftingtime.testdriver.NativeCpuFixture"),
            Map.entry("appbot-cpu", "com.ctux.ae2craftingtime.testdriver.AppliedBotanicsFixture"),
            Map.entry("appbot-fork-cpu", "com.ctux.ae2craftingtime.testdriver.AppliedBotanicsFixture"),
            Map.entry("advancedperipherals-cpu", "com.ctux.ae2craftingtime.testdriver.AdvancedPeripheralsFixture"),
            Map.entry("ae2things-cpu", "com.ctux.ae2craftingtime.testdriver.Ae2ThingsFixture"),
            Map.entry("expandedae-cpu", "com.ctux.ae2craftingtime.testdriver.ExpandedAeFixture"),
            Map.entry("megacells-cpu", "com.ctux.ae2craftingtime.testdriver.MegaCellsFixture"),
            Map.entry("neoeco-cpu", "com.ctux.ae2craftingtime.testdriver.NeoEcoFixture"),
            Map.entry("lightningtech-cpu", "com.ctux.ae2craftingtime.testdriver.LightningTechFixture"),
            Map.entry("omnicells-cpu", "com.ctux.ae2craftingtime.testdriver.OmniCellsFixture"),
            Map.entry("projectcell-cpu", "com.ctux.ae2craftingtime.testdriver.ProjectCellFixture"),
            Map.entry("appliede-cpu", "com.ctux.ae2craftingtime.testdriver.AppliedEFixture"),
            Map.entry("appflux-cpu", "com.ctux.ae2craftingtime.testdriver.AppliedFluxFixture"),
            Map.entry("appmek-cpu", "com.ctux.ae2craftingtime.testdriver.AppliedMekanisticsFixture"),
            Map.entry("modern-ae2-additions-cpu", "com.ctux.ae2craftingtime.testdriver.ModernAe2AdditionsFixture"),
            Map.entry("omnisequence-cpu", "com.ctux.ae2craftingtime.testdriver.OmniSequenceFixture"));

    private CompletableFuture<P> placementFuture;
    private CompletableFuture<Boolean> setupFuture;

    static boolean supports(String scenario) {
        return "craft-plan".equals(scenario) || NoSpaceScenario.SCENARIO.equals(scenario) || NoProviderScenario.SCENARIO.equals(scenario) || CraftingTreeScenario.SCENARIO.equals(scenario) || RequesterFixture.SCENARIO.equals(scenario)
                || Ae2NetworkAnalyserFixture.SCENARIO.equals(scenario)
                || WirelessTerminalFixture.supports(scenario)
                || FIXTURES.containsKey(scenario);
    }

    static AddonCpuFixture<?> create(String scenario) {
        if ("craft-plan".equals(scenario) || NoSpaceScenario.SCENARIO.equals(scenario) || NoProviderScenario.SCENARIO.equals(scenario) || CraftingTreeScenario.SCENARIO.equals(scenario) || RequesterFixture.SCENARIO.equals(scenario)
                || Ae2NetworkAnalyserFixture.SCENARIO.equals(scenario)
                || WirelessTerminalFixture.supports(scenario)) {
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

    final void startCraft(ServerPlayer player, CraftConfirmMenu menu) {
        startCraft(player, placementFuture.join(), menu);
    }

    protected void startCraft(ServerPlayer player, P placement, CraftConfirmMenu menu) {
        menu.startJob();
    }

    protected abstract P place(ServerPlayer player, FixtureMarker marker);

    protected abstract boolean finish(ServerPlayer player, P placement);

    protected abstract ICraftingCPU cpu(ServerPlayer player, P placement, IGrid grid);
}
