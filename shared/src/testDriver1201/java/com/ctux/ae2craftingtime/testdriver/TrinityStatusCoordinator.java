package com.ctux.ae2craftingtime.testdriver;

import java.util.List;

/** Matches the native coordinator's name to its AE2 menu serial. */
final class TrinityStatusCoordinator {
    static UiSnapshot.CpuCard card(List<UiSnapshot.CpuCard> cards, String coordinatorName) {
        if (coordinatorName == null || coordinatorName.isBlank()) return null;
        var matching = cards.stream().filter(card -> coordinatorName.equals(card.name())
                && card.jobId() == null && card.amount() == 0).toList();
        if (matching.size() > 1)
            throw new IllegalStateException("native Trinity coordinator has duplicate Status cards");
        return matching.isEmpty() ? null : matching.get(0);
    }

    private TrinityStatusCoordinator() {}
}
