package com.ctux.ae2craftingtime.testdriver;

import appeng.api.features.GridLinkables;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Arrays;
import java.util.Objects;

abstract class WirelessTerminalFixture {
    private BlockPos accessPointPos;

    static boolean supports(String scenario) {
        return "ae2wcwt-terminal".equals(scenario) || "ae2wtlib-terminal".equals(scenario)
                || "ae2importexportcard-terminal".equals(scenario)
                || "aeinfinitybooster-terminal".equals(scenario);
    }

    static WirelessTerminalFixture create(String scenario) {
        return switch (scenario) {
            case "ae2wcwt-terminal" -> DriverPlatform.wcwtTerminal();
            case "ae2wtlib-terminal" -> new Ae2wtlibTerminalFixture();
            case "ae2importexportcard-terminal" -> new Ae2ImportExportCardFixture();
            case "aeinfinitybooster-terminal" -> new AeInfinityBoosterFixture();
            default -> null;
        };
    }

    final ItemStack setup(ServerPlayer player, FixtureMarker marker) {
        if (!DriverPlatform.isModLoaded(modId())) {
            throw new IllegalStateException(modId() + " is unavailable");
        }
        var level = player.level();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture terminal node is unavailable"));
        var accessPointBlock = BuiltInRegistries.BLOCK.getOptional(
                Objects.requireNonNull(Identifier.tryBuild("ae2", "wireless_access_point"))).orElse(null);
        if (accessPointBlock == null) {
            throw new IllegalStateException("AE2 wireless access point is unavailable");
        }
        if (accessPointPos == null) {
            accessPointPos = BlockPos.betweenClosedStream(terminal.offset(-6, -2, -6), terminal.offset(6, 2, 6))
                    .filter(pos -> level.getBlockState(pos).isAir()).findFirst()
                    .map(BlockPos::immutable)
                    .orElseThrow(() -> new IllegalStateException("no space for the wireless access point"));
            level.setBlockAndUpdate(accessPointPos, accessPointBlock.defaultBlockState());
            return null;
        }
        if (!(level.getBlockEntity(accessPointPos) instanceof WirelessAccessPointBlockEntity accessPoint)) {
            throw new IllegalStateException("AE2 wireless access point was not placed");
        }
        var accessPointNode = accessPoint.getMainNode().getNode();
        if (accessPointNode == null) {
            return null;
        }
        if (terminalNode.getGrid() != accessPointNode.getGrid()) {
            GridHelper.createConnection(terminalNode, accessPointNode);
        }

        var stack = terminalStack();
        var linkable = GridLinkables.get(stack.getItem());
        if (linkable == null || !linkable.canLink(stack)) {
            throw new IllegalStateException(modId() + " terminal cannot be linked");
        }
        linkable.link(stack, GlobalPos.of(level.dimension(), accessPointPos));
        stack = finishSetup(player, marker, terminalNode.getGrid(), accessPoint, stack);
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    abstract String modId();
    abstract String itemId();
    abstract String screenClass();
    abstract String screenshotPrefix();
    abstract ItemStack terminalStack();

    ItemStack finishSetup(ServerPlayer player, FixtureMarker marker, IGrid grid,
            WirelessAccessPointBlockEntity accessPoint, ItemStack stack) {
        return stack;
    }
}
