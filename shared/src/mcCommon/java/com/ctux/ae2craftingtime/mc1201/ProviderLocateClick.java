package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.net.ProviderLocateC2S;

/**
 * Client-side only. Sends a locate request for a double-clicked delayed row
 * in the crafting CPU screen. The server resolves the player's open CPU
 * scope and answers with the same highlight (or expiry notice) as the chat
 * link. Never touched on a dedicated server.
 */
public final class ProviderLocateClick {
    /**
     * Sends the request when the output currently reports a stall.
     * Returns whether a request was sent, so the screen mixin knows whether
     * to consume the click.
     */
    public static boolean requestLocate(String outputId) {
        if (outputId == null || outputId.isBlank()) {
            return false;
        }
        if (ClientStats.CACHE.stall(new ProfileKey(outputId)).isEmpty()) {
            return false;
        }
        StatsNetwork.sendToServer(new ProviderLocateC2S(outputId));
        return true;
    }

    private ProviderLocateClick() {
    }
}
