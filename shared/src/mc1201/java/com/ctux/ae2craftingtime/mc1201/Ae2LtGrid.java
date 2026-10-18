package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import java.lang.reflect.Field;

/** The optional time-wheel logic exposes its CPU only through this stable field. */
public final class Ae2LtGrid {
    private static Field cpu;

    public static IGrid get(Object logic) {
        try {
            if (cpu == null) {
                cpu = logic.getClass().getDeclaredField("cpu");
                cpu.setAccessible(true);
            }
            var owner = cpu.get(logic);
            return owner == null ? null : (IGrid) owner.getClass().getMethod("getGrid").invoke(owner);
        } catch (ReflectiveOperationException | ClassCastException | SecurityException error) {
            return null;
        }
    }

    private Ae2LtGrid() { }
}
