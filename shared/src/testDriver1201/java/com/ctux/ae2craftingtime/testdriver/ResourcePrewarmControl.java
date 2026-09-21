package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The resource dispatcher remains inert until both native peers are bound by one arm. */
final class ResourcePrewarmControl {
    static final int MAX_BYTES = 65536;
    static final UUID PLAYER = UUID.fromString("446b6d0c-cadd-3e57-baf6-99d70f01a628");
    private static final Set<String> READY_KEYS = Set.of("schema", "epoch", "head", "bundle", "pid", "startTime",
            "player", "generation", "serverTicks", "worldFrames");
    private static final Set<String> ARM_KEYS = Set.of("schema", "epoch", "head", "bundle", "player", "generation",
            "serverReadySha256", "clientReadySha256");
    final Path root;
    final String epoch;
    final String head;
    final String bundle;
    final long deadlineMillis;
    private String acceptedArm;

    ResourcePrewarmControl(Path root, String epoch, String head, String bundle, long deadlineMillis) {
        this.root = root.toAbsolutePath().normalize();
        this.epoch = ResourceFixtureControl.parseUuid(epoch).toString();
        if (!head.matches("[a-f0-9]{40}") || !bundle.matches("[a-fA-F0-9]{64}") || deadlineMillis <= 0) {
            throw new IllegalArgumentException("invalid resource prewarm identity");
        }
        this.head = head;
        this.bundle = bundle.toLowerCase(java.util.Locale.ROOT);
        this.deadlineMillis = deadlineMillis;
    }

    static ResourcePrewarmControl configured(Path root) {
        return new ResourcePrewarmControl(root, System.getProperty("ae2craftingtime.test.prewarmEpoch"),
                System.getProperty("ae2craftingtime.test.prewarmHead"), System.getProperty("ae2craftingtime.test.prewarmBundle"),
                Long.parseLong(System.getProperty("ae2craftingtime.test.prewarmDeadline")));
    }

    static int nextConnectionAttempt(int previous, boolean disconnected, boolean armed, boolean fixtureExists) {
        if (previous < 0 || previous >= 3 || !disconnected || armed || fixtureExists) {
            throw new IllegalStateException("prewarm retry requires native disconnect, an empty fixture, no arm and fewer than three attempts");
        }
        return previous + 1;
    }

    static int advanceReadiness(int previous, int required, boolean ready, boolean sameConnection) {
        if ((required != 20 && required != 40) || previous < 0 || previous > required) {
            throw new IllegalArgumentException("invalid native readiness count");
        }
        return !ready ? 0 : !sameConnection ? 1 : Math.min(required, previous + 1);
    }

    void publishAttempt(int generation, long pid, Instant startTime) throws IOException {
        invalidateReadiness();
        write("attempt.json", ready(pid, startTime, generation, 0, 0));
    }

    private Map<String, String> attempt() throws IOException {
        var value = read("attempt.json");
        validateReceipt(value, Integer.parseInt(value.get("generation")), 0, 0);
        return value;
    }

    int attemptForJoin(int previous) throws IOException {
        int generation = Integer.parseInt(attempt().get("generation"));
        if (generation <= previous) throw new IllegalStateException("native join reused a prewarm attempt");
        return generation;
    }

    boolean attemptStillCurrent(int generation) throws IOException {
        int published = Integer.parseInt(attempt().get("generation"));
        if (published < generation) throw new IllegalStateException("prewarm attempt moved backwards");
        return published == generation;
    }

    void invalidateReadiness() throws IOException {
        waiting(System.currentTimeMillis());
        if (exists("arm.json") || exists("armed.json")) throw new IllegalStateException("prewarm connection changed after arm publication");
        for (String name : new String[]{"server-ready.json", "client-ready.json"}) {
            var file = path(name); checked(file); Files.deleteIfExists(file);
        }
    }

