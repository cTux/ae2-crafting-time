package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static void click(net.minecraft.client.Minecraft minecraft, double x, double y) {
        minecraft.screen.mouseClicked(x, y, 0);
    }

    static final String IMPORT_EXPORT_ID = "ae2insertexportcard";
    static final String EXTENDED_AE_ID = "expatternprovider";
    static final String TARGET = "1.20.1-forge";

    static AddonCpuFixture<?> baseFixture(String scenario) {
        return null;
    }

    static net.minecraft.world.item.ItemStack processingPattern(appeng.api.stacks.GenericStack input,
            appeng.api.stacks.GenericStack output) {
        return appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                new appeng.api.stacks.GenericStack[] {input}, new appeng.api.stacks.GenericStack[] {output});
    }
    static boolean isModLoaded(String id) {
        return net.minecraftforge.fml.ModList.get().isLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        return new WcwtTerminalFixture();
    }

    static void clearLevel(net.minecraft.client.Minecraft minecraft) {
        minecraft.clearLevel(new net.minecraft.client.gui.screens.TitleScreen());
    }

    static void openWorld(net.minecraft.client.Minecraft minecraft, String world) {
        minecraft.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(), world);
    }

    static void configureRequester(com.almostreliable.merequester.requester.RequesterBlockEntity requester,
            appeng.api.stacks.GenericStack stack) {
        requester.getRequests().setStack(0, stack);
        requester.getRequests().get(0).updateState(false);
    }
}
