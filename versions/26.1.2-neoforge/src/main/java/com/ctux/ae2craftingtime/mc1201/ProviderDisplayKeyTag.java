package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** AE2's typed key persistence boundary. Unknown optional key types lose only the icon. */
public final class ProviderDisplayKeyTag {
    public static CompoundTag write(AEKey key, Object registries) {
        if (key == null || !(registries instanceof DynamicOps<?> ops) || !(ops.empty() instanceof Tag)) {
            return null;
        }
        try {
            var tag = AEKey.CODEC.encodeStart(tagOps(ops), key).result().orElse(null);
            return tag instanceof CompoundTag compound ? compound : null;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static AEKey read(CompoundTag tag, Object registries) {
        if (tag == null || !(registries instanceof DynamicOps<?> ops) || !(ops.empty() instanceof Tag)) {
            return null;
        }
        try {
            return AEKey.CODEC.parse(tagOps(ops), tag).result().orElse(null);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static DynamicOps<Tag> tagOps(DynamicOps<?> ops) {
        return (DynamicOps<Tag>) ops;
    }

    private ProviderDisplayKeyTag() {
    }
}
