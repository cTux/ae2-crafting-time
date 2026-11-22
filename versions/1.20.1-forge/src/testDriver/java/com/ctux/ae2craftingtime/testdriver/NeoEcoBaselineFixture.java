package com.ctux.ae2craftingtime.testdriver;

import appeng.menu.me.crafting.CraftConfirmMenu;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

final class NeoEcoBaselineFixture extends NeoEcoStatusFixture {
    @Override
    protected void startCraft(ServerPlayer player, Placement placement, CraftConfirmMenu menu) {
        startBaseline(player);
    }

    @Override
    boolean verifyAfterSample(Minecraft client, Consumer<String> capture) {
        return verifyBaseline(client, capture);
    }
}
