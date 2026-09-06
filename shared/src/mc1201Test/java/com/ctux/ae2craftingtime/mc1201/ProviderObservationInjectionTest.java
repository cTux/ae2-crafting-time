package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

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
        assertRedirect(method(readClass(MIXINS + "CraftingCpuLogicMixin"), "ae2craftingtime$observeProviders"),
                ITERATOR);
    }

    @Test
    void bothCpuHooksDelegateIterationBusyAndPushObservationWithoutRepeatingCalls() throws IOException {
        for (var name : List.of("CraftingCpuLogicMixin", "AdvancedCraftingCpuLogicMixin")) {
            if (name.startsWith("Advanced") && getClass().getResource("/" + MIXINS + name + ".class") == null) {
                continue;
            }
            var type = readClass(MIXINS + name);
            assertDelegates(type, "ae2craftingtime$observeProviders", ITERATOR, "iterator");
            assertDelegates(type, "ae2craftingtime$observeProviderBusy",
                    "Lappeng/api/networking/crafting/ICraftingProvider;isBusy()Z", "busy");
            assertDelegates(type, "ae2craftingtime$observeProviderPush",
                    "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
                    "push");
        }
    }

    @Test
    void providerHooksMatchEveryPinnedPushPatternCallSite() throws IOException {
        var target = method(readClass("appeng/helpers/patternprovider/PatternProviderLogic"), "pushPattern");
        var targetCalls = calls(target).stream().map(ProviderObservationInjectionTest::signature).toList();
        var mixin = readClass(MIXINS + "PatternProviderLogicMixin");
        var handlers = mixin.methods.stream().filter(method -> method.name.startsWith("ae2craftingtime$observe"))
                .toList();
        assertEquals(6, handlers.size());
        for (var handler : handlers) {
            if (handler.name.equals("ae2craftingtime$observeBlocking")) {
                var expression = annotation(handler, "/ModifyExpressionValue;");
                assertEquals(List.of("pushPattern"), value(expression, "method"));
                var at = (AnnotationNode) ((List<?>) value(expression, "at")).get(0);
                assertTrue(targetCalls.contains((String) value(at, "target")));
                continue;
            }
            var redirect = annotation(handler, "/Redirect;");
            assertEquals(List.of("pushPattern"), value(redirect, "method"));
            var targetSignature = (String) value((AnnotationNode) value(redirect, "at"), "target");
            assertTrue(targetCalls.contains(targetSignature), targetSignature);
            assertFalse(redirect.values.contains("require"));
        }
        assertEquals(2, mixin.methods.stream().filter(method -> method.name.startsWith("ae2craftingtime$")
                && method.visibleAnnotations != null
                && method.visibleAnnotations.stream().anyMatch(a -> a.desc.endsWith("/Invoker;"))).count());
        var external = getClass().getResource("/" + MIXINS + "PatternProviderExternalPushMixin.class");
        if (external != null) {
            var handler = method(readClass(MIXINS + "PatternProviderExternalPushMixin"),
                    "ae2craftingtime$observeExternalPush");
            var redirect = annotation(handler, "/Redirect;");
            assertTrue(targetCalls.contains((String) value((AnnotationNode) value(redirect, "at"), "target")));
            assertFalse(redirect.values.contains("require"));
        }
    }

    private static void assertDelegates(ClassNode type, String methodName, String target, String observerCall) {
        var handler = method(type, methodName);
        assertRedirect(handler, target);
        var calls = calls(handler);
        assertEquals(1, calls.stream().filter(call -> call.owner.endsWith("ProviderDispatchObserver")
                && call.name.equals(observerCall)).count());
        assertFalse(calls.stream().anyMatch(call -> call.name.equals("getProviders") || call.name.equals("hasNext")));
    }

    private static void assertRedirect(MethodNode handler, String target) {
        var redirect = annotation(handler, "/Redirect;");
        assertEquals(List.of("executeCrafting"), value(redirect, "method"));
        assertEquals(target, value((AnnotationNode) value(redirect, "at"), "target"));
        assertFalse(redirect.values.contains("require"));
    }

    private static AnnotationNode annotation(MethodNode handler, String suffix) {
        var redirect = handler.visibleAnnotations.stream().filter(a -> a.desc.endsWith(suffix))
                .findFirst().orElseThrow();
        return redirect;
    }

    private static Object value(AnnotationNode annotation, String key) {
        return annotation.values.get(annotation.values.indexOf(key) + 1);
    }

    private static String signature(MethodInsnNode call) {
        return "L" + call.owner + ";" + call.name + call.desc;
    }

    private static List<MethodInsnNode> calls(MethodNode method) {
        return java.util.Arrays.stream(method.instructions.toArray()).filter(MethodInsnNode.class::isInstance)
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
