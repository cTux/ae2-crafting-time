package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

record SuitePlan(int schema, List<Case> cases) {
    record Case(String scenario, String world) { }

    static List<DriverOptions> read(DriverOptions options) throws IOException {
        var plan = new Gson().fromJson(Files.readString(options.output().resolve("suite-plan.json")), SuitePlan.class);
        if (plan == null) {
            throw new IllegalArgumentException("missing suite plan");
        }
        return plan.options(options);
    }

    List<DriverOptions> options(DriverOptions options) {
        if (schema != 1 || cases == null || cases.isEmpty() || cases.size() > 64 || options.interactive()) {
            throw new IllegalArgumentException("invalid suite schema, case count, or interactive mode");
        }
        var scenarios = new HashSet<String>();
        var worlds = new HashSet<String>();
        for (var item : cases) {
            if (item == null || item.scenario == null || !AddonCpuFixture.supports(item.scenario)
                    || item.world == null || !item.world.matches("ae2ct-[a-f0-9]{32}")
                    || !scenarios.add(item.scenario) || !worlds.add(item.world)) {
                throw new IllegalArgumentException("invalid or duplicate suite case");
            }
        }
        if (!cases.get(0).world.equals(options.world())) {
            throw new IllegalArgumentException("suite first world does not match launch world");
        }
        return cases.stream().map(item -> new DriverOptions(item.scenario, options.profile(), item.world,
                options.output().resolve(item.scenario), false)).toList();
    }

    static void verifyWorld(Path saves, DriverOptions options) throws IOException {
        var world = saves.resolve(options.world());
        if (!world.toRealPath().equals(saves.toRealPath().resolve(options.world()))
                || !FixtureMarker.read(world).disposableWorldId().equals(options.world())) {
            throw new IllegalArgumentException("suite world is linked or marker does not match");
        }
    }
}
