package com.ctux.ae2craftingtime.mc1201.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import appeng.core.localization.GuiText;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.PlanAmountLines;
import com.ctux.ae2craftingtime.mc1201.RecurrentPlanEntry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

class CraftConfirmTableRendererMixinTest {
    @Test
    void wrapsOriginalOnceAndCopiesImmutableRowsWithoutChangingDisabledContent() throws ReflectiveOperationException {
        var support = ClientOptionsRuntime.class.getDeclaredMethod("setConnectionSupportForTests", BooleanSupplier.class);
        support.setAccessible(true);
        support.invoke(null, (BooleanSupplier) () -> false);
        try {
            var renderer = new CraftConfirmTableRendererMixin() { };
            var entry = new EmptyEntry();
            for (var methodName : List.of("ae2craftingtime$appendVisibleTimeToCraft",
                    "ae2craftingtime$appendTooltipTimeToCraft")) {
                var method = CraftConfirmTableRendererMixin.class.getDeclaredMethod(methodName,
                        CraftingPlanSummaryEntry.class, Operation.class);
                method.setAccessible(true);
                for (var input : List.<List<Component>>of(List.of(), List.of(Component.literal("first"),
                        Component.literal("addon")))) {
                    var expected = List.copyOf(input);
                    var calls = new AtomicInteger();
                    Operation<List<Component>> original = args -> {
                        calls.incrementAndGet();
                        assertEquals(entry, args[0]);
                        return input;
                    };
                    @SuppressWarnings("unchecked")
                    var result = (List<Component>) method.invoke(renderer, entry, original);
                    assertEquals(1, calls.get());
                    assertNotSame(input, result);
                    assertEquals(input, result);
                    result.add(Component.literal("new"));
                    assertEquals(input.size() + 1, result.size());
                    assertEquals(expected, input);
                }
            }
        } finally {
            support.invoke(null, new Object[] {null});
        }
    }

    private static final class EmptyEntry extends CraftingPlanSummaryEntry implements RecurrentPlanEntry {
        private EmptyEntry() { super(null, 0, 0, 0); }
        public boolean ae2craftingtime$recurrent() { return false; }
        public void ae2craftingtime$recurrent(boolean value) { }
        public boolean ae2craftingtime$storedVariant() { return false; }
        public void ae2craftingtime$storedVariant(boolean value) { }
    }

    @Test
    void compactsNativeAmountsAndPreservesMissingAndForeignLines() {
        var missing = GuiText.Missing.text("5");
        var foreign = Component.literal("foreign");
        var lines = new ArrayList<Component>(List.of(missing, GuiText.FromStorage.text("4"), foreign,
                GuiText.ToCraft.text("10")));
        var compact = compact(lines, 4, "4", 10, "10");
        assertNotNull(compact);
        assertEquals(List.of(missing, compact, foreign), lines);
        assertEquals("text.ae2craftingtime.status.amounts",
                ((TranslatableContents) compact.getContents()).getKey());
        assertEquals(List.of("4/10"), List.of(((TranslatableContents) compact.getContents()).getArgs()));
    }

    @Test
    void onlyOneNativeAmountAndEmptyRows() {
        var lines = new ArrayList<Component>(List.of(GuiText.ToCraft.text("1.5 mB")));
        var compact = compact(lines, 0, null, 1, "1.5 mB");
        assertEquals(List.of(compact), lines);
        assertEquals(List.of("C1.5 mB"), List.of(((TranslatableContents) compact.getContents()).getArgs()));
        lines.clear();
        assertNull(compact(lines, 0, null, 0, null));
        assertEquals(List.of(), lines);
    }

    @Test
    void uncertainNativeAmountsKeepOriginalDescription() {
        var stored = GuiText.FromStorage.text("4");
        for (var lines : List.of(
                new ArrayList<Component>(List.of(Component.literal("foreign"))),
                new ArrayList<Component>(List.of(stored, stored.copy())),
                new ArrayList<Component>(List.of(GuiText.FromStorage.text("5"))),
                new ArrayList<Component>(List.of(stored.copy().append(" extra"))))) {
            var before = List.copyOf(lines);
            assertNull(compact(lines, 4, "4", 0, null));
            assertEquals(before, lines);
        }
    }

    private static MutableComponent compact(List<Component> lines, long stored, String storedText,
            long craft, String craftText) {
        return PlanAmountLines.compact(lines, stored, storedText, craft, craftText);
    }
}
