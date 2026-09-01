package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.PlayerSource;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ultramega.ae2insertexportcard.item.UpgradeHost;
import com.ultramega.ae2insertexportcard.util.UpgradeType;
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
        if (!(terminal.getMenuHost(player, 0, stack, null) instanceof WirelessTerminalMenuHost host)) {
            throw new IllegalStateException("AE2 wireless terminal host is unavailable");
        }
        var card = new UpgradeHost(UpgradeType.EXPORT, 0, player.getInventory(), host);
        if (!card.getUpgrades().addItems(AEItems.CRAFTING_CARD.stack()).isEmpty()) {
            throw new IllegalStateException("AE2 crafting card could not be installed in the export card");
        }
        var output = ForgeRegistries.ITEMS.getValue(
                Objects.requireNonNull(ResourceLocation.tryParse(marker.outputId())));
        if (output == null) {
            throw new IllegalStateException("fixture output is unavailable");
        }
        card.filterConfig.setStack(0, new GenericStack(AEItemKey.of(output), 1));
        var selectedSlots = new int[36];
        selectedSlots[1] = 1;
        card.setSelectedInventorySlots(selectedSlots);
        player.getInventory().setItem(1, ItemStack.EMPTY);
        grid.getStorageService().getInventory().extract(AEItemKey.of(output), Long.MAX_VALUE,
                Actionable.MODULATE, new PlayerSource(player));
        ProfilerBridge.clearStats(new ProfileKey(ProfilerBridge.networkId(grid), marker.outputId()));
        return stack;
    }
}
