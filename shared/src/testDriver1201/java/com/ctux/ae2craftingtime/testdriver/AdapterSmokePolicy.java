package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.core.IntegrationCatalog;
import com.ctux.ae2craftingtime.core.IntegrationSelection;
import java.util.Map;

/** The driver enforces the catalogue's order instead of maintaining another version list. */
final class AdapterSmokePolicy {
    private static final Map<String, String> DEPENDENCIES = Map.ofEntries(
            Map.entry("merequester-read-recovery", "merequester"), Map.entry("neoeco-cpu", "neoecoae"),
            Map.entry("advancedae-cpu", "advanced_ae"), Map.entry("lightningtech-cpu", "ae2lt"),
            Map.entry("merequester-screen", "merequester"), Map.entry("neoeco-fastpath-cpu", "neoecoae"),
            Map.entry("data-energistics-trinity-cpu", "data_energistics"),
            Map.entry("data-energistics-trinity-counted-cpu", "data_energistics"),
            Map.entry("data-energistics-trinity-reload-cpu", "data_energistics"),
            Map.entry("data-energistics-trinity-cancel-cpu", "data_energistics"),
            Map.entry("data-energistics-trinity-global-option-cpu", "data_energistics"),
            Map.entry("data-energistics-trinity-provider-cpu", "data_energistics"),
            Map.entry("data-energistics-trinity-admission-cpu", "data_energistics"));

    static void verifyCpu(String target, String scenario, DispatchObservation.Snapshot observed) {
        var dependency = DEPENDENCIES.getOrDefault(scenario, "");
        var expected = switch (dependency) {
            case "neoecoae" -> "cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic";
            case "advanced_ae" -> "net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU";
            case "ae2lt" -> "com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic";
            case "data_energistics" -> "com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu";
            default -> "";
        };
        if (expected.isEmpty() || IntegrationCatalog.CANDIDATES.stream()
                .noneMatch(c -> c.dependency().equals(dependency) && c.targets().contains(target))) return;
        if (!expected.equals(observed.scopeType())) {
            throw new IllegalStateException("Smoke craft ran on " + observed.scopeType() + "; expected " + expected);
        }
    }

    static void verify(String target, String scenario, String language,
            Map<String, IntegrationSelection.Decision> decisions) {
        if (!language.equals("en_us")) throw new IllegalStateException("Smoke requires English: " + language);
        var dependency = DEPENDENCIES.get(scenario);
        if (dependency == null) return;
        var catalogue = new IntegrationSelection(IntegrationCatalog.CANDIDATES, target, true,
                id -> null, candidate -> null, decision -> {});
        var newest = catalogue.newestVariant(dependency);
        if (newest.isEmpty()) return; // Native coverage can exist without a custom adapter on this target.
        var actual = decisions.get(dependency);
        if (actual == null || !actual.variant().equals(newest)) {
            throw new IllegalStateException("Smoke requires newest adapter " + dependency + "/" + newest
                    + "; selected=" + actual);
        }
    }

    private AdapterSmokePolicy() {}
}
