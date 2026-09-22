package com.ctux.ae2craftingtime.mc1201;

/** Native registry context for the shared display-key boundary checks. */
final class ProviderDisplayKeyTestContext {
    static net.minecraft.network.FriendlyByteBuf buffer() {
        return new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    }

    static Object persistence() {
        return null;
    }


}