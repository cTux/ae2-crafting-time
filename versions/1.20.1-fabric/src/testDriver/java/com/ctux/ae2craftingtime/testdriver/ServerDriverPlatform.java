package com.ctux.ae2craftingtime.testdriver;

final class ServerDriverPlatform {
    static net.minecraft.world.item.ItemStack processingPattern(appeng.api.stacks.GenericStack input,
            appeng.api.stacks.GenericStack output) {
        return appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                new appeng.api.stacks.GenericStack[] {input}, new appeng.api.stacks.GenericStack[] {output});
    }
    private ServerDriverPlatform() {}
}
