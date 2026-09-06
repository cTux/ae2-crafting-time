package com.ctux.ae2craftingtime.testdriver;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record DriverResult(
        int schema,
        boolean complete,
        String driver,
        String target,
        String profile,
        String scenario,
        String result,
        String language,
        Map<String, com.ctux.ae2craftingtime.core.IntegrationSelection.Decision> adapters,
        DispatchObservation.Snapshot dispatch,
        Map<String, Boolean> checks,
        List<String> screenshots,
        Failure failure) {
    public static final List<String> CRAFT_PLAN_CHECKS = List.of(
            "screen", "ttc-row", "total-ttc", "sort-cycle", "tooltip", "layout");
    public static final List<String> ADDON_CPU_CHECKS = List.of(
            "cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample");
    public static final List<String> WIRELESS_TERMINAL_CHECKS = List.of("screen", "ttc-tooltip", "plan-ttc");
    public static final List<String> ME_REQUESTER_CHECKS = List.of("screen", "ttc-row", "total-ttc", "layout");
    public static final List<String> VISUAL_TOOL_CHECKS = List.of("screen", "layout");
    public static final List<String> CRAFTING_TREE_CHECKS = List.of("screen", "node-ttc", "tooltip", "layout", "details", "reset");
    public static final List<String> WIRELESS_RANGE_CHECKS = List.of("screen", "plan-ttc");
    public static final List<String> READ_RECOVERY_CHECKS = List.of("screen", "host-content", "overlay-absent", "layout");
    public static final List<String> TREE_RECOVERY_CHECKS = List.of("screen", "host-content", "overlay-absent", "layout",
            "tooltip", "details-ignored", "reset-ignored");

    public DriverResult {
        adapters = Map.copyOf(adapters);
        checks = Collections.unmodifiableMap(new LinkedHashMap<>(checks));
        screenshots = List.copyOf(screenshots);
        if (!checks.keySet().equals(Set.copyOf(requiredChecks(scenario)))) {
            throw new IllegalArgumentException("result must contain exactly the required checks");
        }
    }

    public static List<String> requiredChecks(String scenario) {
        return scenario.equals(RequesterFixture.RECOVERY) ? READ_RECOVERY_CHECKS
                : scenario.equals(CraftingTreeScenario.RECOVERY) ? TREE_RECOVERY_CHECKS
                : StandardAe2Scenario.supports(scenario) ? StandardAe2Scenario.CHECKS.get(scenario)
                : scenario.equals("craft-plan") ? CRAFT_PLAN_CHECKS
                : scenario.equals(NoSpaceScenario.SCENARIO) ? NoSpaceScenario.CHECKS
                : scenario.equals(NoPowerScenario.SCENARIO) ? NoPowerScenario.CHECKS
                : scenario.equals(NoProviderScenario.SCENARIO) ? NoProviderScenario.CHECKS
                : ProviderDispatchStatusScenario.supports(scenario) ? ProviderDispatchStatusScenario.checks(scenario)
                : scenario.equals(CraftingTreeScenario.SCENARIO) ? CRAFTING_TREE_CHECKS
                : scenario.equals(RequesterFixture.SCENARIO) ? ME_REQUESTER_CHECKS
                : scenario.equals(Ae2NetworkAnalyserFixture.SCENARIO) ? VISUAL_TOOL_CHECKS
                : scenario.equals("aeinfinitybooster-terminal") ? WIRELESS_RANGE_CHECKS
                : WirelessTerminalFixture.supports(scenario) ? WIRELESS_TERMINAL_CHECKS : ADDON_CPU_CHECKS;
    }

    public record Failure(String step, String code, String expected, String observed) {
    }
}
