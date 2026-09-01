package com.ctux.ae2craftingtime.testdriver;

import com.lhy.wcwt.WcwtMod;
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
        return WcwtMod.chargedTerminalStack();
    }
}
