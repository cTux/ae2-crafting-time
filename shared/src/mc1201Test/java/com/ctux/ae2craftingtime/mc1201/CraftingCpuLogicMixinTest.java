package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CraftingCpuLogicMixinTest {
    @Test
    void expectedOutputHookTargetsVoidWaitingForInsert() throws IOException {
        var mixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingCpuLogicMixin.java"));

        assertTrue(mixin.contains(
                "ListCraftingInventory;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)V"));
        assertTrue(mixin.contains("method = \"finishJob\""));
        assertTrue(mixin.contains("ProfilerBridge.finishJob(cluster, success"));
        assertTrue(mixin.contains("method = \"trySubmitJob\""));
        assertTrue(mixin.contains("ProfilerBridge.startJob("));
    }
}
