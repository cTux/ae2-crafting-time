package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class UiObservationStoreTest {
    @Test
    void cpuTtcUsesActuallyDrawnTranslatedOrTruncatedTextWithinCard() {
        var card = new Rect(10, 20, 67, 22);
        var glyph = com.ctux.ae2craftingtime.core.TtcSymbols.Symbol.TIME.glyph();
        var preview = new UiSnapshot.ObservedText("literal", glyph + " ~12...", List.of(),
                new Rect(38, 22, 35, 9), 0xE0E0E0, false);
        var outside = new UiSnapshot.ObservedText("literal", glyph + " ~99...", List.of(),
                new Rect(80, 22, 35, 9), 0xE0E0E0, false);
        var unrelated = new UiSnapshot.ObservedText("literal", "CPU #1", List.of(),
                new Rect(12, 22, 30, 9), null, false);
        var observed = UiObservationStore.cpuTtcText(List.of(outside, unrelated, preview), card);
        assertEquals("text.ae2craftingtime.ttc", observed.key());
        assertEquals(preview.rendered(), observed.rendered());
        assertEquals(preview.bounds(), observed.bounds());

        var translated = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", glyph + " ~7s?", List.of(),
                new Rect(38, 22, 35, 9), null, false);
        assertEquals(translated, UiObservationStore.cpuTtcText(List.of(translated), card));
        var translatedOutside = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", glyph + " ~99s?", List.of(),
                new Rect(80, 22, 35, 9), null, false);
        assertNull(UiObservationStore.cpuTtcText(List.of(translatedOutside), card));
        assertNull(UiObservationStore.cpuTtcText(List.of(outside, unrelated), card));
    }
}
