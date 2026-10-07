package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.me.crafting.CraftingStatusTableRenderer;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Checks the merged production wrappers inside the real client, once per status case. */
public final class StatusWrapperProbe {
    private static final org.apache.logging.log4j.Logger LOG =
            org.apache.logging.log4j.LogManager.getLogger("ae2ct-test-driver");
    private static final OptionFeature[] DISABLED = {
            OptionFeature.STATUS_ROWS, OptionFeature.COMPACT_STATUS_AMOUNTS,
            OptionFeature.CHANCE_OUTPUT_STATUS};
    private static boolean armed;
    private static boolean done;
    private static String failure;

    private StatusWrapperProbe() { }

    static void arm() {
        armed = true;
        done = false;
        failure = null;
    }

    public static void observe(CraftingStatusTableRenderer renderer, CraftingStatusEntry entry) {
        if (!armed || done) return;
        done = true;
        var options = ClientOptionsRuntime.current().features();
        var enabled = new boolean[DISABLED.length];
        for (int i = 0; i < DISABLED.length; i++) enabled[i] = options.enabled(DISABLED[i]);
        try {
            for (var feature : DISABLED) options.setEnabled(feature, false);
            var zero = new CraftingStatusEntry(entry.getSerial(), entry.getWhat(), 0, 0, 0);
            for (var name : List.of("ae2craftingtime$appendVisibleTimeToCraft",
                    "ae2craftingtime$appendTooltipTimeToCraft")) {
                var method = handler(renderer.getClass(), name);
                LOG.info("Status wrapper probe handler: {}", method.toGenericString());
                for (var input : List.<List<Component>>of(List.of(),
                        List.of(Component.literal("native"), Component.literal("addon")))) {
                    var calls = new int[1];
                    Operation<List<Component>> original = args -> {
                        calls[0]++;
                        if (args.length != 1 || args[0] != zero)
                            throw new IllegalStateException(name + " changed original arguments");
                        return input;
                    };
                    @SuppressWarnings("unchecked")
                    var result = (List<Component>) method.invoke(renderer, zero, original);
                    if (calls[0] != 1 || result == input || !result.equals(input))
                        throw new IllegalStateException(name + " changed original call or content");
                    result.add(Component.literal("mutable"));
                    if (result.size() != input.size() + 1 || input.size() > 0
                            && !input.get(1).getString().equals("addon"))
                        throw new IllegalStateException(name + " changed immutable addon input");
                }
            }
            LOG.info("Status wrapper probe PASS");
        } catch (Exception error) {
            failure = (error.getCause() == null ? error : error.getCause()).toString();
            LOG.error("Status wrapper probe FAIL: {}", failure);
        } finally {
            for (int i = 0; i < DISABLED.length; i++) options.setEnabled(DISABLED[i], enabled[i]);
        }
    }

    static void requirePassed() {
        if (!done || failure != null)
            throw new IllegalStateException("Status wrapper probe " + (done ? failure : "did not run"));
    }

    private static Method handler(Class<?> renderer, String name) {
        Method found = null;
        for (var method : renderer.getDeclaredMethods()) {
            var args = method.getParameterTypes();
            if (!method.getName().contains(name) || args.length != 2
                    || args[0] != CraftingStatusEntry.class || args[1] != Operation.class
                    || method.getReturnType() != List.class) continue;
            if (found != null) throw new IllegalStateException("Multiple merged handlers for " + name);
            found = method;
        }
        if (found == null) throw new IllegalStateException("Missing merged handler for " + name);
        found.setAccessible(true);
        return found;
    }
}
