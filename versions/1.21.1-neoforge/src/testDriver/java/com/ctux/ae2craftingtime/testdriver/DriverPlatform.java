package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static void captureSuite(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,
            net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos min, net.minecraft.core.Vec3i size) {
        template.fillFromWorld(level, min, size, false, net.minecraft.world.level.block.Blocks.AIR);
    }

    static com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeSavedData suiteSavedData(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeSavedData.FACTORY, com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeSavedData.FILE_ID);
    }

    static void openChat(net.minecraft.client.Minecraft minecraft) {
        minecraft.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
    }

    static boolean modifiers(net.minecraft.client.Minecraft minecraft, boolean reset) {
        return net.minecraft.client.gui.screens.Screen.hasControlDown()
                && net.minecraft.client.gui.screens.Screen.hasAltDown() == reset;
    }

    static boolean focus(net.minecraft.client.Minecraft minecraft) {
        return StandardAe2Scenario.focus(minecraft.getWindow().getWindow());
    }

    static void cloneEntry(appeng.client.gui.me.common.MEStorageScreen<?> screen,
            appeng.menu.me.common.GridInventoryEntry entry) {
        ((com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor) screen)
                .ae2craftingtime_test_driver$click(entry, 2, net.minecraft.world.inventory.ClickType.CLONE);
    }

    static void click(net.minecraft.client.Minecraft minecraft, double x, double y) {
        minecraft.screen.mouseClicked(x, y, 0);
    }
    static void clickAndRelease(net.minecraft.client.Minecraft minecraft, double x, double y) {
        click(minecraft, x, y);
        minecraft.screen.mouseReleased(x, y, 0);
    }

    static final String IMPORT_EXPORT_ID = "ae2importexportcard";
    static final String EXTENDED_AE_ID = "extendedae";
    static final String TARGET = "1.21.1-neoforge";

    static AddonCpuFixture<?> baseFixture(String scenario) {
        return new NeoForgeBaseFixture();
    }

    static net.minecraft.world.item.ItemStack processingPattern(appeng.api.stacks.GenericStack input,
            appeng.api.stacks.GenericStack output) {
        return ServerDriverPlatform.processingPattern(input, output);
    }
    static boolean isModLoaded(String id) {
        return net.neoforged.fml.ModList.get().isLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        return new WcwtTerminalFixture();
    }

    static void clearLevel(net.minecraft.client.Minecraft minecraft) {
        minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
    }

    static void openWorld(net.minecraft.client.Minecraft minecraft, String world) {
        minecraft.createWorldOpenFlows().openWorld(world, () -> minecraft.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
    }

    static void reconnect(net.minecraft.client.Minecraft minecraft) {
        var server = minecraft.getCurrentServer();
        clearLevel(minecraft);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(),
                minecraft, net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(server.ip), server, false, null);
    }

    static void configureRequester(com.almostreliable.merequester.requester.RequesterBlockEntity requester,
            appeng.api.stacks.GenericStack stack) {
        requester.getRequestManager().setStack(0, stack);
        requester.getRequestManager().get(0).updateState(false);
    }
}
