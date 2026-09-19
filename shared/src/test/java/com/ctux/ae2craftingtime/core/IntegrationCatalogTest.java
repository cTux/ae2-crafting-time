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
                "crazyae2addons-2.6.2",
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
    void crazyCpuListContractIsClientOnlyForgeAndRejectsChangedHandlers() {
        var owner = "net/oktawia/crazyae2addons/mixins/MixinCPUSelectionList";
        var exact = Map.of(owner, new IntegrationContract.ClassInfo("java/lang/Object", Map.of(
                "method:sortThenSlice", List.of("(Ljava/util/List;II)Ljava/util/List;"),
                "method:hitTestOnSorted", List.of("(Lappeng/client/Point;"
                        + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V"))));
        var selected = selector("1.20.1-forge", true, exact);
        assertTrue(selected.shouldApply("CrazyAe2CpuListRenderMixin"));
        assertEquals("cpu-list-2.6.2", selected.snapshot().get("crazyae2addons").variant());

        var changed = Map.of(owner, new IntegrationContract.ClassInfo("java/lang/Object", Map.of(
                "method:sortThenSlice", List.of("(Ljava/util/List;II)Ljava/util/List;"),
                "method:hitTestOnSorted", List.of("(Lappeng/client/Point;)V"))));
        var rejected = selector("1.20.1-forge", true, changed);
        assertFalse(rejected.shouldApply("CrazyAe2CpuListRenderMixin"));
        assertEquals("no_compatible_variant", rejected.snapshot().get("crazyae2addons").reason());
        assertEquals(List.of("cpu-list-2.6.2:missing:" + owner + "#method:hitTestOnSorted"),
                rejected.snapshot().get("crazyae2addons").rejected());

        var wrongTarget = selector("1.20.1-fabric", true, exact);
        assertFalse(wrongTarget.shouldApply("CrazyAe2CpuListRenderMixin"));
        assertEquals("unsupported_target", wrongTarget.snapshot().get("crazyae2addons").reason());
        var server = selector("1.20.1-forge", false, exact);
        assertFalse(server.shouldApply("CrazyAe2CpuListRenderMixin"));
        assertEquals("wrong_side", server.snapshot().get("crazyae2addons").reason());
        var absent = new IntegrationSelection(IntegrationCatalog.CANDIDATES, "1.20.1-forge", true,
                id -> null, c -> IntegrationContract.check(c.contract(), exact::get), d -> {});
        assertFalse(absent.shouldApply("CrazyAe2CpuListRenderMixin"));
        assertEquals("absent", absent.snapshot().get("crazyae2addons").reason());
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
            case "cpu-list-2.6.2" -> Set.of("CrazyAe2CpuListRenderMixin");
            case "pending-accounting" -> Set.of("ECOCraftingCpuLogicMixin", "NeoEcoPendingDispatchMixin");
            case "batched-long" -> Set.of("ECOCraftingCpuLogicMixin", "NeoEcoLongBatchDispatchMixin");
            case "batched-int" -> Set.of("ECOCraftingCpuLogicMixin", "NeoEcoIntBatchDispatchMixin");
            default -> throw new AssertionError(variant);
        };
    }
}
