package com.ctux.ae2craftingtime.testdriver;

final class ServerDriverPlatform {
    static appeng.api.stacks.AEKey bucketlessResourceKey() { throw new UnsupportedOperationException(); }
    static java.util.Map<String, Object> resourceFacts(ResourceFixtureControl.Case resourceCase) {
        return java.util.Map.of("storageValidated", true, "chemical", false);
    }
    static byte[] encodeResourceKey(appeng.api.stacks.AEKey key, net.minecraft.server.level.ServerPlayer player) {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), player.registryAccess());
        try {
            appeng.api.stacks.AEKey.writeKey(buffer, key);
            var bytes = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }
    static boolean isModLoaded(String id) {
        return net.neoforged.fml.ModList.get().isLoaded(id);
    }
    static void installResourceStorage(appeng.blockentity.storage.DriveBlockEntity drive, boolean chemical) {
        if (chemical) throw new IllegalStateException("chemical resource fixture is unsupported on 26.1.2");
        var inventory = drive.getInternalInventory();
        if (inventory.getStackInSlot(1).isEmpty()) inventory.setItemDirect(1,
                appeng.core.definitions.AEItems.FLUID_CELL_1K.stack());
        drive.getMainNode().getGrid().getStorageService().invalidateCache();
    }

    static WirelessTerminalFixture wcwtTerminal() {
        throw new IllegalStateException("WCWT is unavailable for 26.1.2");
    }

    static net.minecraft.world.item.ItemStack processingPattern(appeng.api.stacks.GenericStack input,
            appeng.api.stacks.GenericStack output) {
        return appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                java.util.List.of(input), java.util.List.of(output));
    }
    static net.minecraft.world.item.ItemStack processingPattern(java.util.List<appeng.api.stacks.GenericStack> inputs,
            appeng.api.stacks.GenericStack output) {
        return appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                inputs, java.util.List.of(output));
    }
    static appeng.api.crafting.IPatternDetails substitutePattern(appeng.api.crafting.IPatternDetails original,
            appeng.api.stacks.AEKey substitute) {
        return new appeng.api.crafting.IPatternDetails() {
            public appeng.api.stacks.AEItemKey getDefinition() { return original.getDefinition(); }
            public java.util.List<appeng.api.stacks.GenericStack> getOutputs() { return original.getOutputs(); }
            public IInput[] getInputs() {
                var input = original.getInputs()[0];
                return new IInput[] { new IInput() {
                    public appeng.api.stacks.GenericStack[] getPossibleInputs() {
                        return new appeng.api.stacks.GenericStack[] { input.getPossibleInputs()[0],
                                new appeng.api.stacks.GenericStack(substitute, input.getPossibleInputs()[0].amount()) };
                    }
                    public long getMultiplier() { return input.getMultiplier(); }
                    public boolean isValid(appeng.api.stacks.AEKey key, net.minecraft.world.level.Level level) {
                        return substitute.equals(key) || input.isValid(key, level);
                    }
                    public appeng.api.stacks.AEKey getRemainingKey(appeng.api.stacks.AEKey key) { return input.getRemainingKey(key); }
                } };
            }
        };
    }
    private ServerDriverPlatform() {}
}
