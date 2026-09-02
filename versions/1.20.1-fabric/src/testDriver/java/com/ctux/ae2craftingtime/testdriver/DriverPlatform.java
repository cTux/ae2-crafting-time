package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static final String EXTENDED_AE_ID = "extendedae";
    static final String TARGET = "1.20.1-fabric";

    static AddonCpuFixture<?> baseFixture(String scenario) {
        return new FabricBaseFixture(!java.util.Set.of("appbot-cpu", "ae2things-cpu", "megacells-cpu").contains(scenario));
    }

    static boolean isModLoaded(String id) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        throw new IllegalArgumentException("WCWT is unavailable on Fabric 1.20.1");
    }
}
