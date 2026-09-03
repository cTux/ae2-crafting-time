package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.world.item.ItemStack;

final class Ae2wtlibTerminalFixture extends WirelessTerminalFixture {
    @Override
    String modId() {
        return "ae2wtlib";
    }

    @Override
    String itemId() {
        return "ae2:wireless_crafting_terminal";
    }

    @Override
    String screenClass() {
        return "de.mari_023.ae2wtlib.wct.WCTScreen";
    }

    @Override
    String screenshotPrefix() {
        return "ae2wtlib";
    }

    @Override
    ItemStack terminalStack() {
        var terminal = (WirelessTerminalItem) AEItems.WIRELESS_CRAFTING_TERMINAL.asItem();
        var stack = new ItemStack(terminal);
        terminal.injectAEPower(stack, terminal.getAEMaxPower(stack), Actionable.MODULATE);
        return stack;
    }
}
