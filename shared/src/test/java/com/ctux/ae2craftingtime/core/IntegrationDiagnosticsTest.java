package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class IntegrationDiagnosticsTest {
    @Test void loadedConfigSkipsCaptureHooksUntilReenabled() {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var report = new IntegrationDiagnostics("1.20.1-forge", true,
                id -> id.equals("merequester") ? null : "1", Map.of(), events::add);
        report.configureProfiling(false);
        for (var capability : IntegrationDiagnostics.CPU) {
            assertTrue(events.stream().anyMatch(event -> event.message().contains("integration=ae2craftingtime ")
                    && event.message().contains("capability=" + capability + " state=skipped")));
        }
        assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, report.outcome("neoecoae"));
        assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, report.outcome("merequester"));
        int count = events.size();
        report.configureProfiling(false);
        assertEquals(count, events.size());
        report.configureProfiling(true);
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("neoecoae"));
        assertTrue(events.stream().noneMatch(event -> event.message().contains("state=confirmed")));
    }

    @Test void inventoryAndIndependentHookEvidence() {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var report = new IntegrationDiagnostics("1.20.1-forge", true, id -> "1", Map.of(), events::add);
        assertEquals(26, IntegrationDiagnostics.CATALOG.size());
        assertFalse(report.disabled("ae2ct", "node"));
        assertEquals(26, IntegrationDiagnostics.CATALOG.stream().map(IntegrationDiagnostics.Entry::id).distinct().count());
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("ae2ct"));
        report.observe("ae2ct", "node");
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("ae2ct"));
        assertTrue(events.get(events.size() - 1).message().contains("pending=[layout, tooltip, details, reset]"));
        for (var entry : IntegrationDiagnostics.CATALOG) {
            for (var capability : entry.capabilities()) report.observe(entry.id(), capability);
            assertEquals(entry.mode().equals("compatibility-only") ? IntegrationDiagnostics.Outcome.SKIPPED
                    : IntegrationDiagnostics.Outcome.INITIALIZED, report.outcome(entry.id()));
        }
        report.summary();
        assertTrue(events.get(events.size() - 1).message().contains("initialized=24 skipped=2 pending=0 partial=0 failed=0"));
        int count = events.size();
        report.summary();
        report.observe("ae2ct", "node");
        assertEquals(count, events.size());
    }

    @Test void absenceSideTargetAndAliasesAreMetadataOnly() {
        for (var target : List.of("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")) {
            for (var client : List.of(false, true)) {
                var events = new ArrayList<IntegrationDiagnostics.Event>();
                var report = new IntegrationDiagnostics(target, client, id -> id.equals("ae2craftingtime") ? "1" : null,
                        Map.of(), events::add);
                IntegrationDiagnostics.CATALOG.forEach(entry -> assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, report.outcome(entry.id())));
                report.observe("ae2ct", "node");
                report.observe("ae2craftingtime", "cpu-dispatch");
                report.configured("ae2ct", "node", true);
                assertFalse(report.available("ae2ct"));
                report.disable("ae2ct", "irrelevant", null);
                report.summary();
                assertTrue(events.stream().allMatch(event -> event.level().equals("INFO")));
                assertTrue(events.get(events.size() - 1).message().contains("skipped=26"));
            }
        }
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        new IntegrationDiagnostics("1.20.1-forge", true,
                Map.of("ae2insertexportcard", "actual-fork-version")::get, Map.of(), events::add);
        assertEquals(1, events.stream().filter(event -> event.message().contains("version=actual-fork-version")).count());
        assertTrue(events.stream().anyMatch(event -> event.message().contains("integration=ae2importexportcard mod=ae2insertexportcard")));
    }

    @Test void selectionDoesNotConfirmActivation() {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var selected = new IntegrationSelection.Decision("ae2ct", "1", "tree-layout", "selected", Set.of("mixin"), List.of());
        var rejected = new IntegrationSelection.Decision("merequester", "2", "", "no_compatible_variant", Set.of(), List.of("missing method"));
        var report = new IntegrationDiagnostics("1.21.1-neoforge", true, id -> "1",
                Map.of("ae2ct", selected, "merequester", rejected), events::add);
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("ae2ct"));
        assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, report.outcome("merequester"));
        assertTrue(events.stream().anyMatch(event -> event.message().contains("variant=tree-layout")));
        assertTrue(events.stream().anyMatch(event -> event.message().contains("preflight_no_compatible_variant")));
    }

    @Test void failuresAreTerminalAndRetainTheirFirstCause() {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var report = new IntegrationDiagnostics("1.20.1-forge", true, id -> "1", Map.of(), events::add);
        var failure = new IllegalStateException("broken getter");
        report.fail("advanced_ae", "selected-cpu", "missing field", false, failure);
        assertEquals(IntegrationDiagnostics.Outcome.PARTIAL, report.outcome("advanced_ae"));
        assertSame(failure, events.get(events.size() - 1).cause());
        assertEquals("WARN", events.get(events.size() - 1).level());
        report.observe("advanced_ae", "cpu-submit");
        assertEquals(IntegrationDiagnostics.Outcome.PARTIAL, report.outcome("advanced_ae"));
        int count = events.size();
        report.fail("advanced_ae", "selected-cpu", "second", false, new RuntimeException());
        report.observe("advanced_ae", "selected-cpu");
        report.configured("advanced_ae", "selected-cpu", true);
        assertEquals(count, events.size());
        assertTrue(report.disabled("advanced_ae", "selected-cpu"));
        report.observe("ae2ct", "node");
        report.disable("ae2ct", "missing member", failure);
        assertEquals(IntegrationDiagnostics.Outcome.FAILED, report.outcome("ae2ct"));
        count = events.size();
        report.disable("ae2ct", "again", failure);
        assertEquals(count, events.size());
        report.fail("wcwt", "tooltip", "missing method", false, failure);
        assertEquals(IntegrationDiagnostics.Outcome.FAILED, report.outcome("wcwt"));
        report.fail("ae2craftingtime", "network-registration", "channel failed", true, failure);
        assertEquals(IntegrationDiagnostics.Outcome.FAILED, report.outcome("ae2craftingtime"));
        assertEquals("ERROR", events.get(events.size() - 1).level());
        assertTrue(events.get(events.size() - 1).message().contains("action=propagate"));
        report.summary();
        assertTrue(events.get(events.size() - 1).message().contains("partial=1 failed=2"));
    }

    @Test void nativeScopeAndConfigurationRemainHonest() {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var report = new IntegrationDiagnostics("1.20.1-forge", true, id -> "1", Map.of(), events::add);
        for (var capability : IntegrationDiagnostics.CPU) report.observe("ae2craftingtime", capability);
        assertEquals(IntegrationDiagnostics.Outcome.INITIALIZED, report.outcome("molecularmanipulator"));
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("appbot"));
        assertTrue(events.stream().anyMatch(event -> event.message().contains("shared_hooks_observed;addon_job_and_resource_contract_not_verified")));
        report.observe("ae2craftingtime", "key-normalization");
        report.observe("ae2craftingtime", "mana-normalization");
        assertEquals(IntegrationDiagnostics.Outcome.INITIALIZED, report.outcome("appbot"));
        for (var capability : List.of("layout", "node", "tooltip", "details", "reset")) report.configured("ae2ct", capability, false);
        assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, report.outcome("ae2ct"));
        report.configured("ae2ct", "node", false);
        report.configured("ae2ct", "node", true);
        report.observe("ae2ct", "node");
        assertEquals(IntegrationDiagnostics.Outcome.INITIALIZED, report.outcome("ae2ct"));
        int count = events.size();
        report.configured("ae2ct", "node", false);
        report.configured("ae2ct", "node", true);
        report.observe("ae2ct", "node");
        assertEquals(count, events.size());
    }

    @Test void concurrentRepeatedCallbacksDoNotRepeatMessages() throws Exception {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var report = new IntegrationDiagnostics("1.20.1-forge", true, id -> "1", Map.of(), events::add);
        var pool = Executors.newFixedThreadPool(4);
        try {
            var tasks = new ArrayList<java.util.concurrent.Callable<Void>>();
            for (int i = 0; i < 100; i++) tasks.add(() -> { report.observe("ae2ct", "node"); return null; });
            for (var future : pool.invokeAll(tasks)) future.get();
        } finally { pool.shutdownNow(); }
        assertEquals(1, events.stream().filter(event -> event.message().contains("capability=node")).count());
    }

    @Test void metadataIsBoundedAndCannotInjectLogLines() {
        assertEquals("unknown", IntegrationDiagnostics.clean(null));
        assertEquals("unknown", IntegrationDiagnostics.clean(" \n"));
        assertEquals("a_b_c_d", IntegrationDiagnostics.clean("a\nb\tc\u0000d"));
        assertEquals(256, IntegrationDiagnostics.clean("a".repeat(300)).length());
        assertEquals("1.2.3", IntegrationDiagnostics.clean("1.2.3"));
    }
    @Test void uiAttributionAndConfigurationOnlyConfirmExecutedPaths() {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var report = new IntegrationDiagnostics("1.20.1-forge", true, id -> "1", Map.of(), events::add);
        report.growth("plan-row", 1, 1);
        assertEquals(27, events.size());
        report.growth("plan-row", 1, 2);
        report.wireless("unrelated", false);
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("wcwt"));
        report.wireless("com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen", false);
        assertEquals(IntegrationDiagnostics.Outcome.INITIALIZED, report.outcome("wcwt"));
        report.wireless("de.mari_023.ae2wtlib.wct.WCTScreen", true);
        assertEquals(IntegrationDiagnostics.Outcome.INITIALIZED, report.outcome("ae2wtlib"));
        assertEquals(IntegrationDiagnostics.Outcome.INITIALIZED, report.outcome("ae2importexportcard"));
        report.normalized("minecraft:iron_ingot");
        report.normalized("botania:mana");
        report.configureGroup("ae2ct", false);
        report.configureGroup("ae2ct", false);
        assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, report.outcome("ae2ct"));
        report.configureGroup("ae2ct", true);
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("ae2ct"));
        report.configureGroup("ae2craftingtime", false);
        assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, report.outcome("appbot"));
        report.configureGroup("ae2craftingtime", true);
        assertEquals(IntegrationDiagnostics.Outcome.PENDING, report.outcome("appbot"));
        report.normalized("botania:mana");
        var absent = new IntegrationDiagnostics("1.20.1-forge", true, id -> null, Map.of(), event -> {});
        absent.configureGroup("ae2ct", true);
        assertEquals(IntegrationDiagnostics.Outcome.SKIPPED, absent.outcome("ae2ct"));
    }
    @Test void requiredRegistrationPreservesFailuresAndPositiveMeansPositive() {
        var events = new ArrayList<IntegrationDiagnostics.Event>();
        var report = new IntegrationDiagnostics("1.20.1-forge", true, id -> "1", Map.of(), events::add);
        var called = new java.util.concurrent.atomic.AtomicBoolean();
        report.required("config-registration", () -> called.set(true));
        assertTrue(called.get());
        var failure = new IllegalArgumentException("broken registration");
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> report.required("network-registration", () -> { throw failure; })));
        var fatal = new AssertionError("linkage boundary");
        assertSame(fatal, assertThrows(AssertionError.class,
                () -> report.required("key-registration", () -> { throw fatal; })));
        report.positive("neoecoae", "cpu-dispatch-fastpath", 0, true);
        assertFalse(events.stream().anyMatch(event -> event.message().contains("capability=cpu-dispatch-fastpath")));
        report.positive("neoecoae", "cpu-dispatch-fastpath", 1, false);
        report.positive("neoecoae", "cpu-dispatch-fastpath", 1, true);
        assertTrue(events.stream().anyMatch(event -> event.message().contains("capability=cpu-dispatch-fastpath state=confirmed")));
        var aliasEvents = new ArrayList<IntegrationDiagnostics.Event>();
        new IntegrationDiagnostics("1.21.1-neoforge", true, Map.of("extendedae", "1.21-2.2.35-neoforge")::get,
                Map.of(), aliasEvents::add);
        assertTrue(aliasEvents.stream().anyMatch(event -> event.message().contains("integration=expatternprovider mod=extendedae version=1.21-2.2.35-neoforge")));
    }
}
