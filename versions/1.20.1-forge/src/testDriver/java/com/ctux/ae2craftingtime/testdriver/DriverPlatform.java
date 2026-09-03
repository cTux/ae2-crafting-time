package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static boolean modifiers(net.minecraft.client.Minecraft minecraft, boolean reset) {
        return net.minecraft.client.gui.screens.Screen.hasControlDown()
                && net.minecraft.client.gui.screens.Screen.hasAltDown() == reset;
    }

    static boolean focus(net.minecraft.client.Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        var nativeWindow = new com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Pointer.createConstant(
                org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(window)));
        if (nativeWindow.equals(com.sun.jna.platform.win32.User32.INSTANCE.GetForegroundWindow())) return true;
        org.lwjgl.glfw.GLFW.glfwFocusWindow(window);
        return false;
    }

    static void cloneEntry(appeng.client.gui.me.common.MEStorageScreen<?> screen,
            appeng.menu.me.common.GridInventoryEntry entry) {
        ((com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor) screen)
                .ae2craftingtime_test_driver$click(entry, 2, net.minecraft.world.inventory.ClickType.CLONE);
    }

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
