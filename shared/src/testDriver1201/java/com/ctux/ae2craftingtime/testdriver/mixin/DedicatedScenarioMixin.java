package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.DedicatedCpuScenario;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class DedicatedScenarioMixin {
    @Unique private DedicatedCpuScenario ae2craftingtime_test_driver$dedicated;

    @Inject(method = "tickChildren", at = @At("TAIL"))
    private void ae2craftingtime_test_driver$dedicatedTick(CallbackInfo callback) {
        if (System.getProperty("ae2ct.testDriver.serverScenario") == null) return;
        if (ae2craftingtime_test_driver$dedicated == null) {
            ae2craftingtime_test_driver$dedicated = new DedicatedCpuScenario();
        }
        ae2craftingtime_test_driver$dedicated.tick((MinecraftServer) (Object) this);
    }
}
