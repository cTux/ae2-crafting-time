package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import net.minecraft.nbt.CompoundTag;

/** AE2's typed key persistence boundary. Unknown optional key types lose only the icon. */
public final class ProviderDisplayKeyTag {
    public static CompoundTag write(AEKey key, Object registries) {
        try {
            return key == null ? null : key.toTagGeneric();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static AEKey read(CompoundTag tag, Object registries) {
        try {
            return tag == null ? null : AEKey.fromTagGeneric(tag);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private ProviderDisplayKeyTag() {
    }
}
