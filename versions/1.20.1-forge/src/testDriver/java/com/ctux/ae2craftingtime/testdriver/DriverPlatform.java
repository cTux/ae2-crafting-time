package com.ctux.ae2craftingtime.testdriver;

final class DriverPlatform {
    static final String TARGET = "1.20.1-forge";

    static boolean isModLoaded(String id) {
        return net.minecraftforge.fml.ModList.get().isLoaded(id);
    }

    static WirelessTerminalFixture wcwtTerminal() {
        return new WcwtTerminalFixture();
    }
}
