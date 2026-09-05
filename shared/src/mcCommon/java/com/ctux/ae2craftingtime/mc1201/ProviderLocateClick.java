package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderLocateC2S;

/**
 * Client-side only. Sends a locate request for a double-clicked crafting row
 * in the crafting CPU screen, including normal TTC rows and delayed rows.
 * The server resolves the player's open CPU scope and answers with a rainbow
 * edge only (never a plate) or the same expiry notice as a stale chat link,
 * so manual locates never create or extend red and craft-state changes never
 * clear rainbow. Never touched on a dedicated server.
 */
public final class ProviderLocateClick {
    /**
     * Whether the output id can trigger a locate request. Any resolvable
     * active crafting item qualifies; the server validates resolvability and
     * job ownership, expiring unresolvable rows with no highlight.
     */
    public static boolean shouldLocate(String outputId) {
        return outputId != null && !outputId.isBlank();
    }

    /**
     * Sends the request for any crafting item row. Returns whether a request
     * was sent, so the screen mixin knows whether to consume the click and
     * close the originating screen while leaving red unchanged.
     */
    public static boolean requestLocate(String outputId) {
        if (!shouldLocate(outputId)) {
            return false;
        }
        StatsNetwork.sendToServer(new ProviderLocateC2S(outputId));
        return true;
    }

    private ProviderLocateClick() {
    }
}
