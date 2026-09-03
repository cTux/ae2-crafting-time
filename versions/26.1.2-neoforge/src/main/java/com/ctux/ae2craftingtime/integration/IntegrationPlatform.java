package com.ctux.ae2craftingtime.integration;

import net.neoforged.fml.loading.FMLLoader;

/** Early loader metadata only; never accesses the late runtime ModList. */
final class IntegrationPlatform {
    private IntegrationPlatform() {}
    static final String TARGET = "26.1.2-neoforge";

    static boolean isClient() {
        return FMLLoader.getCurrent().getDist().isClient();
    }

    static String version(String modId) {
        return FMLLoader.getCurrent().getLoadingModList().getMods().stream()
                .filter(mod -> mod.getModId().equals(modId))
                .map(mod -> mod.getVersion().toString()).findFirst().orElse(null);
    }
}
