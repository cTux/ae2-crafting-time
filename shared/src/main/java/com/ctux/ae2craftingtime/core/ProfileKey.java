package com.ctux.ae2craftingtime.core;

public record ProfileKey(String outputId) {
    public ProfileKey {
        if (outputId == null || outputId.isBlank()) {
            throw new IllegalArgumentException("outputId must not be blank");
        }
    }
}
