package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

final class Ae2NetworkAnalyserFixture {
    static final String SCENARIO = "ae2networkanalyser-screen";
    static final String SCREEN = "com.glodblock.github.ae2netanalyser.client.gui.GuiAnalyser";
    static final String MENU = "com.glodblock.github.ae2netanalyser.container.ContainerAnalyser";
    static final String ITEM = "ae2netanalyser:network_analyser";

    ItemStack setup(ServerPlayer player) {
        if (!ModList.get().isLoaded("ae2netanalyser")) {
            throw new IllegalStateException("AE2 Network Analyser is unavailable");
        }
        var item = ForgeRegistries.ITEMS.getValue(Objects.requireNonNull(ResourceLocation.tryParse(ITEM)));
        if (item == null) {
            throw new IllegalStateException("AE2 Network Analyser item is unavailable");
        }
        var stack = new ItemStack(item);
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }
}
