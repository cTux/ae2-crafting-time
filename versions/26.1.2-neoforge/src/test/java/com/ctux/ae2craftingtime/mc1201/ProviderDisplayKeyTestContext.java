package com.ctux.ae2craftingtime.mc1201;

/** Native registry context for the shared display-key boundary checks. */
final class ProviderDisplayKeyTestContext {
    static net.minecraft.network.FriendlyByteBuf buffer() {
        return new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries());
    }

    static Object persistence() {
        return registries().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
    }

    private static net.minecraft.core.RegistryAccess registries() {
        return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }
}