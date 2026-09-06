package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.time.Instant;
import java.util.List;

public final class TestDriverRuntime implements AutoCloseable {
    private final Minecraft minecraft = Minecraft.getInstance();
    private CraftPlanScenario scenario;
    private final InteractiveMcpServer endpoint;
    private final DriverOptions options;
    private final String driverFile;
    private final List<DriverOptions> cases;
    private final SuiteProgress progress;
    private int index;
    private boolean switching;
    private boolean switchingInProgress;
    private boolean finished;

    public TestDriverRuntime(DriverOptions options, String driverFile) throws Exception {
        this.options = options;
        this.driverFile = driverFile;
        cases = options.scenario().equals("suite") ? SuitePlan.read(options) : List.of(options);
        progress = options.scenario().equals("suite") ? new SuiteProgress(cases) : null;
        if (progress != null) {
            for (var item : cases) {
                SuitePlan.verifyWorld(minecraft.gameDirectory.toPath().resolve("saves"), item);
            }
            progress.start(Instant.now());
            writeProgress();
        }
        minecraft.execute(() -> GLFW.glfwMaximizeWindow(minecraft.getWindow().getWindow()));
        scenario = new CraftPlanScenario(minecraft, cases.get(0), driverFile);
        endpoint = options.interactive() ? new InteractiveMcpServer(minecraft, scenario, options) : null;
    }

    public void tick() {
        if (finished) {
            return;
        }
        if (switching) {
            switchCase();
            return;
        }
        scenario.tick();
        if (progress == null) {
            return;
        }
        if (scenario.state() == ScenarioState.RESULT_WRITTEN || scenario.state() == ScenarioState.FAILED) {
            switching = true;
            try {
                boolean next = progress.finish(scenario.state() == ScenarioState.RESULT_WRITTEN, Instant.now());
                writeProgress();
                if (next) {
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
        try {
            if (!switchingInProgress) {
                switchingInProgress = true;
                DriverPlatform.clearLevel(minecraft);
                UiObservationStore.reset();
                return;
            }
            var item = cases.get(++index);
            SuitePlan.verifyWorld(minecraft.gameDirectory.toPath().resolve("saves"), item);
            scenario = new CraftPlanScenario(minecraft, item, driverFile);
            progress.start(Instant.now());
            writeProgress();
            DriverPlatform.openWorld(minecraft, item.world());
            switching = false;
            switchingInProgress = false;
        } catch (Exception error) {
            finished = true;
            throw new IllegalStateException("Cannot advance UI smoke suite", error);
        }
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
