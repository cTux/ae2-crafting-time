package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.time.Instant;
import java.util.List;

public final class TestDriverRuntime implements AutoCloseable {
    static long renderedFrames;
    private final Minecraft minecraft = Minecraft.getInstance();
    private CraftPlanScenario scenario;
    private final InteractiveMcpServer endpoint;
    private final DriverOptions options;
    private final String driverFile;
    private final List<DriverOptions> cases;
    private final SuiteProgress progress;
    private final boolean oneWorld;
    private java.util.concurrent.CompletableFuture<SuiteFixture> fixture;
    private java.util.concurrent.CompletableFuture<Integer> reset;
    private long resetStarted;
    private int index;
    private boolean switching;
    private boolean switchingInProgress;
    private boolean switchingNow;
    private final TestDriverLifecycleGuard lifecycle = new TestDriverLifecycleGuard();
    private net.minecraft.server.MinecraftServer stoppingServer;
    private boolean finalCleanup;
    private boolean finished;
    private int reconnectStep;
    private net.minecraft.client.multiplayer.ServerData reconnectServer;

    public TestDriverRuntime(DriverOptions options, String driverFile) throws Exception {
        this.options = options;
        this.driverFile = driverFile;
        cases = options.scenario().equals("suite") ? SuitePlan.read(options) : List.of(options);
        var continuing = options.continuation() != null;
        progress = options.scenario().equals("suite") ? (continuing
                ? SuiteProgress.resume(cases, options.output().resolve("result.json")) : new SuiteProgress(cases)) : null;
        index = progress == null ? 0 : progress.index();
        oneWorld = progress != null && new com.google.gson.Gson().fromJson(
                java.nio.file.Files.readString(options.output().resolve("suite-plan.json")), SuitePlan.class).schema() == 2;
        if (progress != null) {
            for (var item : cases) {
                SuitePlan.verifyWorld(minecraft.gameDirectory.toPath().resolve("saves"), item);
            }
            if (!continuing) progress.start(Instant.now());
            writeProgress();
        }
        minecraft.execute(() -> GLFW.glfwMaximizeWindow(minecraft.getWindow().handle()));
        scenario = new CraftPlanScenario(minecraft, cases.get(index), driverFile);
        endpoint = options.interactive() ? new InteractiveMcpServer(minecraft, scenario, options) : null;
    }

    public void tick() {
        renderedFrames++;
        if (finished || switchingNow || !lifecycle.enter()) {
            return;
        }
        try {
            tickLifecycle();
        } finally {
            lifecycle.exit();
        }
    }

    private void tickLifecycle() {
        if (switching) {
            switchCase();
            return;
        }
        if (oneWorld && minecraft.screen == null && minecraft.level != null && minecraft.player != null
                && minecraft.getSingleplayerServer() != null) {
            if (fixture == null) {
                var server = minecraft.getSingleplayerServer();
                var playerId = minecraft.player.getUUID();
                fixture = server.submit(() -> {
                    try {
                    SuitePlan.verifyWorld(minecraft.gameDirectory.toPath().resolve("saves"), options);
                    var world = minecraft.gameDirectory.toPath().resolve("saves").resolve(options.world());
                    if (!server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toRealPath().equals(world.toRealPath())) {
                        throw new IllegalStateException("Suite reset requires the requested disposable world");
                    }
                    return new SuiteFixture(server.overworld(), server.getPlayerList().getPlayer(playerId), FixtureMarker.read(world));
                    } catch (java.io.IOException error) {
                        throw new java.io.UncheckedIOException(error);
                    }
                });
            }
            if (!fixture.isDone()) return;
            fixture.join();
        }
        scenario.tick();
        if (scenario.reconnectRequested()) {
            switching = true;
            return;
        }
        if (progress == null) {
            return;
        }
        if (scenario.evidenceReady() && (scenario.state() == ScenarioState.RESULT_WRITTEN || scenario.state() == ScenarioState.FAILED)) {
            switching = true;
            try {
                boolean passed = scenario.state() == ScenarioState.RESULT_WRITTEN;
                boolean next = progress.finish(passed, Instant.now());
                writeProgress();
                if (next) {
                    switching = true;
                } else if (passed && oneWorld) {
                    finalCleanup = true;
                    switching = true;
                } else {
                    finished = true;
                    minecraft.stop();
                }
            } catch (Exception error) {
                finished = true;
                throw new IllegalStateException("Cannot advance UI smoke suite", error);
            }
        }
    }

