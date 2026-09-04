package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StuckEpisodeTrackerTest {
    private static ProfileKey key(String id) {
        return new ProfileKey(id);
    }

    @Test
    void notifiesOncePerStuckEpisode() {
        var tracker = new StuckEpisodeTracker();
        var scope = new Object();
        var iron = key("minecraft:iron_plate");

        assertTrue(tracker.pollNewlyStuck(scope, Set.of()).isEmpty());
        assertEquals(List.of(iron), tracker.pollNewlyStuck(scope, Set.of(iron)));
        assertTrue(tracker.pollNewlyStuck(scope, Set.of(iron)).isEmpty());
        assertTrue(tracker.pollNewlyStuck(scope, Set.of(iron)).isEmpty());
    }

    @Test
    void clearedKeysRearm() {
        var tracker = new StuckEpisodeTracker();
        var scope = new Object();
        var iron = key("minecraft:iron_plate");
        var copper = key("minecraft:copper_plate");

        assertEquals(2, tracker.pollNewlyStuck(scope, Set.of(iron, copper)).size());
        assertTrue(tracker.pollNewlyStuck(scope, Set.of(iron)).isEmpty());
        assertEquals(List.of(copper), tracker.pollNewlyStuck(scope, Set.of(iron, copper)));
    }

    @Test
    void resolvedKeysDrainOnceAndRearm() {
        var tracker = new StuckEpisodeTracker();
        var scope = new Object();
        var iron = key("minecraft:iron_plate");

        assertEquals(List.of(iron), tracker.pollNewlyStuck(scope, Set.of(iron)));
        assertTrue(tracker.pollResolved(scope).isEmpty());

        assertTrue(tracker.pollNewlyStuck(scope, Set.of()).isEmpty());
        assertEquals(List.of(iron), tracker.pollResolved(scope));
        assertTrue(tracker.pollResolved(scope).isEmpty());

        assertTrue(tracker.pollResolved(null).isEmpty());
        assertTrue(tracker.pollResolved(new Object()).isEmpty());

        // Leaving the stuck set re-arms a later transition.
        assertEquals(List.of(iron), tracker.pollNewlyStuck(scope, Set.of(iron)));
    }

    @Test
    void scopesAreIndependentAndNullSafe() {
        var tracker = new StuckEpisodeTracker();
        var first = new Object();
        var second = new Object();
        var iron = key("minecraft:iron_plate");

        assertEquals(List.of(iron), tracker.pollNewlyStuck(first, Set.of(iron)));
        assertEquals(List.of(iron), tracker.pollNewlyStuck(second, Set.of(iron)));

        tracker.clear(first);
        assertEquals(List.of(iron), tracker.pollNewlyStuck(first, Set.of(iron)));
        assertTrue(tracker.pollNewlyStuck(second, Set.of(iron)).isEmpty());

        tracker.clearAll();
        assertEquals(List.of(iron), tracker.pollNewlyStuck(second, Set.of(iron)));

        assertTrue(tracker.pollNewlyStuck(null, Set.of(iron)).isEmpty());
        assertTrue(tracker.pollNewlyStuck(first, null).isEmpty());
        tracker.clear(null);

        var withNull = new HashSet<ProfileKey>();
        withNull.add(null);
        withNull.add(iron);
        tracker.clearAll();
        assertEquals(List.of(iron), tracker.pollNewlyStuck(first, withNull));
    }
}
