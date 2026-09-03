package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IntegrationSelectionTest {
    private static IntegrationSelection.Candidate candidate(String dependency, String id, boolean client,
            String... mixins) {
        return new IntegrationSelection.Candidate(dependency, id, Set.of("forge"), client, Set.of(mixins), List.of());
    }

    @Test
    void choicesAreAtomicOrderedIndependentAndPermanent() throws Exception {
        var probes = new AtomicInteger();
        var reports = new ArrayList<IntegrationSelection.Decision>();
        var versions = new HashMap<>(Map.of("eco", "20.4.2", "tree", "1.0.1"));
        var selection = new IntegrationSelection(List.of(
                candidate("eco", "new", false, "New", "Common"),
                candidate("eco", "old", false, "Old", "Common"),
                candidate("tree", "new", true, "Tree")), "forge", true, versions::get,
                c -> { probes.incrementAndGet(); return null; }, reports::add);
        assertTrue(selection.shouldApply("Core"));
        assertTrue(selection.snapshot().isEmpty());
        var pool = Executors.newFixedThreadPool(4);
        try {
            var calls = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 40; i++) calls.add(() -> selection.shouldApply("Common"));
            for (var result : pool.invokeAll(calls)) assertTrue(result.get());
        } finally {
            pool.shutdownNow();
        }
        assertFalse(selection.shouldApply("Old"));
        assertTrue(selection.shouldApply("New"));
        assertEquals(1, probes.get());
        versions.clear(); // Later world/settings/metadata changes cannot reselect.
        assertTrue(selection.shouldApply("New"));
        assertEquals(Set.of("New", "Common"), selection.snapshot().get("eco").mixins());
        assertFalse(selection.shouldApply("Tree"));
        assertEquals("absent", selection.snapshot().get("tree").reason());
        assertEquals(2, reports.size());
        assertEquals("new", selection.newestVariant("eco"));
        assertEquals("", selection.newestVariant("missing"));
        assertThrows(UnsupportedOperationException.class, () -> selection.snapshot().clear());
        assertThrows(UnsupportedOperationException.class, () -> reports.get(0).mixins().clear());
    }

    @Test
    void checksEligibilityBeforeProbingAndFallsBackOnlyBeforeSelection() {
        var candidates = List.of(candidate("tree", "new", true, "New", "Common"),
                candidate("tree", "old", true, "Old", "Common"));
        for (var mode : List.of("absent", "unsupported_target", "wrong_side", "old", "new", "both", "neither")) {
            var probed = new ArrayList<String>();
            var selection = new IntegrationSelection(candidates,
                    mode.equals("unsupported_target") ? "fabric" : "forge", !mode.equals("wrong_side"),
                    dep -> mode.equals("absent") ? null : "1.0.0",
                    c -> {
                        probed.add(c.variant());
                        return mode.equals(c.variant()) || mode.equals("both") ? null : "missing member";
                    }, ignored -> {});
            assertEquals(Set.of("old", "new", "both").contains(mode), selection.shouldApply("Common"));
            var decision = selection.snapshot().get("tree");
            switch (mode) {
                case "absent", "unsupported_target", "wrong_side" -> {
                    assertEquals(mode, decision.reason());
                    assertTrue(probed.isEmpty());
                }
                case "old" -> {
                    assertEquals("old", decision.variant());
                    assertEquals(Set.of("Old", "Common"), decision.mixins());
                    assertEquals(List.of("new:missing member"), decision.rejected());
                }
                case "neither" -> {
                    assertEquals("no_compatible_variant", decision.reason());
                    assertEquals(List.of("new", "old"), probed);
                    assertEquals("", decision.variant());
                }
                default -> {
                    assertEquals("new", decision.variant());
                    assertEquals(Set.of("New", "Common"), decision.mixins());
                    assertEquals(List.of("new"), probed);
                }
            }
            if (mode.equals("wrong_side")) assertEquals("", selection.newestVariant("tree"));
        }
        var independent = new IntegrationSelection(List.of(candidate("a", "one", false, "A"),
                candidate("b", "one", false, "B")), "forge", false, dep -> "1", c -> null, ignored -> {});
        assertTrue(independent.shouldApply("A"));
        assertTrue(independent.shouldApply("B"));
        assertEquals("one", independent.newestVariant("a"));
    }

    @Test
    void validatesOwnershipAndDoesNotCacheBootstrapFailures() {
        var one = candidate("eco", "one", false, "Hook");
        assertThrows(IllegalArgumentException.class, () -> new IntegrationSelection(List.of(one, one),
                "forge", true, d -> "1", c -> null, d -> {}));
        assertThrows(IllegalArgumentException.class, () -> new IntegrationSelection(List.of(one,
                candidate("other", "one", false, "Hook")), "forge", true, d -> "1", c -> null, d -> {}));
        var failure = new IllegalStateException("bootstrap failed");
        var metadata = new IntegrationSelection(List.of(one), "forge", true, d -> { throw failure; },
                c -> fail("probe must not run"), d -> fail("no report for a failed decision"));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> metadata.shouldApply("Hook")));
        assertTrue(metadata.snapshot().isEmpty());
        var probing = new IntegrationSelection(List.of(one), "forge", true, d -> "1", c -> { throw failure; },
                d -> fail("no report for a failed decision"));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> probing.shouldApply("Hook")));
        assertTrue(probing.snapshot().isEmpty());
    }

    @Test
    void namedBytecodeContractsHandleAbsenceInheritanceAndWrongReturnTypes() throws Exception {
        var method = new IntegrationContract.Member("Child", "method:click", "\\(DDI\\)Z");
        var classes = new HashMap<String, IntegrationContract.ClassInfo>();
        assertEquals("missing:Child#method:click", IntegrationContract.check(List.of(method), classes::get));
        classes.put("Child", new IntegrationContract.ClassInfo("Parent", Map.of("method:click", List.of("(DDI)V"))));
        classes.put("Parent", new IntegrationContract.ClassInfo("java/lang/Object", Map.of()));
        assertNotNull(IntegrationContract.check(List.of(method), classes::get));
        classes.put("Parent", new IntegrationContract.ClassInfo(null, Map.of()));
        assertNotNull(IntegrationContract.check(List.of(method), classes::get));
        classes.put("Parent", new IntegrationContract.ClassInfo("java/lang/Object", Map.of("method:click", List.of("(DDI)Z"))));
        assertNull(IntegrationContract.check(List.of(method), classes::get));
        assertNull(IntegrationContract.check(List.of(new IntegrationContract.Member("Child", "", "")), classes::get));
        classes.put("Parent", new IntegrationContract.ClassInfo("Child", Map.of()));
        assertThrows(IllegalStateException.class, () -> IntegrationContract.check(List.of(method), classes::get));
        var failure = new java.io.UncheckedIOException(new java.io.IOException("read failed"));
        assertSame(failure, assertThrows(java.io.UncheckedIOException.class,
                () -> IntegrationContract.check(List.of(method), name -> { throw failure; })));
    }
}
