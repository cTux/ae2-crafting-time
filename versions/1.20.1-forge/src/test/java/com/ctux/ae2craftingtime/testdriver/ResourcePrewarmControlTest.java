package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePrewarmControlTest {
    @TempDir Path root;
    private final String epoch = UUID.randomUUID().toString();
    private ResourcePrewarmControl gate() {
        return new ResourcePrewarmControl(root, epoch, "a".repeat(40), "b".repeat(64), System.currentTimeMillis() + 600_000);
    }
    private Map<String, String> ready(ResourcePrewarmControl gate, boolean server) {
        return gate.ready(12, Instant.ofEpochMilli(1000), 1, server ? 20 : 0, server ? 0 : 40);
    }
    private void publish(ResourcePrewarmControl gate) throws Exception {
        publish(gate, 1);
    }
    private void publish(ResourcePrewarmControl gate, int generation) throws Exception {
        gate.write("attempt.json", gate.ready(12, Instant.ofEpochMilli(1000), generation, 0, 0));
        gate.write("server-ready.json", gate.ready(12, Instant.ofEpochMilli(1000), generation, 20, 0));
        gate.write("client-ready.json", gate.ready(12, Instant.ofEpochMilli(1000), generation, 0, 40));
        var value = new LinkedHashMap<String, String>();
        value.put("schema", "1"); value.put("epoch", epoch); value.put("head", "a".repeat(40));
        value.put("bundle", "b".repeat(64)); value.put("player", ResourcePrewarmControl.PLAYER.toString()); value.put("generation", Integer.toString(generation));
        for (var side : new String[]{"server", "client"}) value.put(side + "ReadySha256",
                CaptureEvidence.sha256(Files.readAllBytes(root.resolve("prewarm/" + side + "-ready.json"))));
        gate.write("arm.json", value);
    }

    @Test void activatesOnceAndDoesNotRepublishAnIdenticalArm() throws Exception {
        var server = gate(); var client = gate();
        assertFalse(server.accept(1, true, true)); assertFalse(client.clientAccepted(1));
        publish(server); assertTrue(server.accept(1, true, true));
        var accepted = root.resolve("prewarm/armed.json"); var timestamp = Files.getLastModifiedTime(accepted);
        assertTrue(server.accept(1, true, false)); assertEquals(timestamp, Files.getLastModifiedTime(accepted));
        assertTrue(client.clientAccepted(1));
        assertThrows(IllegalStateException.class, () -> server.waiting(System.currentTimeMillis()));
        assertThrows(IllegalStateException.class, () -> client.waiting(System.currentTimeMillis()));
        server.write("arm.json", Map.of("changed", "arm"));
        assertThrows(IllegalStateException.class, () -> server.accept(1, true, false));
    }

    @Test void earlyArmMutationAndAbsoluteTimeoutFail() throws Exception {
        var gate = gate(); gate.waiting(System.currentTimeMillis());
        gate.write("arm.json", Map.of("schema", "1"));
        assertThrows(IllegalArgumentException.class, () -> gate.accept(1, true, true));
        Files.createDirectories(root.resolve("resource"));
        for (var file : new String[]{"command.properties", "state.properties"}) {
            var path = root.resolve("resource/" + file); Files.writeString(path, "fixture");
            assertThrows(IllegalStateException.class, () -> gate.waiting(System.currentTimeMillis())); Files.delete(path);
        }
        assertThrows(IllegalStateException.class, () -> gate.waiting(gate.deadlineMillis)); gate.waiting(gate.deadlineMillis - 1);
    }

    @Test void changedConnectionOrNonemptyFixtureCannotArm() throws Exception {
        var gate = gate(); publish(gate);
        assertThrows(IllegalStateException.class, () -> gate.accept(1, false, true));
        assertThrows(IllegalStateException.class, () -> gate.accept(1, true, false));
        assertThrows(IllegalArgumentException.class, () -> gate.accept(2, true, true));
    }

    @Test void bindsEveryReceiptFieldAndNativeThreshold() {
        var gate = gate();
        for (boolean server : new boolean[]{true, false}) {
            var receipt = ready(gate, server); gate.validateReady(receipt, server, 1);
            for (String key : receipt.keySet()) {
                var changed = new LinkedHashMap<>(receipt); changed.put(key, "invalid");
                assertThrows(RuntimeException.class, () -> gate.validateReady(changed, server, 1), key);
            }
            var extra = new LinkedHashMap<>(receipt); extra.put("extra", "field");
            assertThrows(IllegalArgumentException.class, () -> gate.validateReady(extra, server, 1));
        }
        for (int generation : new int[]{0,4}) assertThrows(IllegalArgumentException.class, () -> gate.ready(12, Instant.now(), generation, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> gate.ready(0, Instant.now(), 1, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> gate.ready(12, Instant.now(), 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> gate.ready(12, Instant.now(), 1, 0, -1));
    }

    @Test void changedReadyBytesCannotActivateOrValidateAcceptance() throws Exception {
        var gate = gate(); publish(gate); var ready = root.resolve("prewarm/client-ready.json");
        Files.writeString(ready, Files.readString(ready) + " ");
        assertThrows(IllegalArgumentException.class, () -> gate.accept(1, true, true));
        publish(gate); assertTrue(gate.accept(1, true, true));
        Files.writeString(ready, Files.readString(ready) + " ");
        assertThrows(IllegalArgumentException.class, () -> gate().clientAccepted(1));
    }

    @Test void malformedDuplicateOversizedAndNonregularFilesFailClosed() throws Exception {
        var gate = gate();
        for (String json : new String[]{"[]", "{", "{\"x\":null}", "{\"x\":true}", "{\"x\":{}}", "{\"x\":\"a\",\"x\":\"b\"}",
                "{} {}", "{\"" + "x".repeat(65) + "\":\"x\"}", "{\"x\":\"" + "x".repeat(129) + "\"}"})
            assertThrows(Exception.class, () -> ResourcePrewarmControl.parse(json.getBytes(StandardCharsets.UTF_8)), json);
        assertEquals(Map.of("number", "1"), ResourcePrewarmControl.parse("{\"number\":1}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> ResourcePrewarmControl.parse(new byte[65537]));
        assertThrows(IllegalArgumentException.class, () -> gate.write("unknown.json", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> gate.write("arm.json", Map.of("x", "x".repeat(65537))));
        Files.createDirectories(root.resolve("prewarm/arm.json"));
        assertThrows(IllegalArgumentException.class, () -> gate.read("arm.json"));
        Files.delete(root.resolve("prewarm/arm.json")); Files.write(root.resolve("prewarm/arm.json"), new byte[65537]);
        assertThrows(IllegalArgumentException.class, () -> gate.read("arm.json"));
    }

    @Test void rejectsInvalidLaunchIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new ResourcePrewarmControl(root, epoch, "bad", "b".repeat(64), 1));
        assertThrows(IllegalArgumentException.class, () -> new ResourcePrewarmControl(root, epoch, "a".repeat(40), "bad", 1));
        assertThrows(IllegalArgumentException.class, () -> new ResourcePrewarmControl(root, epoch, "a".repeat(40), "b".repeat(64), 0));
    }

    @Test void configuredIdentityAndSemanticBoundsAreChecked() {
        var properties = Map.of("prewarmEpoch", epoch, "prewarmHead", "a".repeat(40),
                "prewarmBundle", "b".repeat(64), "prewarmDeadline", "600000");
        var previous = new LinkedHashMap<String, String>();
        try {
            properties.forEach((key, value) -> previous.put(key, System.setProperty("ae2craftingtime.test." + key, value)));
            assertEquals(epoch, ResourcePrewarmControl.configured(root).epoch);
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty("ae2craftingtime.test." + key);
                else System.setProperty("ae2craftingtime.test." + key, value); });
        }
        var gate = gate();
        for (boolean server : new boolean[]{true, false}) {
            for (var entry : Map.of("pid", "0", "startTime", Instant.EPOCH.toString(), "serverTicks", "1", "worldFrames", "1").entrySet()) {
                var value = new LinkedHashMap<>(ready(gate, server)); value.put(entry.getKey(), entry.getValue());
                assertThrows(IllegalArgumentException.class, () -> gate.validateReady(value, server, 1));
            }
            for (int generation : new int[]{0, 4}) {
                var value = new LinkedHashMap<>(ready(gate, server)); value.put("generation", Integer.toString(generation));
                assertThrows(IllegalArgumentException.class, () -> gate.validateReady(value, server, generation));
            }
        }
    }

    @Test void serverDigestAndAcceptedArmFieldsAreBound() throws Exception {
        var gate = gate(); publish(gate);
        assertEquals(ready(gate, true), gate.read("server-ready.json"));
        var ready = root.resolve("prewarm/server-ready.json");
        Files.writeString(ready, Files.readString(ready) + " ");
        assertThrows(IllegalArgumentException.class, () -> gate.accept(1, true, true));
        publish(gate); gate.accept(1, true, true);
        Files.writeString(ready, Files.readString(ready) + " ");
        assertThrows(IllegalArgumentException.class, () -> gate().clientAccepted(1));
        Files.delete(root.resolve("prewarm/armed.json"));
        gate.write("armed.json", Map.of("extra", "field"));
        assertThrows(IllegalArgumentException.class, () -> gate().clientAccepted(1));
        gate.write("arm.json", Map.of("extra", "field"));
        assertThrows(IllegalArgumentException.class, () -> gate().clientAccepted(1));
    }

    @Test void redirectedReceiptDirectoriesAreRejected() throws Exception {
        var target = Files.createDirectory(root.resolve("actual"));
        var link = root.resolve("prewarm");
        if (System.getProperty("os.name").startsWith("Windows")) {
            var process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.waitFor(), output);
        } else Files.createSymbolicLink(link, target);
        try { assertThrows(IllegalArgumentException.class, () -> gate().write("arm.json", Map.of())); }
        finally { Files.delete(link); }
    }

    @Test void reconnectCannotConsumePreviousJoinReadiness() throws Exception {
        var gate = gate(); int generation = 1;
        var stale = gate.ready(12, Instant.ofEpochMilli(1000), generation, 20, 0);
        gate.write("server-ready.json", stale); gate.write("client-ready.json", ready(gate, false));
        assertTrue(gate.currentServerReady(stale, generation));
        gate.invalidateReadiness();
        assertFalse(gate.exists("server-ready.json")); assertFalse(gate.exists("client-ready.json"));
        gate.invalidateReadiness(); // Disconnect and subsequent join both invalidate before counting ticks.
        generation = 2;
        assertFalse(gate.currentServerReady(stale, generation)); // Even a previously read snapshot cannot be adopted.
        var current = gate.ready(12, Instant.ofEpochMilli(1000), generation, 20, 0);
        assertTrue(gate.currentServerReady(current, generation));
        assertThrows(IllegalArgumentException.class, () -> gate.currentServerReady(current, 1));
        gate.write("arm.json", Map.of());
        assertThrows(IllegalStateException.class, gate::invalidateReadiness);
        Files.delete(root.resolve("prewarm/arm.json")); gate.write("armed.json", Map.of());
        assertThrows(IllegalStateException.class, gate::invalidateReadiness);
    }

    @Test void acknowledgmentNeverReplacesPreexistingAcceptance() throws Exception {
        var gate = gate(); publish(gate);
        gate.write("armed.json", Map.of("foreign", "acceptance"));
        var path = root.resolve("prewarm/armed.json"); var original = Files.readAllBytes(path);
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> gate.accept(1, true, true));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> gate.write("armed.json", Map.of("foreign", "acceptance")));
        assertArrayEquals(original, Files.readAllBytes(path));
        // Retry-ready publications remain replaceable before activation.
        gate.write("server-ready.json", ready(gate, true)); gate.write("server-ready.json", ready(gate, true));
        gate.write("client-ready.json", ready(gate, false)); gate.write("client-ready.json", ready(gate, false));
        try (var paths = Files.list(path.getParent())) { assertFalse(paths.anyMatch(value -> value.toString().endsWith(".tmp"))); }
    }

    @Test void serverJoinWithoutAnyClientWorldFrameDoesNotDesynchronizeRetry() throws Exception {
        var server = gate(); var client = gate();
        int clientAttempt = ResourcePrewarmControl.nextConnectionAttempt(0, true, false, false);
        client.publishAttempt(clientAttempt, 12, Instant.ofEpochMilli(1000)); // Before native connect.
        int serverAttempt = server.attemptForJoin(0); // Server observes the first join.
        assertEquals(1, serverAttempt);
        server.write("server-ready.json", server.ready(34, Instant.ofEpochMilli(2000), serverAttempt, 20, 0));
        assertTrue(server.attemptStillCurrent(serverAttempt));
        assertFalse(client.exists("client-ready.json")); // Client never rendered a ready world.
        server.invalidateReadiness(); // Native disconnect, no fixture state and no activation.
        clientAttempt = ResourcePrewarmControl.nextConnectionAttempt(clientAttempt, true, false, false);
        client.publishAttempt(clientAttempt, 12, Instant.ofEpochMilli(1000));
        assertFalse(server.attemptStillCurrent(serverAttempt)); // A late callback on the old join cannot publish readiness.
        server.invalidateReadiness();
        serverAttempt = server.attemptForJoin(serverAttempt);
        assertEquals(clientAttempt, serverAttempt);
        assertTrue(server.attemptStillCurrent(serverAttempt));
        assertEquals(2, server.attemptForJoin(0)); // A pre-join handshake failure may also skip a server-observed join.
        assertThrows(IllegalStateException.class, () -> server.attemptForJoin(2));
        publish(server, serverAttempt); // Second native join reaches 20 server ticks and 40 client frames.
        assertTrue(server.accept(serverAttempt, true, true));
        assertTrue(client.clientAccepted(clientAttempt));
    }

    @Test void attemptIdentityIsBoundToTheClientAndCannotGoBackwards() throws Exception {
        var server = gate(); publish(server);
        assertThrows(IllegalStateException.class, () -> server.attemptStillCurrent(2));
        for (var changed : java.util.List.of(Map.of("pid", "99"), Map.of("startTime", Instant.ofEpochMilli(3000).toString()))) {
            var attempt = new LinkedHashMap<>(server.read("attempt.json")); attempt.putAll(changed);
            server.write("attempt.json", attempt);
            assertThrows(IllegalArgumentException.class, () -> server.accept(1, true, true));
            publish(server);
        }
        var malformed = new LinkedHashMap<>(server.read("attempt.json")); malformed.put("worldFrames", "40");
        server.write("attempt.json", malformed);
        assertThrows(IllegalArgumentException.class, () -> server.attemptForJoin(0));
    }

    @Test void threeNativeAttemptsCannotRetryAnArmedOrProgressedFixture() {
        for (int attempt = 0; attempt < 3; attempt++) assertEquals(attempt + 1,
                ResourcePrewarmControl.nextConnectionAttempt(attempt, true, false, false));
        for (int attempt : new int[]{-1, 3}) assertThrows(IllegalStateException.class,
                () -> ResourcePrewarmControl.nextConnectionAttempt(attempt, true, false, false));
        assertThrows(IllegalStateException.class, () -> ResourcePrewarmControl.nextConnectionAttempt(1, false, false, false));
        assertThrows(IllegalStateException.class, () -> ResourcePrewarmControl.nextConnectionAttempt(1, true, true, false));
        assertThrows(IllegalStateException.class, () -> ResourcePrewarmControl.nextConnectionAttempt(1, true, false, true));
    }

    @Test void readinessNeedsConsecutiveNativeCallbacksOnTheSameConnection() {
        for (int required : new int[]{20,40}) {
            for (int value = 0; value <= required; value++) assertEquals(Math.min(required, value + 1),
                    ResourcePrewarmControl.advanceReadiness(value, required, true, true));
            assertEquals(0, ResourcePrewarmControl.advanceReadiness(required, required, false, true));
            assertEquals(1, ResourcePrewarmControl.advanceReadiness(required, required, true, false));
            assertThrows(IllegalArgumentException.class, () -> ResourcePrewarmControl.advanceReadiness(-1, required, true, true));
            assertThrows(IllegalArgumentException.class, () -> ResourcePrewarmControl.advanceReadiness(required + 1, required, true, true));
        }
        assertThrows(IllegalArgumentException.class, () -> ResourcePrewarmControl.advanceReadiness(0, 30, true, true));
    }

    @Test void clientReadinessRunsOnceAtFrameCompletionNotAtScreenRender() throws Exception {
        var node = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/TestDriverRuntime.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        for (var methodName : new String[]{"tick", "afterRender"}) {
            var method = node.methods.stream().filter(candidate -> candidate.name.equals(methodName))
                    .findFirst().orElseThrow();
            var calls = java.util.Arrays.stream(method.instructions.toArray())
                    .filter(org.objectweb.asm.tree.MethodInsnNode.class::isInstance)
                    .map(org.objectweb.asm.tree.MethodInsnNode.class::cast)
                    .filter(call -> call.owner.endsWith("/ResourcePrewarmClient"))
                    .map(call -> call.name).filter(name -> !name.equals("checkpoint")).toList();
            assertEquals(methodName.equals("tick") ? java.util.List.of("afterRender", "tick") : java.util.List.of(),
                    calls, "screenless world frames must count once, before readiness receipt publication");
        }
    }

    @Test void screenlessWorldFramesReachFortyAndInterruptedFramesRestartTheWindow() {
        int frames = 0;
        // No ScreenEvent.Render.Post is emitted in-world; only completed-frame callbacks advance this window.
        for (int frame = 1; frame <= 40; frame++) {
            frames = ResourcePrewarmControl.advanceReadiness(frames, 40, true, frame != 1);
            assertEquals(frame, frames);
            assertEquals(frame == 40, frames >= 40);
        }
        // An open screen/overlay or missing world resets readiness, not merely pauses it.
        frames = ResourcePrewarmControl.advanceReadiness(frames, 40, false, true);
        assertEquals(0, frames);
        for (int frame = 1; frame <= 39; frame++)
            frames = ResourcePrewarmControl.advanceReadiness(frames, 40, true, true);
        frames = ResourcePrewarmControl.advanceReadiness(frames, 40, true, false);
        assertEquals(1, frames, "a different native connection must earn its own 40 frames");
        for (int frame = 2; frame <= 40; frame++) {
            frames = ResourcePrewarmControl.advanceReadiness(frames, 40, true, true);
            assertEquals(frame == 40, frames >= 40);
        }
    }
}
