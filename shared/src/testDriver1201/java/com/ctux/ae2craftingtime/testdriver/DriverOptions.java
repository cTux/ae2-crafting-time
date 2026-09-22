package com.ctux.ae2craftingtime.testdriver;

import java.nio.file.Path;

public record DriverOptions(String scenario, String profile, String world, Path output, boolean interactive) {
    public boolean prewarm() { return Boolean.getBoolean("ae2craftingtime.test.prewarm"); }
    public boolean resourceFixtureOnly() { return Boolean.getBoolean("ae2craftingtime.test.resourceFixtureOnly"); }
    public boolean connectedDedicated() { return Boolean.getBoolean("ae2craftingtime.test.connectedDedicated"); }
    public String dedicatedAddress() { return required("ae2craftingtime.test.dedicatedAddress"); }
    public String campaign() { return System.getProperty("ae2craftingtime.test.campaign", "local"); }
    public String resourceFixture() { return required("ae2craftingtime.test.resourceFixture"); }
    public Path continuation() {
        var value = System.getProperty("ae2craftingtime.test.continuation", "");
        return value.isBlank() ? null : Path.of(value).toAbsolutePath().normalize();
    }
    public static DriverOptions load() {
        var scenario = System.getProperty("ae2craftingtime.test.scenario", "");
        if (scenario.isEmpty()) {
            return null;
        }
        boolean resourceScenario = isResourceScenario(scenario);
        if (Boolean.getBoolean("ae2craftingtime.test.prewarm") && (!resourceScenario
                || !Boolean.getBoolean("ae2craftingtime.test.connectedDedicated"))) {
            throw new IllegalArgumentException("prewarm requires a connected resource scenario");
        }
        if (!scenario.equals("suite") && !resourceScenario && !AddonCpuFixture.supports(scenario)) {
            throw new IllegalArgumentException("unsupported test-driver scenario: " + scenario);
        }
        if (!scenario.equals("suite") && Boolean.getBoolean("ae2craftingtime.test.resourceFixtureOnly")
                && !resourceScenario) {
            throw new IllegalArgumentException("resource fixture mode requires a resource scenario");
        }
        var profile = required("ae2craftingtime.test.profile");
        if (!profile.equals("compatible") && !profile.equals("latest")) {
            throw new IllegalArgumentException("unsupported test-driver profile: " + profile);
        }
        var world = required("ae2craftingtime.test.world");
        if (!world.matches("ae2ct-[a-f0-9]{32}")) {
            throw new IllegalArgumentException("invalid disposable world ID");
        }
        return new DriverOptions(scenario, profile, world,
                Path.of(required("ae2craftingtime.test.output")).toAbsolutePath().normalize(),
                Boolean.getBoolean("ae2craftingtime.test.interactive"));
    }

    static boolean isResourceScenario(String scenario) {
        return scenario.equals("delayed-resource-icons") || scenario.equals("appmek-resource-icons");
    }

    private static String required(String name) {
        var value = System.getProperty(name, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("missing system property " + name);
        }
        return value;
    }
}
