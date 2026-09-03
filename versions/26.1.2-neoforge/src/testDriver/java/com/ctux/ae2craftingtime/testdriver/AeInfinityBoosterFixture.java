package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Objects;

final class AeInfinityBoosterFixture extends WirelessTerminalFixture {
    @Override
    String modId() {
        return "aeinfinitybooster";
    }

    @Override
    String itemId() {
        return "ae2:wireless_terminal";
    }

    @Override
    String screenClass() {
        return "appeng.client.gui.me.common.MEStorageScreen";
    }

    @Override
    String screenshotPrefix() {
        return "aeinfinitybooster";
    }

    @Override
    ItemStack terminalStack() {
        var terminal = (WirelessTerminalItem) AEItems.WIRELESS_TERMINAL.asItem();
        var stack = new ItemStack(terminal);
        terminal.injectAEPower(stack, terminal.getAEMaxPower(stack), Actionable.MODULATE);
        return stack;
    }

    @Override
    ItemStack finishSetup(ServerPlayer player, FixtureMarker marker, IGrid grid,
            WirelessAccessPointBlockEntity accessPoint, ItemStack stack) {
        var card = BuiltInRegistries.ITEM.getOptional(
                Objects.requireNonNull(Identifier.tryBuild(modId(), "infinity_card"))).orElse(null);
        var normalRange = accessPoint.getRange();
        if (card == null || !accessPoint.getInternalInventory().addItems(new ItemStack(card)).isEmpty()) {
            throw new IllegalStateException("AEInfinityBooster Infinity Card could not be installed");
        }
        var position = accessPoint.getBlockPos();
        var level = player.level();
        level.setChunkForced(position.getX() >> 4, position.getZ() >> 4, true);
        player.teleportTo(position.getX() + normalRange + 64, position.getY() + 1, position.getZ());
        if (player.distanceToSqr(position.getX(), position.getY(), position.getZ()) <= normalRange * normalRange) {
            throw new IllegalStateException("wireless range fixture did not move beyond normal range");
        }
        var output = BuiltInRegistries.ITEM.getOptional(
                Objects.requireNonNull(Identifier.tryParse(marker.outputId()))).orElse(null);
        if (output == null) {
            throw new IllegalStateException("fixture output is unavailable");
        }
        var key = AEItemKey.of(output);
        var tick = level.getGameTime();
        var networkId = ProfilerBridge.networkId(grid);
        ProfilerBridge.start(networkId, stack, key, 1, tick);
        ProfilerBridge.complete(networkId, stack, key, 1, tick + 40);
        return stack;
    }
}
