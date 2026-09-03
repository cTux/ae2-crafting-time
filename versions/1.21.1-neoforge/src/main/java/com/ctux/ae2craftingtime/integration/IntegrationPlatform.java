package com.ctux.ae2craftingtime.integration;

import net.neoforged.fml.loading.FMLLoader;

/** Early loader metadata only; never accesses the late runtime ModList. */
final class IntegrationPlatform {
    private IntegrationPlatform() {}
    static final String TARGET = "1.21.1-neoforge";

    static boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    static String version(String modId) {
        return FMLLoader.getLoadingModList().getMods().stream()
                .filter(mod -> mod.getModId().equals(modId))
                .map(mod -> mod.getVersion().toString()).findFirst().orElse(null);
    }
}
