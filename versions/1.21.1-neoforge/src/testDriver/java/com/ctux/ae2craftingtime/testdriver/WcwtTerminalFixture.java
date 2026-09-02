package com.ctux.ae2craftingtime.testdriver;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.api.config.Actionable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class WcwtTerminalFixture extends WirelessTerminalFixture {
    @Override
    String modId() {
        return "wcwt";
    }

    @Override
    String itemId() {
        return "wcwt:wireless_comprehensive_work_terminal";
    }

    @Override
    String screenClass() {
        return "com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen";
    }

    @Override
    String screenshotPrefix() {
        return "ae2wcwt";
    }

    @Override
    ItemStack terminalStack() {
        var terminal = (WirelessTerminalItem) BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId())).orElseThrow();
        var stack = new ItemStack(terminal);
        terminal.injectAEPower(stack, terminal.getAEMaxPower(stack), Actionable.MODULATE);
        return stack;
    }
}
