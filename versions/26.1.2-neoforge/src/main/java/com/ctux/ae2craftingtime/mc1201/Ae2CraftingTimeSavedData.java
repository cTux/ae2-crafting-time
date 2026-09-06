package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PersistedOutputSamples;
import com.ctux.ae2craftingtime.core.PersistedOutputStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;

public final class Ae2CraftingTimeSavedData extends SavedData {
    public static final String FILE_ID = "ae2-crafting-time";
    public static final SavedDataType<Ae2CraftingTimeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("ae2craftingtime", FILE_ID),
            Ae2CraftingTimeSavedData::new,
            CompoundTag.CODEC.xmap(Ae2CraftingTimeSavedData::load, Ae2CraftingTimeSavedData::save),
            DataFixTypes.LEVEL);

    private List<PersistedOutputSamples> samples = List.of();
    private List<ProviderLocateRecords.StoredStart> providerStarts = List.of();
    private List<ProviderLocateRecords.LocateRecord> providerRecords = List.of();
    private List<PersistedOutputStatus> statuses = List.of();

    private static Ae2CraftingTimeSavedData load(CompoundTag tag) {
        var data = new Ae2CraftingTimeSavedData();
        if (tag.getIntOr("version", PersistedSamplesTag.VERSION) == PersistedSamplesTag.VERSION) {
            data.samples = PersistedSamplesTag.readOutputs(tag.getListOrEmpty("outputs"));
        }
        data.providerStarts = PersistedProviderTag.readStarts(tag.getListOrEmpty("providers"));
        data.providerRecords = PersistedProviderTag.readRecords(tag.getListOrEmpty("locateRecords"));
        data.statuses = PersistedStatusTag.readStatuses(tag.getListOrEmpty("statuses"));
        return data;
    }

    public void replaceFrom(List<PersistedOutputSamples> samples) {
        this.samples = List.copyOf(samples);
        setDirty();
    }

    public void replaceProviderStarts(List<ProviderLocateRecords.StoredStart> starts) {
        this.providerStarts = starts == null ? List.of() : List.copyOf(starts);
        setDirty();
    }

    public void replaceProviderRecords(List<ProviderLocateRecords.LocateRecord> records) {
        this.providerRecords = records == null ? List.of() : List.copyOf(records);
        setDirty();
    }

    public void replaceStatuses(List<PersistedOutputStatus> statuses) {
        this.statuses = statuses == null ? List.of() : List.copyOf(statuses);
        setDirty();
    }

    public List<PersistedOutputSamples> samples() {
        return samples;
    }

    public List<ProviderLocateRecords.StoredStart> providerStarts() {
        return providerStarts;
    }

    public List<ProviderLocateRecords.LocateRecord> providerRecords() {
        return providerRecords;
    }

    public List<PersistedOutputStatus> statuses() {
        return statuses;
    }

    private CompoundTag save() {
        if (Ae2CraftingTimeConfig.SPEC.isLoaded()) {
            ProfilerBridge.flushCompletedSamples();
        }
        var tag = new CompoundTag();
        tag.putInt("version", PersistedSamplesTag.VERSION);
        tag.put("outputs", PersistedSamplesTag.writeOutputs(samples));
        tag.put("providers", PersistedProviderTag.writeStarts(providerStarts));
        tag.put("locateRecords", PersistedProviderTag.writeRecords(providerRecords));
        tag.put("statuses", PersistedStatusTag.writeStatuses(statuses));
        return tag;
    }
}
