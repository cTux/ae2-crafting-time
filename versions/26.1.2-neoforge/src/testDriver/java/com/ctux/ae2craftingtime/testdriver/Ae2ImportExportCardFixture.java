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

final class Ae2ImportExportCardFixture extends WirelessTerminalFixture {
    @Override
    String modId() {
        return DriverPlatform.IMPORT_EXPORT_ID;
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
        return "ae2importexportcard";
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
        var terminal = (WirelessTerminalItem) stack.getItem();
        var exportCard = BuiltInRegistries.ITEM.getOptional(
                Objects.requireNonNull(Identifier.tryBuild(modId(), "export_card"))).orElse(null);
        if (exportCard == null || !terminal.getUpgrades(stack).addItems(new ItemStack(exportCard)).isEmpty()) {
            throw new IllegalStateException("AE2 Import Export Card could not be installed");
        }
        var output = BuiltInRegistries.ITEM.getOptional(
                Objects.requireNonNull(Identifier.tryParse(marker.outputId()))).orElse(null);
        if (output == null) {
            throw new IllegalStateException("fixture output is unavailable");
        }
        var key = AEItemKey.of(output);
        var tick = player.level().getGameTime();
        var networkId = ProfilerBridge.networkId(grid);
        ProfilerBridge.start(networkId, stack, key, 1, tick);
        ProfilerBridge.complete(networkId, stack, key, 1, tick + 40);
        return stack;
    }
}
