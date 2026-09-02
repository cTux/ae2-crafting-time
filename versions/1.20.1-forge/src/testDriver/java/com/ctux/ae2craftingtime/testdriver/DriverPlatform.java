package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static final String EXTENDED_AE_ID = "expatternprovider";
    static final String TARGET = "1.20.1-forge";

    static AddonCpuFixture<?> baseFixture() {
        return null;
    }

    static boolean isModLoaded(String id) {
        return net.minecraftforge.fml.ModList.get().isLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        return new WcwtTerminalFixture();
    }
}
