package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.FeatureOptions;
import com.ctux.ae2craftingtime.core.OptionFeature;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

class RecurrentPlanFixtureOptionsTest {
    @Test void recurrentBadgeFollowsTheClientSwitchAndContainsTheLabel() {
        var text = new Rect(2, 2, 10, 8);
        var containing = new Rect(0, 0, 20, 12);
        var unrelated = new Rect(30, 0, 20, 12);
        assertTrue(StandardAe2Scenario.recurrentBadgeMatches(java.util.List.of(), text, false));
        assertFalse(StandardAe2Scenario.recurrentBadgeMatches(java.util.List.of(), text, true));
        assertTrue(StandardAe2Scenario.recurrentBadgeMatches(java.util.List.of(containing), text, true));
        assertFalse(StandardAe2Scenario.recurrentBadgeMatches(java.util.List.of(containing), text, false));
        assertTrue(StandardAe2Scenario.recurrentBadgeMatches(java.util.List.of(unrelated), text, false));
        assertFalse(StandardAe2Scenario.recurrentBadgeMatches(java.util.List.of(unrelated), text, true));
    }

    @Test void temporaryDetectionPreservesBothOriginalValuesAcrossRepeatedSetup() {
        for (boolean original : new boolean[] {false, true}) {
            var options = new FeatureOptions(OptionFeature.Owner.SERVER);
            options.setEnabled(OptionFeature.RECURRENT_DETECTION, original);
            options.setEnabled(OptionFeature.NOTIFY_ON_DELAYED, false);
            var originalDisabled = options.disabled();
            var fixture = new RecurrentPlanFixture(null);

            assertTrue(fixture.enableDetection(options));
            assertTrue(options.enabled(OptionFeature.RECURRENT_DETECTION));
            assertFalse(fixture.enableDetection(options));
            assertTrue(options.enabled(OptionFeature.RECURRENT_DETECTION));
            assertTrue(fixture.restoreDetection(options));
            assertEquals(original, options.enabled(OptionFeature.RECURRENT_DETECTION));
            assertEquals(originalDisabled, options.disabled());
            assertFalse(fixture.restoreDetection(options));
            fixture.close();
            fixture.close();
            options.setEnabled(OptionFeature.RECURRENT_DETECTION, !original);
            assertTrue(fixture.enableDetection(options));
            assertTrue(fixture.restoreDetection(options));
            assertEquals(!original, options.enabled(OptionFeature.RECURRENT_DETECTION));
        }

        new RecurrentPlanFixture(null).close();
    }

    @Test void adaptersRetainSetupAndCleanupDelegation() throws Exception {
        var fixture = bytecode(RecurrentPlanFixture.class);
        assertCalls(fixture, "prepare", fixture.name, "enableDetection");
        var optionsRuntime = "com/ctux/ae2craftingtime/mc1201/ServerOptionsRuntime";
        assertCalls(fixture, "prepare", optionsRuntime, "sendTo");
        assertCalls(fixture, "close", fixture.name, "restoreDetection");
        assertCalls(fixture, "close", optionsRuntime, "sendTo");
        assertCalls(fixture, "close", "appeng/api/networking/IManagedGridNode", "destroy");
        var dedicated = bytecode(DedicatedCpuScenario.class);
        assertCalls(dedicated, "stepRecurrentConnected", fixture.name, "close");
        assertCalls(dedicated, "finish", fixture.name, "close");
    }

    private static ClassNode bytecode(Class<?> type) throws Exception {
        var node = new ClassNode();
        try (var input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static void assertCalls(ClassNode node, String method, String owner, String called) {
        var body = node.methods.stream().filter(candidate -> candidate.name.equals(method)).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(body.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast)
                .anyMatch(call -> call.owner.equals(owner) && call.name.equals(called)),
                method + " must delegate to " + owner + "." + called);
    }
}
