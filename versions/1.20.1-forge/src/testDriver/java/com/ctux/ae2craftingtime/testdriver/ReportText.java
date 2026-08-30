package com.ctux.ae2craftingtime.testdriver;

public final class ReportText {
    public static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[A-Za-z]:\\\\[^ ]+|/[^ ]+", "<path>").replaceAll("(?i)bearer +\\S+", "<secret>");
    }

    private ReportText() {
    }
}
