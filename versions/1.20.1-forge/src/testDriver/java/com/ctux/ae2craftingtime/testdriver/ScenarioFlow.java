package com.ctux.ae2craftingtime.testdriver;

public final class ScenarioFlow {
    public static boolean allows(ScenarioState current, ScenarioState next) {
        if (next == ScenarioState.FAILED) {
            return current != ScenarioState.FAILED && current != ScenarioState.QUIT_REQUESTED;
        }
        return switch (current) {
            case STARTING -> next == ScenarioState.WORLD_READY;
            case WORLD_READY -> next == ScenarioState.TERMINAL_OPEN;
            case TERMINAL_OPEN -> next == ScenarioState.PLAN_OPEN;
            case PLAN_OPEN -> next == ScenarioState.PLAN_STABLE;
            case PLAN_STABLE -> next == ScenarioState.BASE_CHECKED;
            case BASE_CHECKED -> next == ScenarioState.SORTS_CHECKED;
            case SORTS_CHECKED -> next == ScenarioState.TOOLTIP_CHECKED;
            case TOOLTIP_CHECKED -> next == ScenarioState.RESULT_WRITTEN;
            case RESULT_WRITTEN -> next == ScenarioState.QUIT_REQUESTED;
            case QUIT_REQUESTED, FAILED -> false;
        };
    }

    private ScenarioFlow() {
    }
}
