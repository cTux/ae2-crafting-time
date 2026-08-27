package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PersistedOutputSamples;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;

public final class Ae2CraftingTimeSavedData extends SavedData {
    public static final String FILE_ID = "ae2-crafting-time";
    public static final SavedData.Factory<Ae2CraftingTimeSavedData> FACTORY = new SavedData.Factory<>(
            Ae2CraftingTimeSavedData::new,
            Ae2CraftingTimeSavedData::load,
            DataFixTypes.LEVEL);

    private List<PersistedOutputSamples> samples = List.of();

    public static Ae2CraftingTimeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new Ae2CraftingTimeSavedData();
        if (!tag.contains("version", Tag.TAG_INT) || tag.getInt("version") == PersistedSamplesTag.VERSION) {
            data.samples = PersistedSamplesTag.readOutputs(tag.getList("outputs", Tag.TAG_COMPOUND));
        }
        return data;
    }

    public void replaceFrom(List<PersistedOutputSamples> samples) {
        this.samples = List.copyOf(samples);
        setDirty();
    }

    public List<PersistedOutputSamples> samples() {
        return samples;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", PersistedSamplesTag.VERSION);
        tag.put("outputs", PersistedSamplesTag.writeOutputs(samples));
        return tag;
    }
}
