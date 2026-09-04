package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.PersistedOutputStatus;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatusKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

final class PersistedStatusTag {
    private static final int MAX_STATUSES = 256;

    static List<PersistedOutputStatus> readStatuses(ListTag tags) {
        var persisted = new ArrayList<PersistedOutputStatus>();
        for (var statusTag : tags) {
            if (!(statusTag instanceof CompoundTag status)) {
                continue;
            }
            ProfileKey key;
            StatusKind kind;
            try {
                key = new ProfileKey(status.getStringOr("networkId", ""),
                        PacketLimits.checkedOutputId(status.getStringOr("key", "")));
                kind = kindOf(status.getStringOr("kind", ""));
                if (kind == null) {
                    continue;
                }
            } catch (IllegalArgumentException e) {
                continue;
            }
            persisted.add(new PersistedOutputStatus(key, kind, Math.max(0, status.getLongOr("idleTicks", 0)),
                    Math.max(0, status.getDoubleOr("typicalTicks", 0)), status.getLongOr("acceptedAtTick", 0)));
            if (persisted.size() >= MAX_STATUSES) {
                break;
            }
        }
        return persisted;
    }

    static ListTag writeStatuses(List<PersistedOutputStatus> statuses) {
        var statusTags = new ListTag();
        for (var status : statuses) {
            if (status == null || status.key() == null || status.kind() == null
                    || statusTags.size() >= MAX_STATUSES) {
                continue;
            }
            var tag = new CompoundTag();
            tag.putString("networkId", status.key().networkId());
            tag.putString("key", status.key().outputId());
            tag.putString("kind", kindId(status.kind()));
            tag.putLong("idleTicks", Math.max(0, status.idleTicks()));
            tag.putDouble("typicalTicks", Math.max(0, status.typicalDurationTicks()));
            tag.putLong("acceptedAtTick", status.acceptedAtTick());
            statusTags.add(tag);
        }
        return statusTags;
    }

    private static String kindId(StatusKind kind) {
        return switch (kind) {
            case DELAYED -> "delayed";
            case WAITING -> "waiting";
            case NO_PROVIDER -> "no_provider";
            case NO_POWER -> "no_power";
        };
    }

    private static StatusKind kindOf(String id) {
        return switch (id) {
            case "delayed" -> StatusKind.DELAYED;
            case "waiting" -> StatusKind.WAITING;
            case "no_provider" -> StatusKind.NO_PROVIDER;
            case "no_power" -> StatusKind.NO_POWER;
            default -> null;
        };
    }

    private PersistedStatusTag() {
    }
}
