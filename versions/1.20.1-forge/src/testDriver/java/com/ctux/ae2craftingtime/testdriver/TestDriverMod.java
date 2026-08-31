package com.ctux.ae2craftingtime.testdriver;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(TestDriverMod.MOD_ID)
public final class TestDriverMod {
    public static final String MOD_ID = "ae2craftingtime_test_driver";

    public TestDriverMod() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TestDriverMod::startClient);
    }

    private static void startClient() {
        var options = DriverOptions.load();
        if (options == null) {
            return;
        }
        try {
            var version = ModList.get().getModContainerById(MOD_ID).orElseThrow().getModInfo().getVersion();
            var driverFile = "ae2-crafting-time-" + version + "-forge-1.20.1-test-driver.jar";
            var runtime = new TestDriverRuntime(options, driverFile);
            MinecraftForge.EVENT_BUS.register(runtime);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    runtime.close();
                } catch (Exception ignored) {
                }
            }, "ae2ct-test-driver-shutdown"));
        } catch (Exception error) {
            throw new IllegalStateException("Cannot start AE2 Crafting Time test driver", error);
        }
    }
}
