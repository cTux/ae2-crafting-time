package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import com.ctux.ae2craftingtime.core.CraftProfiler;
import com.ctux.ae2craftingtime.core.CraftingJobEstimate;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.mc1201.mixin.TrinityCpuLogicMixin;
import com.electronwill.nightconfig.core.CommentedConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import net.neoforged.neoforge.common.ModConfigSpec;
import sun.misc.Unsafe;

class TrinityCpuClockBoundaryTest {
    private static final long OLD_WORLD_TIME = 2_000_000;
    private static Object previousConfig;
    private static CraftProfiler previousProfiler;
    private static final Map<Object, Object> previousConfigCache = new IdentityHashMap<>();

    @BeforeAll
    static void isolateProfilerAndConfig() throws Exception {
        var cache = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cache.setAccessible(true);
        for (var valueField : Ae2CraftingTimeConfig.class.getFields()) {
            if (valueField.get(null) instanceof ModConfigSpec.ConfigValue<?> value) {
                previousConfigCache.put(value, cache.get(value));
            }
        }
        var loadedType = Class.forName("net.neoforged.fml.config.LoadedConfig");
        var modConfigType = Class.forName("net.neoforged.fml.config.ModConfig");
        var constructor = loadedType.getDeclaredConstructor(CommentedConfig.class, Path.class, modConfigType);
        constructor.setAccessible(true);
        var loaded = constructor.newInstance(CommentedConfig.inMemory(), null, null);
        var field = Ae2CraftingTimeConfig.SPEC.getClass().getDeclaredField("loadedConfig");
        field.setAccessible(true);
        previousConfig = field.get(Ae2CraftingTimeConfig.SPEC);
        field.set(Ae2CraftingTimeConfig.SPEC, loaded);
        previousProfiler = profiler();
        var profilerField = ProfilerBridge.class.getDeclaredField("PROFILER");
        profilerField.setAccessible(true);
        profilerField.set(null, new CraftProfiler(10));
    }

    @AfterAll
    static void restoreProfilerAndConfig() throws Exception {
        var profilerField = ProfilerBridge.class.getDeclaredField("PROFILER");
        profilerField.setAccessible(true);
        profilerField.set(null, previousProfiler);
        var cache = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cache.setAccessible(true);
        previousConfigCache.forEach((value, old) -> {
            try { cache.set(value, old); }
            catch (IllegalAccessException failure) { throw new AssertionError(failure); }
        });
        var field = Ae2CraftingTimeConfig.SPEC.getClass().getDeclaredField("loadedConfig");
        field.setAccessible(true);
        field.set(Ae2CraftingTimeConfig.SPEC, previousConfig);
    }

    @Test
    void trinityCallbackClockReadsOwningWorldAgeRatherThanProcessTicks() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var levelData = (WritableLevelData) Proxy.newProxyInstance(
                WritableLevelData.class.getClassLoader(), new Class<?>[] { WritableLevelData.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getGameTime")) return OLD_WORLD_TIME;
                    throw new AssertionError("Unexpected level-data access: " + method.getName());
                });
        var level = (ServerLevel) unsafe().allocateInstance(ServerLevel.class);
        var dataField = Level.class.getDeclaredField("levelData");
        dataField.setAccessible(true);
        dataField.set(level, levelData);

        assertEquals(OLD_WORLD_TIME, callbackTick(new FakeCpu(grid(node(level)))));
    }

    @Test
    void missingOwnerFailsClosedInsideExistingObserver() throws Exception {
        var observe = TrinityCpuLogicMixin.class.getDeclaredMethod("ae2craftingtime$observe", Runnable.class);
        observe.setAccessible(true);
        for (var cpu : new FakeCpu[] { new FakeCpu(null), new FakeCpu(grid(null)),
                new FakeCpu(grid(node(null))) }) {
            var failure = assertThrows(InvocationTargetException.class, () -> callbackTick(cpu));
            assertInstanceOf(IllegalStateException.class, failure.getCause());

            var observer = (ProbeObserver) unsafe().allocateInstance(ProbeObserver.class);
            observer.cpu = cpu;
            var profiler = profiler();
            var key = new ProfileKey("trinity-clock-boundary-" + System.identityHashCode(cpu), "minecraft:glass");
            profiler.start(key, cpu, 1, ProfileUnit.ITEM, OLD_WORLD_TIME - 100);
            profiler.complete(key, cpu, 1, OLD_WORLD_TIME - 80);
            profiler.flushCompletedSamples();
            profiler.start(key, cpu, 1, ProfileUnit.ITEM, OLD_WORLD_TIME);
            profiler.setJobEstimate(cpu, new CraftingJobEstimate(key, Map.of(key, 1L), Map.of()));
            assertTrue(profiler.hasPending(key));
            assertTrue(profiler.jobEstimate(cpu).isPresent());
            assertEquals(1, profiler.stats(key).orElseThrow().sampleCount());
            assertFalse(warned(observer));
            observe.invoke(observer, (Runnable) () -> {
                try { callbackTick(cpu); }
                catch (InvocationTargetException error) { throw (IllegalStateException) error.getCause(); }
                catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            assertTrue(warned(observer));
            assertFalse(profiler.hasPending(key));
            assertTrue(profiler.jobEstimate(cpu).isEmpty());
            assertEquals(1, profiler.stats(key).orElseThrow().sampleCount());
        }
    }

    private static CraftProfiler profiler() throws Exception {
        var field = ProfilerBridge.class.getDeclaredField("PROFILER");
        field.setAccessible(true);
        return (CraftProfiler) field.get(null);
    }

    private static boolean warned(TrinityCpuLogicMixin observer) throws Exception {
        var field = TrinityCpuLogicMixin.class.getDeclaredField("ae2craftingtime$warned");
        field.setAccessible(true);
        return field.getBoolean(observer);
    }

    private static Unsafe unsafe() throws Exception {
        var field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static long callbackTick(Object cpu) throws ReflectiveOperationException {
        var method = TrinityCpuLogicMixin.class.getDeclaredMethod("tick", Object.class);
        method.setAccessible(true);
        return (long) method.invoke(null, cpu);
    }

    private static IGrid grid(IGridNode pivot) {
        return (IGrid) Proxy.newProxyInstance(IGrid.class.getClassLoader(), new Class<?>[] { IGrid.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getPivot")) return pivot;
                    throw new AssertionError("Unexpected grid access: " + method.getName());
                });
    }

    private static IGridNode node(ServerLevel level) {
        return (IGridNode) Proxy.newProxyInstance(IGridNode.class.getClassLoader(),
                new Class<?>[] { IGridNode.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return level;
                    throw new AssertionError("Unexpected node access: " + method.getName());
                });
    }

    public record FakeCpu(IGrid grid) { }

    private static final class ProbeObserver extends TrinityCpuLogicMixin {
        private Object cpu;
        private Object job;
    }
}
