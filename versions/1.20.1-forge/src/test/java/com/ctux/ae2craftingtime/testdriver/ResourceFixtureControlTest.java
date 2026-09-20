package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourceFixtureControlTest {
    @TempDir Path directory;

    @Test void readsWritesAndBindsTheCompleteProtocol() throws Exception {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var state = state(epoch, player, fixture, 1, 0, ResourceFixtureControl.Phase.READY,
                ResourceFixtureControl.Case.ITEM);
        ResourceFixtureControl.writeState(directory, state);
        assertEquals(state, ResourceFixtureControl.readState(directory));

        var command = command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0);
        writeCommand(command);
        assertEquals(command, ResourceFixtureControl.readCommand(directory));
        var accepted = ResourceFixtureControl.decide(state, command, null, false);
        assertFalse(accepted.replay());
        assertEquals(ResourceFixtureControl.Phase.HELD, accepted.nextPhase());

        var acknowledged = state(epoch, player, fixture, 1, 1, ResourceFixtureControl.Phase.HELD,
                ResourceFixtureControl.Case.ITEM);
        assertTrue(ResourceFixtureControl.decide(acknowledged, command, command, false).replay());
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.decide(acknowledged,
                command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.RELEASE,
                        ResourceFixtureControl.Case.ITEM, 0), command, false));
    }

    @Test void resourceScenariosExposeTheExactNonEmptyEvidenceContract() {
        assertEquals(DriverResult.RESOURCE_FIXTURE_CHECKS,
                DriverResult.requiredChecks("delayed-resource-icons"));
        assertEquals(DriverResult.RESOURCE_FIXTURE_CHECKS,
                DriverResult.requiredChecks("appmek-resource-icons"));
        assertFalse(DriverResult.RESOURCE_FIXTURE_CHECKS.isEmpty());
    }

    @Test void coversEveryTransitionAndRevisionBoundary() {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        assertTransition(state(epoch, player, fixture, 1, 1, ResourceFixtureControl.Phase.HELD,
                        ResourceFixtureControl.Case.FLUID_OVERLAP),
                command(epoch, player, fixture, 1, 2, ResourceFixtureControl.Action.RELEASE,
                        ResourceFixtureControl.Case.FLUID_OVERLAP, 1), ResourceFixtureControl.Phase.SETTLED, 1);
        assertTransition(state(epoch, player, fixture, 1, 2, ResourceFixtureControl.Phase.HELD,
                        ResourceFixtureControl.Case.ITEM),
                command(epoch, player, fixture, 1, 3, ResourceFixtureControl.Action.CANCEL,
                        ResourceFixtureControl.Case.ITEM, 0), ResourceFixtureControl.Phase.SETTLED, 1);
        assertTransition(state(epoch, player, fixture, 1, 3, ResourceFixtureControl.Phase.HELD,
                        ResourceFixtureControl.Case.ITEM),
                command(epoch, player, fixture, 1, 4, ResourceFixtureControl.Action.REJOIN_PREPARE,
                        ResourceFixtureControl.Case.ITEM, 0), ResourceFixtureControl.Phase.REJOINING, 1);
        assertTransition(state(epoch, player, fixture, 1, 4, ResourceFixtureControl.Phase.REJOINING,
                        ResourceFixtureControl.Case.ITEM),
                command(epoch, player, fixture, 1, 5, ResourceFixtureControl.Action.RECONNECT,
                        ResourceFixtureControl.Case.ITEM, 0), ResourceFixtureControl.Phase.HELD, 1);
        assertTransition(state(epoch, player, fixture, 1, 5, ResourceFixtureControl.Phase.SETTLED,
                        ResourceFixtureControl.Case.ITEM),
                command(epoch, player, fixture, 1, 6, ResourceFixtureControl.Action.RESET,
                        ResourceFixtureControl.Case.ITEM, 0), ResourceFixtureControl.Phase.CLEAN, 2);
        assertTransition(state(epoch, player, fixture, 2, 6, ResourceFixtureControl.Phase.CLEAN,
                        ResourceFixtureControl.Case.ITEM),
                command(epoch, player, fixture, 2, 7, ResourceFixtureControl.Action.COMPLETE,
                        ResourceFixtureControl.Case.ITEM, 0), ResourceFixtureControl.Phase.COMPLETE, 2);
        assertRejected(state(epoch, player, fixture, 1, 1, ResourceFixtureControl.Phase.HELD,
                        ResourceFixtureControl.Case.ITEM),
                command(epoch, player, fixture, 1, 2, ResourceFixtureControl.Action.RECONNECT,
                        ResourceFixtureControl.Case.ITEM, 0), IllegalStateException.class);
    }

    @Test void rejectsIdentitySequencePhaseSlotAndCaseViolations() {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var ready = state(epoch, player, fixture, 1, 0, ResourceFixtureControl.Phase.READY,
                ResourceFixtureControl.Case.ITEM);
        assertRejected(ready, command(UUID.randomUUID(), player, fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0), IllegalArgumentException.class);
        assertRejected(ready, command(epoch, UUID.randomUUID(), fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0), IllegalArgumentException.class);
        assertRejected(ready, command(epoch, player, UUID.randomUUID(), 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0), IllegalArgumentException.class);
        assertRejected(ready, command(epoch, player, fixture, 2, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0), IllegalArgumentException.class);
        assertRejected(ready, command(epoch, player, fixture, 1, 2, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0), IllegalArgumentException.class);
        assertRejected(ready, command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0), IllegalStateException.class, true);
        assertRejected(ready, command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.RELEASE,
                ResourceFixtureControl.Case.WATER, 0), IllegalArgumentException.class);
        assertRejected(ready, command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 1), IllegalArgumentException.class);
        assertRejected(ready, command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.RELEASE,
                ResourceFixtureControl.Case.ITEM, 0), IllegalStateException.class);
        var overflow = state(epoch, player, fixture, 1, Long.MAX_VALUE, ResourceFixtureControl.Phase.HELD,
                ResourceFixtureControl.Case.ITEM);
        assertRejected(overflow, command(epoch, player, fixture, 1, Long.MAX_VALUE,
                ResourceFixtureControl.Action.RELEASE, ResourceFixtureControl.Case.ITEM, 0), IllegalArgumentException.class);
        var settled = state(epoch, player, fixture, Long.MAX_VALUE, 1, ResourceFixtureControl.Phase.SETTLED,
                ResourceFixtureControl.Case.ITEM);
        assertRejected(settled, command(epoch, player, fixture, Long.MAX_VALUE, 2,
                ResourceFixtureControl.Action.RESET, ResourceFixtureControl.Case.ITEM, 0), IllegalArgumentException.class);
    }

    @Test void validatesTargetSpecificCases() {
        ResourceFixtureControl.validateCase("1.20.1-forge", "delayed-resource-icons", ResourceFixtureControl.Case.BUCKETLESS);
        ResourceFixtureControl.validateCase("1.20.1-fabric", "delayed-resource-icons", ResourceFixtureControl.Case.WATER);
        ResourceFixtureControl.validateCase("1.20.1-forge", "appmek-resource-icons", ResourceFixtureControl.Case.OXYGEN);
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.validateCase(
                "1.20.1-fabric", "appmek-resource-icons", ResourceFixtureControl.Case.OXYGEN));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.validateCase(
                "1.21.1-neoforge", "delayed-resource-icons", ResourceFixtureControl.Case.BUCKETLESS));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.validateCase(
                "1.20.1-forge", "delayed-resource-icons", ResourceFixtureControl.Case.HYDROGEN));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.validateCase(
                "1.20.1-forge", "appmek-resource-icons", ResourceFixtureControl.Case.ITEM));
    }

    @Test void retriesTheExactInFlightCommandAndRejectsEveryConflict() {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var ready = state(epoch, player, fixture, 1, 0, ResourceFixtureControl.Phase.READY,
                ResourceFixtureControl.Case.ITEM);
        var create = command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0);
        assertTrue(ResourceFixtureControl.decide(ready, create, null, create).replay());
        assertThrows(IllegalStateException.class, () -> ResourceFixtureControl.decide(ready,
                command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                        ResourceFixtureControl.Case.WATER, 0), null, create));
    }

    @Test void retainsTheAcceptedResetDecisionWhileTheOperationIsInFlight() {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var settled = state(epoch, player, fixture, 7, 8, ResourceFixtureControl.Phase.SETTLED,
                ResourceFixtureControl.Case.ITEM);
        var reset = command(epoch, player, fixture, 7, 9, ResourceFixtureControl.Action.RESET,
                ResourceFixtureControl.Case.ITEM, 0);
        var accepted = ResourceFixtureControl.decide(settled, reset, null, null, null);

        assertEquals(ResourceFixtureControl.Phase.CLEAN, accepted.nextPhase());
        assertEquals(8, accepted.nextRevision());
        assertEquals(accepted, ResourceFixtureControl.decide(settled, reset, null, reset, accepted));
        assertThrows(IllegalStateException.class, () -> ResourceFixtureControl.decide(settled,
                command(epoch, player, fixture, 7, 9, ResourceFixtureControl.Action.RESET,
                        ResourceFixtureControl.Case.WATER, 0), null, reset, accepted));
    }

    @Test void acknowledgementBindsRevisionActionCaseAndSlot() {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var command = command(epoch, player, fixture, 1, 1, ResourceFixtureControl.Action.CREATE,
                ResourceFixtureControl.Case.ITEM, 0);
        var acknowledged = state(epoch, player, fixture, 1, 1, ResourceFixtureControl.Phase.HELD,
                ResourceFixtureControl.Case.ITEM);
        ResourceFixtureControl.requireAcknowledgement(acknowledged, command);
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireAcknowledgement(
                new ResourceFixtureControl.State(epoch, "delayed-resource-icons", player, fixture, 1, 1, 2,
                        ResourceFixtureControl.Action.CREATE, ResourceFixtureControl.Case.ITEM, 0,
                        ResourceFixtureControl.Phase.HELD, "", "0,64,0", "[]", "[]"), command));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireAcknowledgement(
                state(epoch, player, fixture, 1, 1, ResourceFixtureControl.Phase.SETTLED,
                        ResourceFixtureControl.Case.ITEM), command));

        var reset = command(epoch, player, fixture, 1, 2, ResourceFixtureControl.Action.RESET,
                ResourceFixtureControl.Case.ITEM, 0);
        var resetAck = new ResourceFixtureControl.State(epoch, "delayed-resource-icons", player, fixture,
                2, 2, 1, ResourceFixtureControl.Action.RESET, ResourceFixtureControl.Case.ITEM, 0,
                ResourceFixtureControl.Phase.CLEAN, "", "0,64,0", "[]", "[]");
        ResourceFixtureControl.requireAcknowledgement(resetAck, reset);
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireAcknowledgement(
                new ResourceFixtureControl.State(epoch, "delayed-resource-icons", player, fixture,
                        1, 2, 1, ResourceFixtureControl.Action.RESET, ResourceFixtureControl.Case.ITEM, 0,
                        ResourceFixtureControl.Phase.CLEAN, "", "0,64,0", "[]", "[]"), reset));
    }

    @Test void initialStateRejectsStaleOrWrongLaunchIdentity() {
        var epoch = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var player = UUID.randomUUID();
        var initial = state(epoch, player, fixture, 1, 0, ResourceFixtureControl.Phase.READY,
                ResourceFixtureControl.Case.ITEM);
        ResourceFixtureControl.requireInitialIdentity(initial, epoch, fixture, player, "delayed-resource-icons");
        assertEquals(epoch, ResourceFixtureControl.parseUuid(epoch.toString().replace("-", "")));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireInitialIdentity(
                initial, UUID.randomUUID(), fixture, player, "delayed-resource-icons"));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireInitialIdentity(
                initial, epoch, UUID.randomUUID(), player, "delayed-resource-icons"));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireInitialIdentity(
                state(epoch, player, fixture, 2, 0, ResourceFixtureControl.Phase.READY,
                        ResourceFixtureControl.Case.ITEM), epoch, fixture, player, "delayed-resource-icons"));
    }

    @Test void publishesFixedIndependentCheckpointContracts() {
        assertEquals(List.of("held", "completed", "cancel-held", "cancelled"),
                ResourceFixtureControl.expectedCheckpoints(ResourceFixtureControl.Case.ITEM, false));
        assertEquals(List.of("held", "rejoined", "winner-promoted", "completed", "cancel-held", "cancelled"),
                ResourceFixtureControl.expectedCheckpoints(ResourceFixtureControl.Case.FLUID_OVERLAP, true));
        assertEquals(List.of("item-held.png", "item-completed.png", "item-cancel-held.png", "item-cancelled.png"),
                ResourceFixtureControl.expectedScreenshots(ResourceFixtureControl.Case.ITEM, false));
    }

    @Test void finalClientEvidenceBindsTheCleanFixtureBeforeCompletion() {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var clean = state(epoch, player, fixture, 4, 8, ResourceFixtureControl.Phase.CLEAN,
                ResourceFixtureControl.Case.ITEM);
        var evidence = new ResourceFixtureControl.ClientEvidence(epoch, "delayed-resource-icons", player, fixture,
                4, 5, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ResourceFixtureControl.writeClientEvidence(directory, evidence);
        assertEquals(evidence, ResourceFixtureControl.readClientEvidence(directory));
        ResourceFixtureControl.requireClientEvidence(clean, evidence, 5);
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireClientEvidence(clean,
                new ResourceFixtureControl.ClientEvidence(epoch, "delayed-resource-icons", player, fixture,
                        4, 4, evidence.digest()), 5));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireClientEvidence(
                state(epoch, player, fixture, 3, 8, ResourceFixtureControl.Phase.CLEAN,
                        ResourceFixtureControl.Case.ITEM), evidence, 5));
    }

    @Test void partialInsertionRetainsTheUninsertedRemainder() {
        var slot = new ResourceProcessingFixture.Slot(0, null, 10, "minecraft:cobblestone",
                "1,2,3", "4,5,6", true, 10, 0, false, 7);
        var partial = ResourceProcessingFixture.afterInsertion(slot, 4);
        assertEquals(6, partial.heldAmount());
        assertEquals(4, partial.releasedAmount());
        var finished = ResourceProcessingFixture.afterInsertion(partial, 6);
        assertEquals(0, finished.heldAmount());
        assertEquals(10, finished.releasedAmount());
        assertThrows(IllegalArgumentException.class, () -> ResourceProcessingFixture.afterInsertion(slot, 11));
    }

    @Test void multiSlotWarmupReturnPollsEveryPartialSlotUntilComplete() {
        var first = new ResourceProcessingFixture.Slot(0, null, 10, "first", "provider", "cpu-0",
                true, 10, 0, false, 1);
        var second = new ResourceProcessingFixture.Slot(1, null, 12, "second", "provider", "cpu-1",
                true, 12, 0, false, 1);
        var firstTick = List.of(ResourceProcessingFixture.afterInsertion(first, 4),
                ResourceProcessingFixture.afterInsertion(second, 3));
        assertFalse(ResourceProcessingFixture.allReleased(firstTick));
        var secondTick = List.of(ResourceProcessingFixture.afterInsertion(firstTick.get(0), 6),
                ResourceProcessingFixture.afterInsertion(firstTick.get(1), 9));
        assertTrue(ResourceProcessingFixture.allReleased(secondTick));
        assertEquals(10, secondTick.get(0).releasedAmount());
        assertEquals(12, secondTick.get(1).releasedAmount());
    }

    @Test void overlapNamesUseOneCanonicalWireEncoding() {
        assertEquals("fluid-overlap", ResourceFixtureControl.wireCase(ResourceFixtureControl.Case.FLUID_OVERLAP));
        assertEquals("chemical-overlap", ResourceFixtureControl.wireCase(ResourceFixtureControl.Case.CHEMICAL_OVERLAP));
        assertEquals("fluid-overlap-winner-promoted.png",
                ResourceFixtureControl.expectedScreenshots(ResourceFixtureControl.Case.FLUID_OVERLAP, false).get(1));
    }

    @Test void exceptionalCaptureCanBeQuarantinedBeforeAbortPolling() {
        var failed = new java.util.concurrent.CompletableFuture<Void>();
        failed.completeExceptionally(new IllegalStateException("screenshot failed"));
        assertDoesNotThrow(() -> ResourceFixtureClient.quarantineCaptureFuture(failed).join());
    }

    @Test void sidecarFailureArtifactsAreRemovedBeforeTermination(@org.junit.jupiter.api.io.TempDir Path directory)
            throws Exception {
        var image = directory.resolve("held.png");
        var sidecar = directory.resolve("held.json");
        Files.writeString(image, "partial image");
        Files.writeString(sidecar, "partial sidecar");
        ResourceFixtureClient.quarantineCaptureArtifacts(image);
        assertFalse(Files.exists(image));
        assertFalse(Files.exists(sidecar));
    }

    @Test void neverCompletingCaptureIsCancelledForAbortCleanup() {
        var pending = new java.util.concurrent.CompletableFuture<Void>();
        assertDoesNotThrow(() -> ResourceFixtureClient.quarantineCaptureFuture(pending).join());
        assertTrue(pending.isCancelled());
    }

    @Test void cleanupFailureKeepsTheOriginalFailureVisible() {
        var result = ResourceFixtureServer.cleanupEvidence("original", "cleanup");
        assertEquals("original", result.get("originalFailure"));
        assertEquals("FAIL", result.get("liveCleanup"));
        assertEquals("cleanup", result.get("cleanupFailure"));
    }

    @Test void connectedAbortBindsOriginalFailureAndTerminalRevision() {
        var epoch = UUID.randomUUID();
        var player = UUID.randomUUID();
        var fixture = UUID.randomUUID();
        var held = state(epoch, player, fixture, 3, 4, ResourceFixtureControl.Phase.HELD,
                ResourceFixtureControl.Case.ITEM);
        var abortCommand = command(epoch, player, fixture, 3, 5, ResourceFixtureControl.Action.ABORT,
                ResourceFixtureControl.Case.ITEM, 0);
        var abort = new ResourceFixtureControl.Abort(epoch, "delayed-resource-icons", player, fixture, 3,
                "original client failure");
        ResourceFixtureControl.writeAbort(directory, abort);
        assertEquals(abort, ResourceFixtureControl.readAbort(directory));
        ResourceFixtureControl.requireAbort(abortCommand, abort);
        var decision = ResourceFixtureControl.decide(held, abortCommand, null, false);
        assertEquals(ResourceFixtureControl.Phase.FAILED, decision.nextPhase());
        assertEquals(4, decision.nextRevision());
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.requireAbort(abortCommand,
                new ResourceFixtureControl.Abort(UUID.randomUUID(), "delayed-resource-icons", player, fixture, 3,
                        "original client failure")));
    }

    @Test void finalReceiptHistoryIsExactAndOrdered() {
        var receipts = List.<Map<String, Object>>of(
                receipt("CREATE"), receipt("REJOIN_PREPARE"), receipt("RECONNECT"),
                receipt("RELEASE"), receipt("RESET"), receipt("CREATE"), receipt("CANCEL"), receipt("RESET"));
        ResourceFixtureServer.requireReceiptHistory(receipts, List.of(ResourceFixtureControl.Case.ITEM), true);
        var missingReset = receipts.subList(0, receipts.size() - 1);
        assertThrows(IllegalStateException.class, () -> ResourceFixtureServer.requireReceiptHistory(
                missingReset, List.of(ResourceFixtureControl.Case.ITEM), true));
    }

    @Test void rejectsMalformedDuplicateUnknownOversizedAndLinkedFiles() throws Exception {
        var resource = Files.createDirectories(directory.resolve("resource"));
        var command = resource.resolve("command.properties");
        Files.writeString(command, "schema=1\nschema=1\n");
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.readCommand(directory));
        Files.writeString(command, "not-a-property\n");
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.readCommand(directory));
        Files.writeString(command, "x=".repeat(ResourceFixtureControl.MAX_BYTES));
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.readCommand(directory));
        Files.writeString(command, "schema=1\nunknown=true\n");
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.readCommand(directory));
        Files.delete(command);
        assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.readCommand(directory));
        try {
            var real = directory.resolve("real.properties");
            Files.writeString(real, "schema=1\n");
            Files.createSymbolicLink(command, real);
            assertThrows(IllegalArgumentException.class, () -> ResourceFixtureControl.readCommand(directory));
        } catch (java.io.IOException | UnsupportedOperationException | SecurityException ignored) {
            assertTrue(Files.notExists(command));
        }
    }

    private void writeCommand(ResourceFixtureControl.Command command) throws Exception {
        var text = "schema=1\nepoch=" + command.epoch() + "\nscenario=" + command.scenario() + "\nplayer="
                + command.player() + "\nfixture=" + command.fixture() + "\nrevision=" + command.revision() + "\nsequence="
                + command.sequence() + "\naction=" + command.action().name().toLowerCase().replace('_', '-') + "\ncase="
                + command.resourceCase().name().toLowerCase().replace('_', '-') + "\nslot=" + command.slot() + "\n";
        Files.writeString(Files.createDirectories(directory.resolve("resource")).resolve("command.properties"), text);
    }

    private static ResourceFixtureControl.Command command(UUID epoch, UUID player, UUID fixture, long revision,
            long sequence, ResourceFixtureControl.Action action, ResourceFixtureControl.Case resourceCase, int slot) {
        return new ResourceFixtureControl.Command(epoch, "delayed-resource-icons", player, fixture, revision, sequence,
                action, resourceCase, slot);
    }

    private static ResourceFixtureControl.State state(UUID epoch, UUID player, UUID fixture, long revision, long ack,
            ResourceFixtureControl.Phase phase, ResourceFixtureControl.Case resourceCase) {
        return new ResourceFixtureControl.State(epoch, "delayed-resource-icons", player, fixture, revision, ack,
                ack == 0 ? 0 : revision, ResourceFixtureControl.Action.CREATE, resourceCase, 0, phase, "", "0,64,0",
                "[]", "[]");
    }

    private static Map<String, Object> receipt(String action) {
        return Map.of("case", "ITEM", "action", action);
    }

    private static void assertTransition(ResourceFixtureControl.State state, ResourceFixtureControl.Command command,
            ResourceFixtureControl.Phase phase, long revision) {
        var decision = ResourceFixtureControl.decide(state, command, null, false);
        assertEquals(phase, decision.nextPhase());
        assertEquals(revision, decision.nextRevision());
    }

    private static void assertRejected(ResourceFixtureControl.State state, ResourceFixtureControl.Command command,
            Class<? extends Throwable> type) {
        assertRejected(state, command, type, false);
    }

    private static void assertRejected(ResourceFixtureControl.State state, ResourceFixtureControl.Command command,
            Class<? extends Throwable> type, boolean inFlight) {
        assertThrows(type, () -> ResourceFixtureControl.decide(state, command, null, inFlight));
    }
}
