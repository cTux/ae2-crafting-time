package com.ctux.ae2craftingtime.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class PresentationHookBoundaryTest {
    @Test
    void aelisRequiresTheScreenTooltipAndBothLayoutChanges() {
        var screen = new ClassNode();
        assertFalse(IntegrationMixinPlugin.aelisScreenReady(screen));
        var draw = add(screen, "drawFG", "()V");
        for (int i = 0; i < 3; i++) call(draw, "ae2craftingtime$drawTtc");
        assertFalse(IntegrationMixinPlugin.aelisScreenReady(screen));
        call(add(screen, "drawBG", "()V"), "ae2craftingtime$summaryHeight");
        var render = add(screen, "m_88315_", "()V");
        for (int i = 0; i < 3; i++) call(render, "ae2craftingtime$ttcTooltip");
        assertTrue(IntegrationMixinPlugin.aelisScreenReady(screen));
        call(render, "ae2craftingtime$ttcTooltip");
        assertFalse(IntegrationMixinPlugin.aelisScreenReady(screen));
        var layout = new ClassNode();
        assertFalse(IntegrationMixinPlugin.aelisLayoutReady(layout));
        call(add(layout, "fit", "()V"), "ae2craftingtime$panelCap");
        assertFalse(IntegrationMixinPlugin.aelisLayoutReady(layout));
        call(add(layout, "columns", "()V"), "ae2craftingtime$columnEnd");
        assertTrue(IntegrationMixinPlugin.aelisLayoutReady(layout));
    }
    @Test
    void rendererNeedsEveryTransformedCallSiteInBothAdapters() {
        var older = renderer(false);
        assertTrue(IntegrationMixinPlugin.tableReady(older));
        var newest = renderer(true);
        assertTrue(IntegrationMixinPlugin.tableReady(newest));
        var render = method(newest, "render");
        for (var instruction : render.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.endsWith("$beginTtcColors")) {
                render.instructions.remove(instruction);
                break;
            }
        }
        assertFalse(IntegrationMixinPlugin.tableReady(newest));
        newest = renderer(true);
        call(method(newest, "render"), "ae2craftingtime$textScale");
        assertFalse(IntegrationMixinPlugin.tableReady(newest));
        method(older, "render").instructions.remove(method(older, "render").instructions.getFirst());
        assertFalse(IntegrationMixinPlugin.tableReady(older));
        assertFalse(IntegrationMixinPlugin.tableReady(new ClassNode()));
    }

    @Test
    void screenAndCpuRequireTheirOwnMatchingCalls() {
        var screen = new ClassNode();
        var input = add(screen, "ae2craftingtime$clickedTableIndex", "(DD)I");
        call(input, "of");
        assertFalse(IntegrationMixinPlugin.inputReady(screen));
        call(input, "entryAt");
        assertFalse(IntegrationMixinPlugin.inputReady(screen));
        call(add(screen, "ae2craftingtime$clickedEntry", "()V"), "ae2craftingtime$clickedTableIndex");
        assertFalse(IntegrationMixinPlugin.inputReady(screen));
        call(add(screen, "ae2craftingtime$handleClickedStats", "()V"), "ae2craftingtime$clickedEntry");
        assertTrue(IntegrationMixinPlugin.inputReady(screen));
        assertFalse(IntegrationMixinPlugin.inputReady(new ClassNode()));

        var cpu = new ClassNode();
        call(add(cpu, "drawBackgroundLayer", "()V"), "ae2craftingtime$drawTtc");
        assertFalse(IntegrationMixinPlugin.cpuReady(cpu));
        call(add(cpu, "getTooltip", "(II)Lappeng/client/gui/Tooltip;"), "ae2craftingtime$fullTtcTooltip");
        assertTrue(IntegrationMixinPlugin.cpuReady(cpu));
        assertFalse(IntegrationMixinPlugin.cpuReady(new ClassNode()));
    }

    private static ClassNode renderer(boolean newest) {
        var node = new ClassNode();
        var render = add(node, "render", newest ? "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
                : "(Lnet/minecraft/client/gui/GuiGraphics;)V");
        for (int i = 0; i < 5; i++) {
            call(render, "ae2craftingtime$cellWidth");
            if (i < 4) call(render, "ae2craftingtime$cellHeight");
        }
        for (int i = 0; i < (newest ? 2 : 3); i++) call(render, "ae2craftingtime$textScale");
        for (int i = 0; i < 2; i++) call(render, "ae2craftingtime$lineHeight");
        call(render, "ae2craftingtime$beginTtcColors");
        for (var name : new String[] {"columnPitch", "rowPitch", "columnCount", "indexColumns",
                "inverseX", "inverseY", "visibleRows", "centeringHeight", "fullRowTooltip", "drawTextWithShadow"}) {
            call(render, "ae2craftingtime$" + name);
        }
        call(add(node, "getScrollableRows", "(I)I"), "ae2craftingtime$scrollRows");
        return node;
    }

    private static MethodNode add(ClassNode node, String name, String descriptor) {
        var method = new MethodNode(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
        node.methods.add(method);
        return method;
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    private static void call(MethodNode method, String name) {
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "example/Target", name, "()V", false));
    }
}
