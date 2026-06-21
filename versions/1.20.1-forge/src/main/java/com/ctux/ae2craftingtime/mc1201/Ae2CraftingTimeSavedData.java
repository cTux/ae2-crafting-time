package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PersistedCraftSample;
import com.ctux.ae2craftingtime.core.PersistedOutputSamples;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public final class Ae2CraftingTimeSavedData extends SavedData {
    public static final String FILE_ID = "ae2-crafting-time";
    private static final int VERSION = 1;

    private List<PersistedOutputSamples> samples = List.of();

    public static Ae2CraftingTimeSavedData load(CompoundTag tag) {
        var data = new Ae2CraftingTimeSavedData();
        data.samples = readOutputs(tag.getList("outputs", Tag.TAG_COMPOUND));
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
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("version", VERSION);
        tag.put("outputs", writeOutputs(samples));
        return tag;
    }

    private static List<PersistedOutputSamples> readOutputs(ListTag outputs) {
        var persisted = new ArrayList<PersistedOutputSamples>();
        for (var outputTag : outputs) {
            var output = (CompoundTag) outputTag;
            var sampleTags = output.getList("samples", Tag.TAG_COMPOUND);
            var samples = new ArrayList<PersistedCraftSample>();
            for (var sampleTag : sampleTags) {
                var sample = (CompoundTag) sampleTag;
                samples.add(new PersistedCraftSample(sample.getLong("amount"), sample.getLong("durationTicks")));
            }
            persisted.add(new PersistedOutputSamples(
                    new ProfileKey(output.getString("key")),
                    ProfileUnit.valueOf(output.getString("unit")),
                    samples));
        }
        return persisted;
    }

    private static ListTag writeOutputs(List<PersistedOutputSamples> outputs) {
        var outputTags = new ListTag();
        for (var output : outputs) {
            var tag = new CompoundTag();
            tag.putString("key", output.key().outputId());
            tag.putString("unit", output.unit().name());
            var sampleTags = new ListTag();
            for (var sample : output.samples()) {
                var sampleTag = new CompoundTag();
                sampleTag.putLong("amount", sample.amount());
                sampleTag.putLong("durationTicks", sample.durationTicks());
                sampleTags.add(sampleTag);
            }
            tag.put("samples", sampleTags);
            outputTags.add(tag);
        }
        return outputTags;
    }
}
