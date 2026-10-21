package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CraftingJobRecoveryTest {
    @Test void quickOffOnChangesTrinityJobStampWithoutBackendTick() {
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        var server = new Object();
        lifecycle.begin(server);
        lifecycle.ready(server);
        var oldJob = lifecycle.stamp(OptionFeature.DATA_ENERGISTICS_TRINITY);
        var unaffected = lifecycle.stamp(OptionFeature.ADVANCED_AE);
        lifecycle.disable(OptionFeature.DATA_ENERGISTICS_TRINITY);
        assertNotEquals(oldJob, lifecycle.stamp(OptionFeature.DATA_ENERGISTICS_TRINITY));
        assertEquals(unaffected, lifecycle.stamp(OptionFeature.ADVANCED_AE));
        var freshJob = lifecycle.stamp(OptionFeature.DATA_ENERGISTICS_TRINITY);
        assertEquals(freshJob, lifecycle.stamp(OptionFeature.DATA_ENERGISTICS_TRINITY));
        lifecycle.disable(OptionFeature.PROFILING);
        assertNotEquals(freshJob, lifecycle.stamp(OptionFeature.DATA_ENERGISTICS_TRINITY));
    }
    private static final UUID JOB = UUID.fromString("a49302d3-6630-493a-932e-41cf90a594cf");
    private static final ProfileKey ROOT = new ProfileKey("network", "minecraft:stone");

    private static byte[] saved() {
        return CraftingJobEstimateCodec.encode(JOB,
                new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L), Map.of())).orElseThrow();
    }

    @Test
    void attachesExactlyOnceAfterStartupAndPreservesPendingSave() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), null);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        assertArrayEquals(saved(), recovery.savePending(server, JOB, ROOT.outputId(), ROOT.networkId(), true)
                .orElseThrow());
        lifecycle.ready(server);
        assertEquals(5L, recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true)
                .orElseThrow().snapshot().orElseThrow().remainingAmounts().get(ROOT));
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        assertTrue(recovery.savePending(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
    }

    @Test
    void acceptedOutputBeforeReadinessDiscardsOldAmounts() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), null);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        recovery.missedOutput();
        lifecycle.ready(server);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
    }

    @Test
    void identityNetworkOutputAndServerMustMatch() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        lifecycle.ready(server);
        var recovery = new CraftingJobRecovery(lifecycle);
        for (int mismatch = 0; mismatch < 4; mismatch++) {
            recovery.read(saved(), null);
            var result = switch (mismatch) {
                case 0 -> recovery.attach(server, UUID.randomUUID(), ROOT.outputId(), ROOT.networkId(), true);
                case 1 -> recovery.attach(server, JOB, "minecraft:glass", ROOT.networkId(), true);
                case 2 -> recovery.attach(server, JOB, ROOT.outputId(), "other", true);
                default -> recovery.attach(new Object(), JOB, ROOT.outputId(), ROOT.networkId(), true);
            };
            assertTrue(result.isEmpty());
            assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        }
    }

    @Test
    void pendingSaveRejectsKnownIdentityMismatchAndBadLifecycle() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), OptionFeature.ADVANCED_AE);
        assertTrue(recovery.savePending(server, JOB, "minecraft:glass", ROOT.networkId(), true).isEmpty());
        recovery.read(saved(), OptionFeature.ADVANCED_AE);
        assertTrue(recovery.savePending(server, JOB, ROOT.outputId(), "other", true).isEmpty());
        recovery.read(saved(), OptionFeature.ADVANCED_AE);
        lifecycle.disable(OptionFeature.ADVANCED_AE);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        recovery.read(saved(), OptionFeature.ADVANCED_AE);
        lifecycle.reset();
        assertFalse(recovery.preservePendingTag(server, true));
        recovery.read(saved(), null);
        lifecycle.stop(server);
        assertTrue(recovery.savePending(server, JOB, null, null, true).isEmpty());
    }

    @Test
    void delayedRestorePreservesPendingTagThroughPreStartedSave() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        assertSame(server, lifecycle.server());
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), OptionFeature.AE2_LIGHTNING_TECH);
        assertTrue(recovery.preservePendingTag(server, true));
        assertArrayEquals(saved(), recovery.savePending(server, JOB, null, null, true).orElseThrow());
        assertTrue(recovery.attach(server, JOB, null, null, true).isEmpty());
        lifecycle.ready(new Object());
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        lifecycle.ready(server);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isPresent());
        assertFalse(recovery.preservePendingTag(server, true));
    }

    @Test
    void repeatedLegacyMalformedAndValidReadsReplaceOnlyPendingGraph() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        lifecycle.ready(server);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), null);
        recovery.read(null, null);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        recovery.read(saved(), null);
        recovery.read(new byte[] {1, 2}, null);
        assertTrue(recovery.savePending(server, JOB, null, null, true).isEmpty());
        recovery.read(saved(), null);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isPresent());
    }

    @Test
    void replacementWorldAndGlobalDisableCannotRevivePendingGraph() {
        var first = new Object();
        var second = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(first);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), null);
        lifecycle.stop(second);
        assertTrue(recovery.preservePendingTag(first, true));
        lifecycle.begin(second);
        lifecycle.ready(second);
        assertTrue(recovery.attach(second, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        recovery.read(saved(), null);
        lifecycle.disable(OptionFeature.PROFILING);
        assertTrue(recovery.savePending(second, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        lifecycle.ready(second);
        assertTrue(recovery.attach(second, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        lifecycle.stop(second);
        assertNull(lifecycle.server());
    }

    @Test
    void disablingOneIntegrationLeavesOtherPendingScopesUnaffected() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        lifecycle.ready(server);
        var advanced = new CraftingJobRecovery(lifecycle);
        var neoEco = new CraftingJobRecovery(lifecycle);
        advanced.read(saved(), OptionFeature.ADVANCED_AE);
        neoEco.read(saved(), OptionFeature.NEO_ECO);
        lifecycle.disable(OptionFeature.ADVANCED_AE);
        assertTrue(advanced.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        assertTrue(neoEco.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isPresent());
        assertTrue(advanced.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
    }

    @Test
    void trinityLateAcceptedOutputAdvancesOnlyTheMatchingRestoredGraph() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), OptionFeature.DATA_ENERGISTICS_TRINITY);
        lifecycle.ready(server);
        var estimate = recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).orElseThrow();
        estimate.complete(ROOT, 2);
        assertEquals(3L, estimate.snapshot().orElseThrow().remainingAmounts().get(ROOT));
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        recovery.read(saved(), OptionFeature.DATA_ENERGISTICS_TRINITY);
        lifecycle.disable(OptionFeature.DATA_ENERGISTICS_TRINITY);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
    }

    @Test
    void readsWithoutALiveServerAndProfilerReloadNeverReviveRecovery() {
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.ready(null);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), null);
        assertFalse(recovery.preservePendingTag(null, true));
        var server = new Object();
        lifecycle.begin(server);
        lifecycle.ready(server);
        recovery.read(saved(), null);
        lifecycle.ready(server);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        recovery.read(saved(), null);
        lifecycle.disable(null);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        recovery.read(saved(), null);
        lifecycle.begin(new Object());
        assertFalse(recovery.preservePendingTag(server, true));
    }

    @Test
    void missingIdentityDefersUntilPresentAndSaveRejectsOtherJobOrDisabledScope() {
        var server = new Object();
        var lifecycle = new CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        lifecycle.ready(server);
        var recovery = new CraftingJobRecovery(lifecycle);
        recovery.read(saved(), null);
        assertTrue(recovery.attach(server, null, ROOT.outputId(), ROOT.networkId(), true).isEmpty());
        assertTrue(recovery.attach(server, JOB, null, ROOT.networkId(), true).isEmpty());
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), null, true).isEmpty());
        assertTrue(recovery.preservePendingTag(server, true));
        assertTrue(recovery.savePending(server, UUID.randomUUID(), null, null, true).isEmpty());
        recovery.read(saved(), null);
        assertTrue(recovery.savePending(server, null, null, null, true).isEmpty());
        recovery.read(saved(), null);
        assertFalse(recovery.preservePendingTag(server, false));
        recovery.read(saved(), null);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), false).isEmpty());
        recovery.read(saved(), null);
        assertTrue(recovery.attach(server, JOB, ROOT.outputId(), ROOT.networkId(), true).isPresent());
    }
}
