package com.ctux.ae2craftingtime.testdriver;

import java.util.List;
import java.util.Map;

final class RecurrentCampaign {
    static String phase(boolean swapped, boolean replanned, boolean complete) {
        return complete ? "complete" : !swapped ? "initial" : !replanned ? "swap" : "reconnect";
    }

    static String turn(List<String> roles, Map<String, String> actions, String phase) {
        if (phase.equals("reconnect")) return "alpha";
        var expected = switch (phase) {
            case "initial" -> "initial";
            case "swap" -> "swapped";
            case "complete" -> "captured";
            default -> throw new IllegalArgumentException("Unknown recurrent phase: " + phase);
        };
        return roles.stream().filter(role -> !expected.equals(actions.get(role))).findFirst().orElse("");
    }

    static boolean allows(String role, String action, String phase, String turn, boolean disconnected) {
        if (!role.equals(turn)) return false;
        return switch (phase) {
            case "initial" -> action.equals("initial");
            case "swap" -> action.equals("swapped");
            case "reconnect" -> disconnected && role.equals("alpha") && action.equals("rejoined");
            case "complete" -> action.equals("captured");
            default -> false;
        };
    }

    static boolean publish(boolean online, boolean complete, boolean captured) {
        if (!online && !(complete && captured)) throw new IllegalStateException("Role left before capture acknowledgement");
        return online;
    }

    private RecurrentCampaign() {}
}
