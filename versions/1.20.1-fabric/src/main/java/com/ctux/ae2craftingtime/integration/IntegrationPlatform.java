package com.ctux.ae2craftingtime.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;

/** Early loader metadata only; never accesses the late runtime ModList. */
final class IntegrationPlatform {
    private IntegrationPlatform() {}
    static final String TARGET = "1.20.1-fabric";

    static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
    }
}
