package com.ctux.ae2craftingtime.testdriver;

import java.nio.file.Path;

public record DriverOptions(String scenario, String profile, String world, Path output, boolean interactive) {
    public static DriverOptions load() {
        var scenario = System.getProperty("ae2craftingtime.test.scenario", "");
        if (scenario.isEmpty()) {
            return null;
        }
        if (!AddonCpuFixture.supports(scenario)) {
            throw new IllegalArgumentException("unsupported test-driver scenario: " + scenario);
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

    private static String required(String name) {
        var value = System.getProperty(name, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("missing system property " + name);
        }
        return value;
    }
}
