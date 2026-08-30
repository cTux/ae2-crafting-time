package com.ctux.ae2craftingtime.testdriver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EndpointPolicy {
    public static final int MAX_REQUEST_BYTES = 64 * 1024;
    public static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    public static final Set<String> TOOLS = Set.of(
            "minecraft_get_state", "minecraft_get_screen", "minecraft_get_ui_snapshot",
            "minecraft_take_screenshot", "minecraft_get_logs", "minecraft_quit");
    private final byte[] token;
    private final AtomicBoolean controller = new AtomicBoolean();

    public EndpointPolicy(String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("interactive token must be 256-bit lowercase hex");
        }
        this.token = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
    }

    public boolean authenticate(String remoteAddress, String authorization, long contentLength,
            boolean startsController) {
        if (!("127.0.0.1".equals(remoteAddress) || "0:0:0:0:0:0:0:1".equals(remoteAddress))
                || authorization == null
                || !MessageDigest.isEqual(token, authorization.getBytes(StandardCharsets.UTF_8))
                || contentLength > MAX_REQUEST_BYTES) {
            return false;
        }
        return !startsController || controller.compareAndSet(false, true);
    }

    public static String bounded(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
            throw new IllegalArgumentException("MCP response exceeds 1 MiB");
        }
        return value;
    }
}
