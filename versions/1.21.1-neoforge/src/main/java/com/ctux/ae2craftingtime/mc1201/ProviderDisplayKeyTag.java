package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

/** AE2's typed key persistence boundary. Unknown optional key types lose only the icon. */
public final class ProviderDisplayKeyTag {
    public static CompoundTag write(AEKey key, Object registries) {
        if (key == null || !(registries instanceof HolderLookup.Provider provider)) {
            return null;
        }
        try {
            return key.toTagGeneric(provider);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static AEKey read(CompoundTag tag, Object registries) {
        try {
            return tag == null || !(registries instanceof HolderLookup.Provider provider)
                    ? null : AEKey.fromTagGeneric(provider, tag);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private ProviderDisplayKeyTag() {
    }
}
