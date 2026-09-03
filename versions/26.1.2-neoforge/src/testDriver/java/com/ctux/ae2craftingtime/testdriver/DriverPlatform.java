package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static void click(net.minecraft.client.Minecraft minecraft, double x, double y) {
        minecraft.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
    }

    static final String IMPORT_EXPORT_ID = "ae2importexportcard";
    static final String EXTENDED_AE_ID = "extendedae";
    static final String TARGET = "26.1.2-neoforge";

    static AddonCpuFixture<?> baseFixture(String scenario) {
        return new NeoForgeBaseFixture();
    }

    static net.minecraft.world.item.ItemStack processingPattern(appeng.api.stacks.GenericStack input,
            appeng.api.stacks.GenericStack output) {
        return appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(java.util.List.of(input), java.util.List.of(output));
    }

    static boolean isModLoaded(String id) {
        return net.neoforged.fml.ModList.get().isLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        throw new IllegalStateException("WCWT is unavailable for 26.1.2");
    }

    static void clearLevel(net.minecraft.client.Minecraft minecraft) {
        minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
    }

    static void openWorld(net.minecraft.client.Minecraft minecraft, String world) {
        minecraft.createWorldOpenFlows().openWorld(world, () -> minecraft.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
    }

}
