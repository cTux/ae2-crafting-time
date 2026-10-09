package com.ctux.ae2craftingtime.testdriver;

import appeng.api.stacks.GenericStack;
import com.ctux.ae2craftingtime.core.IntegrationRead;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import java.util.ArrayList;
import java.util.List;

/** Reads the actual optional-widget amount and sibling identity, never a displayed approximation. */
public final class TreeAmountObservation {
    private static String output;
    private static long amount;
    private static final List<List<String>> siblings = new ArrayList<>();

    static void reset() { clear(); siblings.clear(); }
    public static void clear() { output = null; amount = 0; }
    public static void record(Object entry) {
        if (entry == null) { clear(); return; }
        record(entry, entry.getClass().getName().contains("CraftingTreeHelper$Node"));
    }
    static void record(Object entry, boolean helper) {
        var data = helper ? entry : IntegrationRead.invoke(entry, "node", Object.class);
        var stack = stack(data, helper);
        if (stack == null) { clear(); return; }
        output = stack.what().getId().toString();
        var value = helper ? IntegrationRead.field(IntegrationRead.field(data, "amountHelper", Object.class),
                "craftAmount", Number.class) : IntegrationRead.invoke(data, "amount", Number.class);
        amount = value == null ? 0 : AeKeyAmounts.normalize(stack.what(), value.longValue());
        if (helper) {
            var nodes = IntegrationRead.field(data, "subNodes", List.class);
            group(nodes, helper);
        } else {
            var processes = IntegrationRead.invoke(data, "inputs", List.class);
            if (processes != null) for (var process : processes) {
                var nodes = IntegrationRead.invoke(process, "inputs", List.class);
                group(nodes, helper);
            }
        }
    }
    private static void group(List<?> nodes, boolean helper) {
        if (nodes != null) siblings.add(nodes.stream().map(child -> stack(child, helper))
                .filter(java.util.Objects::nonNull).map(child -> child.what().getId().toString()).toList());
    }
    private static GenericStack stack(Object data, boolean helper) {
        return helper ? IntegrationRead.field(data, "stack", GenericStack.class)
                : IntegrationRead.invoke(data, "output", GenericStack.class);
    }
    static long amount(String id) { return id.equals(output) ? amount : 0; }
    static List<String> pair(List<UiSnapshot.Row> rows) {
        for (var group : siblings) {
            var candidates = rows.stream().filter(row -> group.contains(row.outputId()) && row.craftAmount() > 0).toList();
            for (var first : candidates) for (var second : candidates)
                if (!first.outputId().equals(second.outputId()) && first.craftAmount() != second.craftAmount())
                    return List.of(first.outputId(), second.outputId());
        }
        return List.of();
    }
    private TreeAmountObservation() {}
}
