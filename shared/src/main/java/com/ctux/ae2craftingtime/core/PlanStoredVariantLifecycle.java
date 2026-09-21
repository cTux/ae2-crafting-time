package com.ctux.ae2craftingtime.core;

import java.util.Set;

/** Pure menu/plan identity and notification lifecycle. All callbacks only mark dirty. */
public final class PlanStoredVariantLifecycle {
    public enum Action { NONE, INSTALL, REFRESH, CLEAR, CLOSE }
    private Object plan;
    private Object grid;
    private Set<?> items = Set.of();
    private boolean dirty;
    private boolean suspended;

    public Action broadcast(Object currentPlan, Object currentGrid, boolean valid, Set<?> missingItems) {
        if (!valid || currentPlan == null) {
            close();
            return Action.CLOSE;
        }
        if (currentPlan != plan) {
            close();
            plan = currentPlan;
            grid = currentGrid;
            items = Set.copyOf(missingItems);
            suspended = currentGrid == null;
            return Action.INSTALL;
        }
        if (suspended) return Action.NONE;
        if (currentGrid != grid) {
            suspend();
            return Action.CLEAR;
        }
        if (!dirty) return Action.NONE;
        dirty = false;
        return Action.REFRESH;
    }

    public void changed(Object primaryItem) {
        if (!suspended && items.contains(primaryItem)) dirty = true;
    }

    public boolean observes() { return !suspended && !items.isEmpty(); }

    public void suspend() { suspended = true; dirty = false; }

    public void close() {
        plan = null;
        grid = null;
        items = Set.of();
        dirty = false;
        suspended = false;
    }

    public static boolean show(boolean item, long missing, boolean flag, boolean enabled) {
        return item && missing > 0 && flag && enabled;
    }
}