    private void switchCase() {
        switchingNow = true;
        try {
            if (scenario.reconnectRequested() || reconnectStep != 0) {
                reconnectScenario();
                return;
            }
            if (oneWorld) {
                var server = minecraft.getSingleplayerServer();
                if (reset == null) {
                    resetStarted = System.nanoTime();
                    minecraft.player.closeContainer();
                    minecraft.setScreen(null);
                    var playerId = minecraft.player.getUUID();
                    reset = server.submit(() -> {
                        fixture.join().restore(server.getPlayerList().getPlayer(playerId));
                        return server.getTickCount();
                    });
                    return;
                }
                if (System.nanoTime() - resetStarted > java.time.Duration.ofSeconds(30).toNanos()) {
                    throw new IllegalStateException("Suite fixture reset timed out");
                }
                if (!reset.isDone() || server.getTickCount() < reset.join() + 2) return;
                minecraft.gui.getChat().clearMessages(true);
                com.ctux.ae2craftingtime.testdriver.mixin.ClientStatsAccessor.ae2craftingtime_test_driver$networkAmounts().clear();
                com.ctux.ae2craftingtime.mc1201.ClientStats.CACHE.clear();
                com.ctux.ae2craftingtime.mc1201.ClientStatsRequests.clear();
                com.ctux.ae2craftingtime.mc1201.ProviderHighlightClient.onSessionEnd();
                UiObservationStore.reset();
                if (finalCleanup) {
                    finished = true;
                    minecraft.stop();
                    return;
                }
                var item = cases.get(++index);
                scenario = new CraftPlanScenario(minecraft, item, driverFile);
                progress.start(Instant.now());
                writeProgress();
                System.out.println("AE2CT suite fixture-reset case=" + item.scenario() + " world=" + item.world()
                        + " durationNanos=" + (System.nanoTime() - resetStarted) + " utc=" + Instant.now());
                reset = null;
                switching = false;
                return;
            }
            if (!switchingInProgress) {
                switchingInProgress = true;
                stoppingServer = minecraft.getSingleplayerServer();
                minecraft.packetProcessor().processQueuedPackets();
                DriverPlatform.clearLevel(minecraft);
                UiObservationStore.reset();
                return;
            }
            if (stoppingServer != null && stoppingServer.getRunningThread().isAlive()) {
                return;
            }
            stoppingServer = null;
            var item = cases.get(++index);
            SuitePlan.verifyWorld(minecraft.gameDirectory.toPath().resolve("saves"), item);
            scenario = new CraftPlanScenario(minecraft, item, driverFile);
            progress.start(Instant.now());
            writeProgress();
            switching = false;
            switchingInProgress = false;
            DriverPlatform.openWorld(minecraft, item.world());
        } catch (Exception error) {
            finished = true;
            throw new IllegalStateException("Cannot advance UI smoke suite", error);
        } finally {
            switchingNow = false;
        }
    }

    private void reconnectScenario() {
        if (reconnectStep == 0) {
            reconnectServer = minecraft.getCurrentServer();
            stoppingServer = minecraft.getSingleplayerServer();
            DriverPlatform.disconnectLevel(minecraft);
            reconnectStep = 1;
            return;
        }
        if (reconnectStep == 1) {
            if (stoppingServer != null && stoppingServer.getRunningThread().isAlive()) return;
            stoppingServer = null;
            UiObservationStore.reset();
            if (reconnectServer == null) DriverPlatform.openWorld(minecraft, options.world());
            else DriverPlatform.connectServer(minecraft, reconnectServer);
            reconnectStep = 2;
            return;
        }
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || (reconnectServer == null && minecraft.getSingleplayerServer() == null)
                || (reconnectServer != null && minecraft.getCurrentServer() == null)) return;
        scenario.reconnected();
        reconnectServer = null;
        reconnectStep = 0;
        switching = false;
    }

    private void writeProgress() throws java.io.IOException {
        AtomicResultWriter.write(options.output(), progress.snapshot(ProcessHandle.current().pid()));
    }

    public void beforeRender() {
        UiObservationStore.begin(minecraft);
    }

    public void afterRender() {
        UiObservationStore.finish(minecraft);
    }

    @Override
    public void close() throws Exception {
        if (endpoint != null) {
            endpoint.close();
        }
    }
}
