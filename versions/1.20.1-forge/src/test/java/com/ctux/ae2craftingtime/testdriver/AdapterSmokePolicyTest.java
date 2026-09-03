package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;

import com.ctux.ae2craftingtime.core.IntegrationSelection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdapterSmokePolicyTest {
    @Test
    void countsFromAnotherCpuCannotProveTheRequestedAdapter() {
        var nativeCpu = snapshot("appeng.me.cluster.implementations.CraftingCPUCluster");
        for (var entry : Map.of(
                "neoeco-cpu", "cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic",
                "advancedae-cpu", "net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU",
                "lightningtech-cpu", "com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic").entrySet()) {
            assertThrows(IllegalStateException.class,
                    () -> AdapterSmokePolicy.verifyCpu("1.20.1-forge", entry.getKey(), nativeCpu));
            assertDoesNotThrow(() -> AdapterSmokePolicy.verifyCpu("1.20.1-forge", entry.getKey(), snapshot(entry.getValue())));
        }
        assertDoesNotThrow(() -> AdapterSmokePolicy.verifyCpu("26.1.2-neoforge", "lightningtech-cpu", nativeCpu));
        assertDoesNotThrow(() -> AdapterSmokePolicy.verifyCpu("1.20.1-forge", "craft-plan", nativeCpu));
    }

    private static DispatchObservation.Snapshot snapshot(String type) {
        return new DispatchObservation.Snapshot(1, 1, 1, 1, 1, true, 0, type, 1);
    }

    @Test
    void requiresEnglishAndTheNewestApplicableAdapterWithoutBlockingNativeCoverage() {
        var old = new IntegrationSelection.Decision("neoecoae", "20.3.0", "pending-accounting", "selected",
                Set.of("ECOCraftingCpuLogicMixin", "NeoEcoPendingDispatchMixin"), List.of());
        var current = new IntegrationSelection.Decision("neoecoae", "20.4.2", "batched-long", "selected",
                Set.of("ECOCraftingCpuLogicMixin", "NeoEcoLongBatchDispatchMixin"), List.of());
        assertThrows(IllegalStateException.class,
                () -> AdapterSmokePolicy.verify("1.20.1-forge", "craft-plan", "uk_ua", Map.of()));
        assertThrows(IllegalStateException.class,
                () -> AdapterSmokePolicy.verify("1.20.1-forge", "neoeco-cpu", "en_us", Map.of()));
        assertThrows(IllegalStateException.class,
                () -> AdapterSmokePolicy.verify("1.20.1-forge", "neoeco-cpu", "en_us", Map.of("neoecoae", old)));
        assertDoesNotThrow(() -> AdapterSmokePolicy.verify("1.20.1-forge", "neoeco-cpu", "en_us",
                Map.of("neoecoae", current)));
        assertDoesNotThrow(() -> AdapterSmokePolicy.verify("1.20.1-forge", "craft-plan", "en_us", Map.of()));
        assertDoesNotThrow(() -> AdapterSmokePolicy.verify("26.1.2-neoforge", "lightningtech-cpu", "en_us", Map.of()));
    }
}
