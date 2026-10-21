package com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu;

import java.util.UUID;

/** Test-only identity stand-in for the optional native CPU class; no addon code is loaded. */
public final class TrinityDataCoreVirtualCpu {
    public final Logic logic = new Logic();

    public static final class Logic { public Job job; }
    public record Job(Link link) {}
    public record Link(UUID id) { public UUID getCraftingID() { return id; } }
}
