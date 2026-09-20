package com.ctux.ae2craftingtime.testdriver;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

final class ResourceFixtureFluid {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(
            ForgeRegistries.Keys.FLUID_TYPES, TestDriverMod.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(
            ForgeRegistries.FLUIDS, TestDriverMod.MOD_ID);
    private static final RegistryObject<FluidType> TYPE = TYPES.register("resource_fixture_fluid", () -> new FluidType(
            FluidType.Properties.create().density(1_000).viscosity(1_000)) {
        @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return new ResourceLocation("minecraft", "block/water_still"); }
                @Override public ResourceLocation getFlowingTexture() { return new ResourceLocation("minecraft", "block/water_flow"); }
                @Override public int getTintColor() { return 0xff00ffff; }
            });
        }
    });
    private static final RegistryObject<FlowingFluid> SOURCE = FLUIDS.register("resource_fixture_fluid",
            () -> new ForgeFlowingFluid.Source(properties()));
    private static final RegistryObject<FlowingFluid> FLOWING = FLUIDS.register("resource_fixture_fluid_flowing",
            () -> new ForgeFlowingFluid.Flowing(properties()));

    static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
    static appeng.api.stacks.AEKey key() { return appeng.api.stacks.AEFluidKey.of(SOURCE.get()); }
    static java.util.Map<String, Object> facts() {
        return java.util.Map.of("id", ForgeRegistries.FLUIDS.getKey(SOURCE.get()).toString(),
                "bucketItem", ForgeRegistries.ITEMS.getKey(SOURCE.get().getBucket()).toString(),
                "hasBucket", SOURCE.get().getBucket() != net.minecraft.world.item.Items.AIR,
                "tintArgb", 0xff00ffff);
    }

    private static ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(TYPE, SOURCE, FLOWING).tickRate(5);
    }
    private ResourceFixtureFluid() { }
}
