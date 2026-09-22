package com.ctux.ae2craftingtime.mc1201.net;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Version-specific AE2 packet boundary; the shared codec enforces the byte limit. */
public final class ProviderDisplayKeyPacket {
    public static final int MAX_BYTES = 16 * 1024;

    public static void write(FriendlyByteBuf buffer, AEKey key) {
        buffer.writeBoolean(key != null);
        if (key != null) AEKey.writeKey((RegistryFriendlyByteBuf) buffer, key);
    }

    public static AEKey read(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return null;
        buffer.markReaderIndex();
        int rawId = buffer.readVarInt();
        if (rawId < 0 || rawId > 127 || AEKeyType.fromRawId(rawId) == null) {
            // Unknown optional integration: retain the plate and discard only its terminal key field.
            buffer.readerIndex(buffer.writerIndex());
            return null;
        }
        buffer.resetReaderIndex();
        return AEKey.readKey((RegistryFriendlyByteBuf) buffer);
    }

    private ProviderDisplayKeyPacket() {
    }
}
