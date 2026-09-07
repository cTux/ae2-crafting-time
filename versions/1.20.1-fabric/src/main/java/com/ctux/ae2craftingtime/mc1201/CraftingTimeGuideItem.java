package com.ctux.ae2craftingtime.mc1201;

import appeng.core.AppEng;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CraftingTimeGuideItem extends Item {
    public CraftingTimeGuideItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            AppEng.instance().openGuideAtPreviousPage(new ResourceLocation(Ae2CraftingTime.MOD_ID, "index.md"));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
