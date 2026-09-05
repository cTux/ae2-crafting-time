package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProviderLocateRecords.StoredStart;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

final class PersistedProviderTag {
    private static final int MAX_NAME_LENGTH = 512;

    static List<StoredStart> readStarts(ListTag starts) {
        var persisted = new ArrayList<StoredStart>();
        for (var startTag : starts) {
            if (!(startTag instanceof CompoundTag start)) {
                continue;
            }
            ProfileKey key;
            UUID owner;
            try {
                key = new ProfileKey(start.getString("networkId"),
                        PacketLimits.checkedOutputId(start.getString("key")));
                owner = UUID.fromString(start.getString("owner"));
            } catch (IllegalArgumentException e) {
                continue;
            }
            var positions = new ArrayList<BlockPos>();
            for (var posTag : start.getList("positions", Tag.TAG_LONG)) {
                if (positions.size() >= PacketLimits.MAX_HIGHLIGHT_POSITIONS) {
                    break;
                }
                positions.add(BlockPos.of(((LongTag) posTag).getAsLong()));
            }
            var name = start.getString("name");
            if (name.length() > MAX_NAME_LENGTH) {
                continue;
            }
            var dimension = start.contains("dimension", Tag.TAG_STRING) ? start.getString("dimension") : "";
            persisted.add(new StoredStart(key, owner, dimension, positions,
                    name.isBlank() ? key.outputId() : name));
        }
        return persisted;
    }

    static ListTag writeStarts(List<StoredStart> starts) {
        var startTags = new ListTag();
        for (var start : starts) {
            if (start == null || start.key() == null || start.owner() == null) {
                continue;
            }
            var tag = new CompoundTag();
            tag.putString("networkId", start.key().networkId());
            tag.putString("key", start.key().outputId());
            tag.putString("owner", start.owner().toString());
            tag.putString("dimension", start.dimensionId() == null ? "" : start.dimensionId());
            var posTags = new ListTag();
            if (start.positions() != null) {
                for (var pos : start.positions()) {
                    if (pos == null || posTags.size() >= PacketLimits.MAX_HIGHLIGHT_POSITIONS) {
                        continue;
                    }
                    posTags.add(LongTag.valueOf(pos.asLong()));
                }
            }
            tag.put("positions", posTags);
            var name = start.outputName() == null ? "" : start.outputName();
            tag.putString("name", name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name);
            startTags.add(tag);
        }
        return startTags;
    }

    private PersistedProviderTag() {
    }
}
