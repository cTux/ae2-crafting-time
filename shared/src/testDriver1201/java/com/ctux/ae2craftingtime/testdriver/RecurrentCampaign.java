package com.ctux.ae2craftingtime.testdriver;

final class RecurrentCampaign {
    static boolean sameGrid(Object expected, Object actual) {
        return expected != null && expected == actual;
    }
    static String phase(boolean visited, boolean swapped, boolean replanned, boolean complete) {
        return complete ? "complete" : !visited ? "initial" : !swapped ? "grid" : !replanned ? "swap" : "reconnect";
    }

    static String turn(String action, String phase) {
        if (phase.equals("reconnect")) return "alpha";
        var expected = switch (phase) {
            case "initial" -> "initial";
            case "grid" -> "grid";
            case "swap" -> "swapped";
            case "complete" -> "captured";
            default -> throw new IllegalArgumentException("Unknown recurrent phase: " + phase);
        };
        return expected.equals(action) ? "" : "alpha";
    }

    static boolean allows(String action, String phase, boolean disconnected) {
        return switch (phase) {
            case "initial" -> action.equals("initial");
            case "grid" -> action.equals("grid");
            case "swap" -> action.equals("swapped");
            case "reconnect" -> disconnected && action.equals("rejoined");
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
