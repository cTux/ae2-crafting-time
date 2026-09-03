package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static final String IMPORT_EXPORT_ID = "ae2insertexportcard";
    static final String EXTENDED_AE_ID = "extendedae";
    static final String TARGET = "1.20.1-fabric";

    static AddonCpuFixture<?> baseFixture(String scenario) {
        return new FabricBaseFixture(!java.util.Set.of("appbot-cpu", "ae2things-cpu", "megacells-cpu").contains(scenario));
    }

    static net.minecraft.world.item.ItemStack processingPattern(appeng.api.stacks.GenericStack input,
            appeng.api.stacks.GenericStack output) {
        return appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                new appeng.api.stacks.GenericStack[] {input}, new appeng.api.stacks.GenericStack[] {output});
    }
    static boolean isModLoaded(String id) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        throw new IllegalArgumentException("WCWT is unavailable on Fabric 1.20.1");
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
