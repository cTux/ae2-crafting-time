package com.ctux.ae2cpd.core;

public record ProfileKey(String outputId) {
    public ProfileKey {
        if (outputId == null || outputId.isBlank()) {
            throw new IllegalArgumentException("outputId must not be blank");
        }
    }
}
