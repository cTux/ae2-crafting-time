package com.ctux.ae2craftingtime.mc1201.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import appeng.core.localization.GuiText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class CraftConfirmTableRendererMixinTest {
    @Test
    void compactsNativeAmountsAndPreservesMissingAndForeignLines() throws ReflectiveOperationException {
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
    void onlyOneNativeAmountAndEmptyRows() throws ReflectiveOperationException {
        var lines = new ArrayList<Component>(List.of(GuiText.ToCraft.text("1.5 mB")));
        var compact = compact(lines, 0, null, 1, "1.5 mB");
        assertEquals(List.of(compact), lines);
        assertEquals(List.of("C1.5 mB"), List.of(((TranslatableContents) compact.getContents()).getArgs()));
        lines.clear();
        assertNull(compact(lines, 0, null, 0, null));
        assertEquals(List.of(), lines);
    }

    @Test
    void uncertainNativeAmountsKeepOriginalDescription() throws ReflectiveOperationException {
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
            long craft, String craftText) throws ReflectiveOperationException {
        var method = CraftConfirmTableRendererMixin.class.getDeclaredMethod("ae2craftingtime$compactAmounts",
                List.class, long.class, String.class, long.class, String.class);
        method.setAccessible(true);
        return (MutableComponent) method.invoke(null, lines, stored, storedText, craft, craftText);
    }
}
