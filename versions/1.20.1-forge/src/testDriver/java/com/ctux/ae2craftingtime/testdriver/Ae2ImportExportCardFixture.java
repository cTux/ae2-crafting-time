package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

final class Ae2ImportExportCardFixture extends WirelessTerminalFixture {
    @Override
    String modId() {
        return "ae2insertexportcard";
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
    ItemStack finishSetup(ServerPlayer player, FixtureMarker marker, IGrid grid, ItemStack stack) {
        var terminal = (WirelessTerminalItem) stack.getItem();
        var exportCard = ForgeRegistries.ITEMS.getValue(
                Objects.requireNonNull(ResourceLocation.tryBuild(modId(), "export_card")));
        if (exportCard == null || !terminal.getUpgrades(stack).addItems(new ItemStack(exportCard)).isEmpty()) {
            throw new IllegalStateException("AE2 Import Export Card could not be installed");
        }
        return stack;
    }
}
