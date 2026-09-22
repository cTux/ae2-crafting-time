package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;

public interface RecurrentPlanMenu {
    long ae2craftingtime$summaryRevision();
    boolean ae2craftingtime$apply(PlanRecurrenceChunk chunk);
    boolean ae2craftingtime$applyStoredVariants(PlanRecurrenceChunk chunk, long updateRevision);
}
