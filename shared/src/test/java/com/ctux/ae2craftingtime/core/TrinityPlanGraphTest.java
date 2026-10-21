package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrinityPlanGraphTest {
    private static final ProfileKey GLASS = new ProfileKey("minecraft:glass");
    private static final ProfileKey SAND = new ProfileKey("minecraft:sand");
    private static final ProfileKey DUST = new ProfileKey("test:dust");

    @Test void scheduledAcyclicFiringsGiveTheExistingEstimatorRealEdges() {
        var firings = List.of(new CraftingJobEstimate.Pattern(1, List.of(GLASS), List.of(Set.of(SAND))),
                new CraftingJobEstimate.Pattern(1, List.of(SAND), List.of(Set.of(DUST))));
        var graph = TrinityPlanGraph.project(Set.of(GLASS, SAND, DUST), firings, false);
        assertTrue(graph.complete());
        assertEquals(Map.of(GLASS, Set.of(SAND), SAND, Set.of(DUST)), graph.edges());
        var estimate = new CraftingJobEstimate(GLASS, Map.of(GLASS, 1L, SAND, 1L, DUST, 1L), graph);
        assertEquals(new TtcEstimate(3, true), estimate.remainingEstimate((key, amount) ->
                java.util.Optional.of(new TtcEstimate(1, true))).orElseThrow());
    }

    @Test void cyclesOrMissingAndAmbiguousFiringsRemainConservativeButKeepRows() {
        var firing = new CraftingJobEstimate.Pattern(1, List.of(GLASS), List.of(Set.of(SAND)));
        var repeated = TrinityPlanGraph.project(Set.of(GLASS, SAND), List.of(firing), true);
        assertEquals(Set.of(SAND), repeated.edges().get(GLASS));
        assertFalse(repeated.complete());
        assertFalse(new CraftingJobEstimate(GLASS, Map.of(GLASS, 1L, SAND, 1L), repeated)
                .remainingEstimate((key, amount) -> java.util.Optional.of(new TtcEstimate(1, true)))
                .orElseThrow().reliable());
        assertFalse(TrinityPlanGraph.project(Set.of(GLASS), List.of(), false).complete());
        assertFalse(TrinityPlanGraph.project(Set.of(GLASS, SAND, DUST), List.of(
                new CraftingJobEstimate.Pattern(1, List.of(GLASS), List.of(Set.of(SAND, DUST)))), false)
                .complete());
        assertFalse(TrinityPlanGraph.project(Set.of(GLASS), List.of(
                new CraftingJobEstimate.Pattern(1, List.of(GLASS), List.of()),
                new CraftingJobEstimate.Pattern(1, List.of(GLASS), List.of())), false).complete());
    }
}
