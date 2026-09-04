package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Once-per-episode warnings for stuck outputs that are not stalls: each key
 * is reported once while continuously stuck, and progress (real or
 * administrative) that clears the stuck set re-arms a later transition.
 * Scopes are compared by identity because crafting CPUs are live objects.
 */
public final class StuckEpisodeTracker {
    private final Map<Object, Set<ProfileKey>> notified = new IdentityHashMap<>();
    private final Map<Object, Set<ProfileKey>> resolved = new IdentityHashMap<>();

    /**
     * Returns keys that newly count as stuck since the last poll for this
     * scope. Keys absent from {@code current} leave the episode and become
     * eligible again on a later transition; they are stashed for
     * {@link #pollResolved} so the server can explicitly clear their
     * highlight while the craft runs.
     */
    public List<ProfileKey> pollNewlyStuck(Object scope, Set<ProfileKey> current) {
        if (scope == null || current == null) {
            return List.of();
        }
        var known = notified.computeIfAbsent(scope, ignored -> new HashSet<>());
        var left = new HashSet<>(known);
        left.removeAll(current);
        if (!left.isEmpty()) {
            resolved.computeIfAbsent(scope, ignored -> new HashSet<>()).addAll(left);
        }
        known.retainAll(current);
        var newly = new ArrayList<ProfileKey>();
        for (var key : current) {
            if (key != null && known.add(key)) {
                newly.add(key);
            }
        }
        if (known.isEmpty()) {
            notified.remove(scope);
        }
        return List.copyOf(newly);
    }

    /**
     * Drains keys whose stuck episode ended since the last
     * {@link #pollNewlyStuck} poll. Each key is reported once; a later stuck
     * transition re-arms it. Empty when the scope is unknown or nothing
     * resolved.
     */
    public List<ProfileKey> pollResolved(Object scope) {
        if (scope == null) {
            return List.of();
        }
        var left = resolved.remove(scope);
        return left == null || left.isEmpty() ? List.of() : List.copyOf(left);
    }

    public void clear(Object scope) {
        if (scope != null) {
            notified.remove(scope);
            resolved.remove(scope);
        }
    }

    public void clearAll() {
        notified.clear();
        resolved.clear();
    }
}
