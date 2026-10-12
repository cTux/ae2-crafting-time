package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.*;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.TtcEstimate;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class TtcChatTransportTest {
    @Test
    void vanillaRoundTripPreservesRecipientLocalChoiceAndConfidence() {
        var options = ClientOptionsRuntime.current().features();
        boolean original = options.enabled(OptionFeature.INSTANT_STATUS);
        try {
            var raw = TtcComponents.semanticEstimate(new TtcEstimate(1.25, true));
            var received = Component.Serializer.fromJson(Component.Serializer.toJson(raw));
            assertNotNull(received);
            options.setEnabled(OptionFeature.INSTANT_STATUS, true);
            var enabled = TtcComponents.decorate(received, true);
            assertTrue(enabled.getString().contains("🥇"));
            assertEquals(0x55FF55, enabled.getStyle().getColor().getValue());
            options.setEnabled(OptionFeature.INSTANT_STATUS, false);
            assertEquals("5ms", TtcComponents.decorate(received, false).getString());
            options.setEnabled(OptionFeature.INSTANT_STATUS, true);
            raw = TtcComponents.semanticEstimate(new TtcEstimate(1.25, false));
            received = Component.Serializer.fromJson(Component.Serializer.toJson(raw));
            assertEquals("5ms?", TtcComponents.decorate(received, false).getString());
        } finally {
            options.setEnabled(OptionFeature.INSTANT_STATUS, original);
        }
    }
}
