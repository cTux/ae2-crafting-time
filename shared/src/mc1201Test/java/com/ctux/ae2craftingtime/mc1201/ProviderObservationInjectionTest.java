package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

class ProviderObservationInjectionTest {
    private static final String ITERATOR = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;";
    private static final String MIXINS = "com/ctux/ae2craftingtime/mc1201/mixin/";

    @Test
    void nativeHookObservesIterationAfterTheAddonReplaceableLookup() throws IOException {
        var execution = method(readClass("appeng/crafting/execution/CraftingCpuLogic"), "executeCrafting");
        var calls = calls(execution);
        var lookups = calls.stream().filter(call -> call.name.equals("getProviders")).toList();
        var iterators = calls.stream().filter(call -> signature(call).equals(ITERATOR)).toList();
        assertEquals(1, lookups.size());
        assertEquals(1, iterators.size(), "Only the provider loop uses Iterable.iterator");
        assertEquals(calls.indexOf(lookups.get(0)) + 1, calls.indexOf(iterators.get(0)));
        assertIteratorHook(method(readClass(MIXINS + "CraftingCpuLogicMixin"), "ae2craftingtime$observeProviders"));
    }

    @Test
    void bothCpuHooksReturnTheSameIteratorWithoutConsumingProvidersOrRepeatingTheLookup() throws IOException {
        for (var name : List.of("CraftingCpuLogicMixin", "AdvancedCraftingCpuLogicMixin")) {
            // AdvancedAE is not packaged on Fabric.
            if (name.startsWith("Advanced") && getClass().getResource("/" + MIXINS + name + ".class") == null) {
                continue;
            }
            var handler = method(readClass(MIXINS + name), "ae2craftingtime$observeProviders");
            assertIteratorHook(handler);
            var calls = calls(handler);
            assertEquals(1, calls.stream().filter(call -> signature(call).equals(ITERATOR)).count());
            assertEquals(1, calls.stream().filter(call -> call.owner.equals("java/util/Iterator")
                    && call.name.equals("hasNext")).count());
            assertFalse(calls.stream().anyMatch(call -> call.name.equals("next") || call.name.equals("getProviders")));
            assertTrue(calls.stream().anyMatch(call -> call.name.equals("observeProviders")
                    && call.desc.endsWith("IPatternDetails;Z)V")));
            var locals = Arrays.stream(handler.instructions.toArray()).filter(VarInsnNode.class::isInstance)
                    .map(VarInsnNode.class::cast).toList();
            var stored = locals.stream().filter(instruction -> instruction.getOpcode() == Opcodes.ASTORE).toList();
            assertEquals(1, stored.size());
            assertEquals(stored.get(0).var, locals.get(locals.size() - 1).var, "Return the observed iterator");
        }
    }

    private static void assertIteratorHook(MethodNode handler) {
        var redirect = handler.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Redirect;"))
                .findFirst().orElseThrow();
        assertEquals(List.of("executeCrafting"), value(redirect, "method"));
        assertEquals(ITERATOR, value((AnnotationNode) value(redirect, "at"), "target"));
        assertFalse(redirect.values.contains("require"), "Keep the required injection check");
    }

    private static Object value(AnnotationNode annotation, String key) {
        return annotation.values.get(annotation.values.indexOf(key) + 1);
    }

    private static String signature(MethodInsnNode call) {
        return "L" + call.owner + ";" + call.name + call.desc;
    }

    private static List<MethodInsnNode> calls(MethodNode method) {
        return Arrays.stream(method.instructions.toArray()).filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast).toList();
    }

    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    private static ClassNode readClass(String name) throws IOException {
        try (var input = ProviderObservationInjectionTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input, name);
            var node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
}
