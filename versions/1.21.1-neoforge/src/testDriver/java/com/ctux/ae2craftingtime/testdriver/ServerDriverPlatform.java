package com.ctux.ae2craftingtime.testdriver;

final class ServerDriverPlatform {
    static net.minecraft.world.item.ItemStack processingPattern(appeng.api.stacks.GenericStack input,
            appeng.api.stacks.GenericStack output) {
        return appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                java.util.List.of(input), java.util.List.of(output));
    }
    private ServerDriverPlatform() {}
}
