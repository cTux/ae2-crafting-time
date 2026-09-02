package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

class TerminalTooltipInjectionTest {
    @Test
    void terminalHookRunsAfterAllAe2TooltipAdditionsAndBeforeEitherRenderPath() throws IOException {
        var mixin = readClass("com/ctux/ae2craftingtime/mc1201/mixin/WirelessTerminalScreenMixin");
        var handler = mixin.methods.stream().filter(method -> method.name.equals("ae2craftingtime$appendTtc"))
                .findFirst().orElseThrow();
        var injection = handler.visibleAnnotations.stream()
                .filter(annotation -> annotation.desc.endsWith("/ModifyVariable;"))
                .findFirst().orElseThrow();
        var at = (AnnotationNode) value(injection, "at");
        assertEquals("INVOKE", value(at, "value"));
        assertEquals(0, value(injection, "ordinal"));

        var screen = readClass("appeng/client/gui/me/common/MEStorageScreen");
        var tooltip = screen.methods.stream().filter(method -> method.name.equals("renderGridInventoryEntryTooltip"))
                .findFirst().orElseThrow();
        var calls = Arrays.stream(tooltip.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast).toList();
        var anchors = calls.stream().filter(call ->
                ("L" + call.owner + ";" + call.name + call.desc).equals(value(at, "target"))).toList();
        var anchor = anchors.get((Integer) value(at, "ordinal"));
        var additions = calls.stream().filter(call -> call.owner.equals("java/util/List") && call.name.equals("add"))
                .toList();
        assertEquals(4, additions.size(), "Stored, requestable, craftable, and advanced serial lines");
        assertTrue(additions.stream().allMatch(call -> calls.indexOf(call) < calls.indexOf(anchor)));
        var renders = calls.stream().filter(call -> call.owner.startsWith("net/minecraft/client/gui/GuiGraphics"))
                .toList();
        assertEquals(2, renders.size(), "Item and non-item tooltips");
        assertTrue(renders.stream().allMatch(call -> calls.indexOf(call) > calls.indexOf(anchor)));
    }

    private static Object value(AnnotationNode annotation, String key) {
        return annotation.values.get(annotation.values.indexOf(key) + 1);
    }

    private static ClassNode readClass(String name) throws IOException {
        try (var input = TerminalTooltipInjectionTest.class.getResourceAsStream("/" + name + ".class")) {
            var node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
}
