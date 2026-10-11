package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import java.util.List;
import org.junit.jupiter.api.Test;

class TreeAmountObservationTest {
    private static final AEKeyType TYPE = new AEKeyType(new net.minecraft.resources.ResourceLocation("test", "item"),
            Key.class, net.minecraft.network.chat.Component.literal("Test item")) {
        public AEKey readFromPacket(net.minecraft.network.FriendlyByteBuf buffer) { throw new UnsupportedOperationException(); }
        public AEKey loadKeyFromTag(net.minecraft.nbt.CompoundTag tag) { throw new UnsupportedOperationException(); }
    };
    private static final class Key extends AEKey {
        private final net.minecraft.resources.ResourceLocation id;
        Key(String name) { id = new net.minecraft.resources.ResourceLocation("minecraft", name); }
        public AEKeyType getType() { return TYPE; }
        public AEKey dropSecondary() { return this; }
        public net.minecraft.nbt.CompoundTag toTag() { throw new UnsupportedOperationException(); }
        public Object getPrimaryKey() { return id; }
        public net.minecraft.resources.ResourceLocation getId() { return id; }
        public void writeToPacket(net.minecraft.network.FriendlyByteBuf buffer) { throw new UnsupportedOperationException(); }
        protected net.minecraft.network.chat.Component computeDisplayName() { return net.minecraft.network.chat.Component.literal(id.toString()); }
        public void addDrops(long amount, List<net.minecraft.world.item.ItemStack> drops,
                net.minecraft.world.level.Level level, net.minecraft.core.BlockPos position) { throw new UnsupportedOperationException(); }
    }
    record Amount(Long craftAmount) {}
    static final class HelperNode {
        public GenericStack stack;
        public Amount amountHelper;
        public List<HelperNode> subNodes;
        HelperNode(GenericStack stack, long amount, List<HelperNode> children) {
            this.stack = stack; amountHelper = new Amount(amount); subNodes = children;
        }
    }
    record Process(List<DirectNode> inputs) {}
    record DirectNode(GenericStack output, Long amount, List<Process> inputs) {}
    record Entry(DirectNode node) {}

    @Test
    void bothWidgetLayoutsExposeExactAmountsAndRequireDistinctSiblingAmounts() {
        var stone = new GenericStack(new Key("stone"), 1);
        var glass = new GenericStack(new Key("glass"), 1);
        var root = new GenericStack(new Key("smooth_stone"), 1);
        var rows = List.of(new UiSnapshot.Row("minecraft:stone", 3, 0, new Rect(0,0,16,16), List.of()),
                new UiSnapshot.Row("minecraft:glass", 7, 0, new Rect(20,0,16,16), List.of()));
        try {
            TreeAmountObservation.reset();
            TreeAmountObservation.record(new HelperNode(root, 1, List.of(
                    new HelperNode(stone, 3, null), new HelperNode(glass, 7, List.of()))), true);
            assertEquals(List.of("minecraft:stone", "minecraft:glass"), TreeAmountObservation.pair(rows));
            TreeAmountObservation.record(new HelperNode(stone, 9_007_199_254_740_993L, null), true);
            assertEquals(9_007_199_254_740_993L, TreeAmountObservation.amount("minecraft:stone"));
            assertEquals(0, TreeAmountObservation.amount("minecraft:glass"));
            TreeAmountObservation.clear();
            assertEquals(0, TreeAmountObservation.amount("minecraft:stone"));
            TreeAmountObservation.reset();
            TreeAmountObservation.record(new Entry(new DirectNode(root, 1L, List.of(new Process(List.of(
                    new DirectNode(stone, 3L, null), new DirectNode(glass, 7L, List.of())))))), false);
            assertEquals(List.of("minecraft:stone", "minecraft:glass"), TreeAmountObservation.pair(rows));
            TreeAmountObservation.record(new Entry(new DirectNode(glass, 7L, null)), false);
            assertEquals(7, TreeAmountObservation.amount("minecraft:glass"));
            assertTrue(TreeAmountObservation.pair(List.of(rows.get(0))).isEmpty());
            TreeAmountObservation.reset();
            assertTrue(TreeAmountObservation.pair(rows).isEmpty());
        } finally { TreeAmountObservation.reset(); }
    }
    @Test
    void absentNativeAmountsAndStacksAreNotCraftedNodes() {
        var stone = new GenericStack(new Key("stone"), 1);
        try {
            var helper = new HelperNode(stone, 1, null);
            helper.amountHelper = null;
            TreeAmountObservation.record(helper, true);
            assertEquals(0, TreeAmountObservation.amount("minecraft:stone"));
            helper.amountHelper = new Amount(null);
            TreeAmountObservation.record(helper, true);
            assertEquals(0, TreeAmountObservation.amount("minecraft:stone"));
            helper.stack = null;
            TreeAmountObservation.record(helper, true);
            TreeAmountObservation.record(new Entry(new DirectNode(stone, null, null)), false);
            assertEquals(0, TreeAmountObservation.amount("minecraft:stone"));
            TreeAmountObservation.record(new Entry(new DirectNode(null, null, null)), false);
            TreeAmountObservation.record(null);
            assertEquals(0, TreeAmountObservation.amount("minecraft:stone"));
        } finally { TreeAmountObservation.reset(); }
    }
}