    void waiting(long nowMillis) {
        if (acceptedArm != null) throw new IllegalStateException("prewarm cannot restart after activation");
        if (nowMillis >= deadlineMillis) throw new IllegalStateException("resource prewarm exceeded 600 seconds");
        if (Files.exists(root.resolve("resource/command.properties"), LinkOption.NOFOLLOW_LINKS)
                || Files.exists(root.resolve("resource/state.properties"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("resource command or fixture state before activation");
        }
    }

    Map<String, String> ready(long pid, Instant startTime, int generation, int serverTicks, int worldFrames) {
        if (pid <= 0 || generation < 1 || generation > 3 || serverTicks < 0 || worldFrames < 0) {
            throw new IllegalArgumentException("invalid prewarm counters or process identity");
        }
        var result = identity(generation);
        result.put("pid", Long.toString(pid)); result.put("startTime", startTime.toString());
        result.put("serverTicks", Integer.toString(serverTicks)); result.put("worldFrames", Integer.toString(worldFrames));
        return Map.copyOf(result);
    }

    private LinkedHashMap<String, String> identity(int generation) {
        var result = new LinkedHashMap<String, String>();
        result.put("schema", "1"); result.put("epoch", epoch); result.put("head", head);
        result.put("bundle", bundle); result.put("player", PLAYER.toString());
        result.put("generation", Integer.toString(generation));
        return result;
    }

    void validateReady(Map<String, String> value, boolean server, int generation) {
        validateReceipt(value, generation, server ? 20 : 0, server ? 0 : 40);
    }

    private void validateReceipt(Map<String, String> value, int generation, int serverTicks, int worldFrames) {
        if (!value.keySet().equals(READY_KEYS)) throw new IllegalArgumentException("invalid prewarm receipt fields");
        validateIdentity(value, generation);
        if (Long.parseLong(value.get("pid")) <= 0 || Instant.parse(value.get("startTime")).toEpochMilli() <= 0
                || Integer.parseInt(value.get("serverTicks")) != serverTicks
                || Integer.parseInt(value.get("worldFrames")) != worldFrames) {
            throw new IllegalArgumentException("prewarm native readiness threshold or process identity mismatch");
        }
    }

    boolean currentServerReady(Map<String, String> value, int generation) {
        int publishedGeneration = Integer.parseInt(value.get("generation"));
        validateReady(value, true, publishedGeneration);
        if (publishedGeneration < generation) return false;
        validateIdentity(value, generation);
        return true;
    }

    private void validateIdentity(Map<String, String> value, int generation) {
        for (var entry : identity(generation).entrySet()) {
            if (!entry.getValue().equals(value.get(entry.getKey()))) throw new IllegalArgumentException("prewarm identity mismatch");
        }
        if (generation < 1 || generation > 3) throw new IllegalArgumentException("prewarm connection generation exceeded");
    }

    boolean accept(int generation, boolean liveConnection, boolean fixtureEmpty) throws IOException {
        if (!exists("arm.json")) return false;
        var armBytes = readBytes("arm.json");
        String armHash = CaptureEvidence.sha256(armBytes);
        if (acceptedArm != null) {
            if (!acceptedArm.equals(armHash)) throw new IllegalStateException("conflicting second prewarm activation");
            return true;
        }
        waiting(System.currentTimeMillis());
        if (!liveConnection || !fixtureEmpty) throw new IllegalStateException("prewarm arm requires the same empty live fixture");
        var arm = parse(armBytes);
        if (!arm.keySet().equals(ARM_KEYS)) throw new IllegalArgumentException("invalid prewarm arm fields");
        validateIdentity(arm, generation);
        var server = readBytes("server-ready.json"); var client = readBytes("client-ready.json");
        validateReady(parse(server), true, generation); validateReady(parse(client), false, generation);
        var attempt = attempt(); validateIdentity(attempt, generation);
        var clientIdentity = parse(client);
        if (!attempt.get("pid").equals(clientIdentity.get("pid"))
                || !attempt.get("startTime").equals(clientIdentity.get("startTime"))) {
            throw new IllegalArgumentException("prewarm client differs from the native attempt owner");
        }
        if (!CaptureEvidence.sha256(server).equals(arm.get("serverReadySha256"))
                || !CaptureEvidence.sha256(client).equals(arm.get("clientReadySha256"))) {
            throw new IllegalArgumentException("prewarm receipt changed before activation");
        }
        write("armed.json", arm);
        acceptedArm = armHash;
        return true;
    }

    boolean clientAccepted(int generation) throws IOException {
        if (!exists("armed.json")) return false;
        var armed = readBytes("armed.json");
        var arm = readBytes("arm.json");
        var value = parse(armed);
        // JSON map order is immaterial; compare decoded contents after strict parsing.
        if (!value.equals(parse(arm))) throw new IllegalArgumentException("prewarm server acceptance differs from arm");
        if (!value.keySet().equals(ARM_KEYS)) throw new IllegalArgumentException("invalid accepted arm fields");
        validateIdentity(value, generation);
        if (!CaptureEvidence.sha256(readBytes("server-ready.json")).equals(value.get("serverReadySha256"))
                || !CaptureEvidence.sha256(readBytes("client-ready.json")).equals(value.get("clientReadySha256"))) {
            throw new IllegalArgumentException("accepted prewarm receipt was changed");
        }
        acceptedArm = CaptureEvidence.sha256(arm);
        return true;
    }

    boolean exists(String name) { return Files.exists(path(name), LinkOption.NOFOLLOW_LINKS); }
    Map<String, String> read(String name) throws IOException { return parse(readBytes(name)); }
    private Path path(String name) {
        if (!Set.of("attempt.json", "server-ready.json", "client-ready.json", "arm.json", "armed.json").contains(name)) {
            throw new IllegalArgumentException("unknown prewarm file");
        }
        return root.resolve("prewarm").resolve(name);
    }

    private void checked(Path path) throws IOException {
        for (var ancestor = path; ancestor != null; ancestor = ancestor.getParent()) {
            if (Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)
                    && !ancestor.toRealPath().equals(ancestor.toAbsolutePath().normalize())) {
                throw new IllegalArgumentException("redirected prewarm path");
            }
        }
    }

    private byte[] readBytes(String name) throws IOException {
        var path = path(name); checked(path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("non-regular prewarm file");
        try (var stream = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            var bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("oversized prewarm file");
            return bytes;
        }
    }

    static Map<String, String> parse(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("oversized prewarm JSON");
        var values = new LinkedHashMap<String, String>();
        try (var reader = new JsonReader(new StringReader(new String(bytes, StandardCharsets.UTF_8)))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (reader.peek() != JsonToken.STRING && reader.peek() != JsonToken.NUMBER) throw new IllegalArgumentException("non-scalar prewarm value");
                String value = reader.nextString();
                if (key.length() > 64 || value.length() > 128 || values.putIfAbsent(key, value) != null) throw new IllegalArgumentException("duplicate or oversized prewarm field");
            }
            reader.endObject();
            reader.peek(); // Strict JsonReader rejects trailing content itself.
        }
        return Map.copyOf(values);
    }

    void write(String name, Map<String, String> values) throws IOException {
        var path = path(name); checked(path);
        var bytes = new Gson().toJson(values).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("oversized prewarm write");
        Files.createDirectories(path.getParent()); checked(path);
        var temporary = path.resolveSibling(name + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(temporary, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            DriverProgress.moveWithAccessDeniedRetry(temporary, path, (source, target) -> {
                // ATOMIC_MOVE permits replacing an existing target. The one-shot acknowledgment
                // instead uses a same-directory, non-replacing rename, including at the race boundary.
                if (name.equals("armed.json")) Files.move(source, target);
                else Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            });
        } finally { Files.deleteIfExists(temporary); }
    }
}
