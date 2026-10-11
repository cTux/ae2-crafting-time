package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import org.junit.jupiter.api.Test;

class CompactHoverLifecycleTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path output;

    @Test
    void nativeAdapterRestoresBeforeFailureAndCancellation() throws Exception {
        var features = ClientOptionsRuntime.current().features();
        boolean initial = features.enabled(OptionFeature.COMPACT_HOVER_NUMBERS);
        try {
            for (boolean original : new boolean[] {false, true}) {
                features.setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, original);
                var options = new DriverOptions("compact-hover-numbers", "compatible", "disposable", output, false);
                var scenario = new CraftPlanScenario(null, options, "test-driver.jar");
                features.setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, !original);
                var fail = CraftPlanScenario.class.getDeclaredMethod("fail", String.class, String.class, String.class);
                fail.setAccessible(true);
                try { fail.invoke(scenario, "capture", "frame", "cancelled"); }
                catch (java.lang.reflect.InvocationTargetException expected) {
                    // A headless capture may fail after the cleanup boundary has run.
                    assertInstanceOf(NullPointerException.class, expected.getCause());
                }
                assertEquals(original, features.enabled(OptionFeature.COMPACT_HOVER_NUMBERS));
                var next = new CraftPlanScenario(null, options, "test-driver.jar");
                features.setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, !original);
                next.cleanup();
                next.cleanup();
                assertEquals(original, features.enabled(OptionFeature.COMPACT_HOVER_NUMBERS));
            }
            var bytecode = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(TestDriverRuntime.class.getName()).accept(bytecode, 0);
            for (String method : new String[] {"close", "switchCase", "tickLifecycle"}) {
                assertTrue(bytecode.methods.stream().filter(candidate -> candidate.name.equals(method))
                        .anyMatch(candidate -> java.util.Arrays.stream(candidate.instructions.toArray())
                                .filter(org.objectweb.asm.tree.MethodInsnNode.class::isInstance)
                                .map(org.objectweb.asm.tree.MethodInsnNode.class::cast)
                                .anyMatch(call -> call.owner.endsWith("/CraftPlanScenario") && call.name.equals("cleanup"))), method);
            }
        } finally { features.setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, initial); }
    }
    @Test
    void failedCaptureAndCancellationRestoreTheNextLeafOption() {
        var features = ClientOptionsRuntime.current().features();
        boolean initial = features.enabled(OptionFeature.COMPACT_HOVER_NUMBERS);
        try {
            for (boolean original : new boolean[] {false, true}) {
                for (String path : new String[] {"failed-capture", "cancellation", "success"}) {
                    features.setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, original);
                    var scenario = new CompactHoverNumbersScenario();
                    try {
                        features.setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, !original);
                        if (!path.equals("success")) throw new IllegalStateException(path);
                    } catch (IllegalStateException error) {
                        assertEquals(path, error.getMessage());
                    } finally {
                        scenario.close();
                    }
                    scenario.close();
                    assertEquals(original, features.enabled(OptionFeature.COMPACT_HOVER_NUMBERS));
                    var next = new StandardAe2Scenario("compact-hover-numbers", "disposable", java.nio.file.Path.of("."), false);
                    next.cleanup();
                    assertEquals(original, features.enabled(OptionFeature.COMPACT_HOVER_NUMBERS));
                }
            }
        } finally {
            features.setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, initial);
        }
    }
}
