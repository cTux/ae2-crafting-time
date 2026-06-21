package com.ctux.ae2craftingtime.core;

public record ProfileKey(String networkId, String outputId) {
    public ProfileKey(String outputId) {
        this("", outputId);
    }

    public ProfileKey {
        networkId = networkId == null ? "" : networkId;
        if (outputId == null || outputId.isBlank()) {
            throw new IllegalArgumentException("outputId must not be blank");
        }
    }
}
