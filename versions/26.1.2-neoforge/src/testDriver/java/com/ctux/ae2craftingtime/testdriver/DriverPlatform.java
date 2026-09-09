package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static void captureSuite(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,
            net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos min, net.minecraft.core.Vec3i size) {
        template.fillFromWorld(level, min, size, false, java.util.List.of(net.minecraft.world.level.block.Blocks.AIR));
    }

    static com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeSavedData suiteSavedData(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeSavedData.TYPE);
    }

    static void openChat(net.minecraft.client.Minecraft minecraft) {
        minecraft.setScreen(new net.minecraft.client.gui.screens.ChatScreen("", false));
    }

    static boolean modifiers(net.minecraft.client.Minecraft minecraft, boolean reset) {
        long window = minecraft.getWindow().handle();
        boolean control = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) != 0;
        boolean alt = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) != 0;
        return control && alt == reset;
    }

    static boolean focus(net.minecraft.client.Minecraft minecraft) {
        return StandardAe2Scenario.focus(minecraft.getWindow().handle());
    }

    static void cloneEntry(appeng.client.gui.me.common.MEStorageScreen<?> screen,
            appeng.menu.me.common.GridInventoryEntry entry) {
        ((com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor) screen)
                .ae2craftingtime_test_driver$click(entry, 2, net.minecraft.world.inventory.ContainerInput.CLONE);
    }

    static void click(net.minecraft.client.Minecraft minecraft, double x, double y) {
        long window = minecraft.getWindow().handle();
        int modifiers = 0;
        if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS) modifiers |= org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
        if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) modifiers |= org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
        minecraft.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, modifiers)), false);
    }
    static void clickAndRelease(net.minecraft.client.Minecraft minecraft, double x, double y) {
        click(minecraft, x, y);
        minecraft.screen.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x, y,
                new net.minecraft.client.input.MouseButtonInfo(0, 0)));
    }

    static final String IMPORT_EXPORT_ID = "ae2importexportcard";
    static final String EXTENDED_AE_ID = "extendedae";
    static final String TARGET = "26.1.2-neoforge";

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
        throw new IllegalStateException("WCWT is unavailable for 26.1.2");
    }

    static void clearLevel(net.minecraft.client.Minecraft minecraft) {
        minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
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

}
