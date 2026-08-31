package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class TestDriverRuntime implements AutoCloseable {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final CraftPlanScenario scenario;
    private final InteractiveMcpServer endpoint;

    public TestDriverRuntime(DriverOptions options, String driverFile) throws Exception {
        GLFW.glfwMaximizeWindow(minecraft.getWindow().getWindow());
        scenario = new CraftPlanScenario(minecraft, options, driverFile);
        endpoint = options.interactive() ? new InteractiveMcpServer(minecraft, scenario, options) : null;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            scenario.tick();
        }
    }

    @SubscribeEvent
    public void beforeRender(ScreenEvent.Render.Pre event) {
        UiObservationStore.begin(minecraft);
    }

    @SubscribeEvent
    public void afterRender(ScreenEvent.Render.Post event) {
        UiObservationStore.finish(minecraft);
    }

    @Override
    public void close() throws Exception {
        if (endpoint != null) {
            endpoint.close();
        }
    }
}
