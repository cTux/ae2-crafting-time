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

    @Test
    void everyTrinityLeafRequiresItsNativeWorkerAndSelectedAdapter() {
        var nativeCpu = snapshot("appeng.me.cluster.implementations.CraftingCPUCluster");
        var trinityCpu = snapshot("com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu");
        var selected = new IntegrationSelection.Decision("data_energistics", "3.3.5", "trinity-3-3-5", "selected",
                Set.of("TrinityCpuLogicMixin"), List.of());
        for (var scenario : List.of("data-energistics-trinity-cpu", "data-energistics-trinity-counted-cpu",
                "data-energistics-trinity-cancel-cpu", "data-energistics-trinity-global-option-cpu",
                "data-energistics-trinity-provider-cpu", "data-energistics-trinity-admission-cpu",
                "data-energistics-trinity-reload-cpu")) {
            assertThrows(IllegalStateException.class,
                    () -> AdapterSmokePolicy.verifyCpu("1.21.1-neoforge", scenario, nativeCpu), scenario);
            assertDoesNotThrow(() -> AdapterSmokePolicy.verifyCpu("1.21.1-neoforge", scenario, trinityCpu), scenario);
            assertThrows(IllegalStateException.class,
                    () -> AdapterSmokePolicy.verify("1.21.1-neoforge", scenario, "en_us", Map.of()), scenario);
            assertDoesNotThrow(() -> AdapterSmokePolicy.verify("1.21.1-neoforge", scenario, "en_us",
                    Map.of("data_energistics", selected)), scenario);
        }
    }
}
