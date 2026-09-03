package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IntegrationCatalogTest {
    @Test
    void retainedReleasedContractsSelectExactlyOneBundle() throws Exception {
        for (var fixture : List.of("tree-helper-forge", "tree-helper-neo", "tree-layout-forge", "tree-layout-neo",
                "neoeco-20.3.0", "neoeco-20.4.0", "neoeco-20.4.2", "neoeco-21.1.1")) {
            try (var input = getClass().getResourceAsStream("/integration-contracts/" + fixture + ".tsv")) {
                var lines = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
                var identity = lines.get(0).substring(2).split("\t");
                var classes = new HashMap<String, IntegrationContract.ClassInfo>();
                var members = new HashMap<String, Map<String, List<String>>>();
                for (var line : lines) {
                    if (line.startsWith("#")) continue;
                    var row = line.split("\t");
                    if (row[1].equals("class")) members.put(row[0], new HashMap<>());
                    else members.get(row[0]).computeIfAbsent(row[1], key -> new ArrayList<>()).add(row[2]);
                }
                members.forEach((owner, data) -> classes.put(owner,
                        new IntegrationContract.ClassInfo("java/lang/Object", data)));
                var selection = selector(identity[0], true, classes);
                var accepted = IntegrationCatalog.CANDIDATES.stream()
                        .filter(c -> c.dependency().equals(identity[1])).flatMap(c -> c.mixins().stream())
                        .distinct().filter(selection::shouldApply).collect(java.util.stream.Collectors.toSet());
                assertEquals(identity[2], selection.snapshot().get(identity[1]).variant(), fixture);
                assertEquals(expectedBundle(identity[2]), accepted, fixture);
            }
        }
    }

    @Test
    void singletonBoundariesAndMissingFamiliesDoNotDisableCoreHooks() {
        for (var target : List.of("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")) {
            for (boolean client : List.of(true, false)) {
                var present = new HashMap<String, IntegrationContract.ClassInfo>();
                for (var candidate : IntegrationCatalog.CANDIDATES) {
                    if (candidate.contract().size() == 1 && candidate.contract().get(0).member().isEmpty()) {
                        present.put(candidate.contract().get(0).owner(), new IntegrationContract.ClassInfo(null, Map.of()));
                    }
                }
                var selection = selector(target, client, present);
                assertEquals(!target.equals("1.20.1-fabric"), selection.shouldApply("AdvancedCraftingCpuLogicMixin"));
                assertEquals(target.equals("1.20.1-forge") || target.equals("1.21.1-neoforge"),
                        selection.shouldApply("Ae2LtTimeWheelCraftingCpuLogicMixin"));
                assertEquals(client && !target.equals("26.1.2-neoforge"), selection.shouldApply("MERequesterScreenMixin"));
                assertFalse(selection.shouldApply("ECOCraftingCpuLogicMixin"));
                assertFalse(selection.shouldApply("CraftingTreeWidgetMixin"));
                assertFalse(selection.shouldApply("CraftingTreeNewWidgetMixin"));
                assertTrue(selection.shouldApply("CraftingCpuLogicMixin"));
            }
        }
    }

    private static IntegrationSelection selector(String target, boolean client,
            Map<String, IntegrationContract.ClassInfo> classes) {
        return new IntegrationSelection(IntegrationCatalog.CANDIDATES, target, client, id -> "fixture",
                c -> IntegrationContract.check(c.contract(), classes::get), d -> {});
    }

    private static Set<String> expectedBundle(String variant) {
        return switch (variant) {
            case "tree-helper" -> Set.of("CraftingTreeWidgetMixin");
            case "tree-layout" -> Set.of("CraftingTreeNewWidgetMixin");
            case "pending-accounting" -> Set.of("ECOCraftingCpuLogicMixin", "NeoEcoPendingDispatchMixin");
            case "batched-long" -> Set.of("ECOCraftingCpuLogicMixin", "NeoEcoLongBatchDispatchMixin");
            case "batched-int" -> Set.of("ECOCraftingCpuLogicMixin", "NeoEcoIntBatchDispatchMixin");
            default -> throw new AssertionError(variant);
        };
    }
}
