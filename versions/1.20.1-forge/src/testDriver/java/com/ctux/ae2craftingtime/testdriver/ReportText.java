package com.ctux.ae2craftingtime.testdriver;

public final class ReportText {
    public static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[A-Za-z]:\\\\[^ ]+|/[^ ]+", "<path>").replaceAll("(?i)bearer +\\S+", "<secret>");
    }

    public static String failure(Throwable error) {
        var text = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (!text.isEmpty()) {
                text.append(" <- ");
            }
            text.append(current.getClass().getSimpleName()).append(": ").append(safe(current.getMessage()));
            if (current.getCause() == current) {
                break;
            }
        }
        return text.toString();
    }

    private ReportText() {
    }
}
