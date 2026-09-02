package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static final String EXTENDED_AE_ID = "extendedae";
    static final String TARGET = "1.20.1-fabric";

    static AddonCpuFixture<?> baseFixture() {
        return new FabricBaseFixture();
    }

    static boolean isModLoaded(String id) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        throw new IllegalArgumentException("WCWT is unavailable on Fabric 1.20.1");
    }
}
