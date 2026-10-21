package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AECraftingPattern;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityPatternCoreBlockEntity;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The real mounted Trinity pattern core executes vanilla book cloning in a counted batch. */
final class DataEnergisticsTrinityCountedFixture extends DataEnergisticsTrinityFixture {
    private static final AEItemKey CONSUMABLE = AEItemKey.of(Items.WRITABLE_BOOK);
    private AEItemKey original;
    private AEItemKey copy;
    private long originalBefore;
    private boolean patternInstalled;
    private CollectionEvidence collection;

    @Override protected boolean requiresImmediateWorkerCapture() { return true; }
    @Override protected boolean requiresFurnaceOutputHold() { return false; }

    @Override
    protected boolean finishVariant(ServerPlayer player, IGrid grid, Placement placement) {
        var level = player.serverLevel();
        if (!patternInstalled) {
            var recipeId = ResourceLocation.withDefaultNamespace("book_cloning");
            var recipe = (CraftingRecipe) level.getRecipeManager().byKey(recipeId)
                    .orElseThrow(() -> new IllegalStateException("vanilla book cloning recipe is unavailable")).value();
            var book = new ItemStack(Items.WRITTEN_BOOK);
            book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                    Filterable.passThrough("Original"), "Trinity", 0,
                    List.of(Filterable.passThrough(Component.literal("Retained page"))), true));
            book.set(DataComponents.CUSTOM_NAME, Component.literal("retained book"));
            var ingredients = new ItemStack[9];
            Arrays.fill(ingredients, ItemStack.EMPTY);
            ingredients[0] = book;
            ingredients[1] = new ItemStack(Items.WRITABLE_BOOK);
            var input = CraftingInput.of(3, 3, Arrays.asList(ingredients));
            if (!recipe.matches(input, level)) throw new IllegalStateException("vanilla book cloning fixture does not match");
            var result = recipe.assemble(input, level.registryAccess());
            var encoded = AEItems.CRAFTING_PATTERN.stack();
            AECraftingPattern.encode(encoded, new RecipeHolder<>(recipeId, recipe), ingredients, result, false, false);
            var core = placement.owned().stream().map(level::getBlockEntity)
                    .filter(TrinityPatternCoreBlockEntity.class::isInstance)
                    .map(TrinityPatternCoreBlockEntity.class::cast).findFirst()
                    .orElseThrow(() -> new IllegalStateException("formed Trinity crafting child has no mounted pattern core"));
            if (!core.trySetPattern(0, encoded)) throw new IllegalStateException("mounted Trinity pattern core rejected book cloning");
            original = AEItemKey.of(book);
            copy = AEItemKey.of(result);
            if (copy == null || original.equals(copy)) throw new IllegalStateException("book cloning output is not a distinct copy");
            patternInstalled = true;
        }
        var storage = grid.getStorageService().getInventory();
        if (storage.getAvailableStacks().get(original) == 0 &&
                storage.insert(original, 1, Actionable.MODULATE, IActionSource.ofPlayer(player)) != 1)
            throw new IllegalStateException("cannot store retained written book");
        var blanks = storage.getAvailableStacks().get(CONSUMABLE);
        if (blanks < 64 && storage.insert(CONSUMABLE, 64 - blanks, Actionable.MODULATE,
                IActionSource.ofPlayer(player)) != 64 - blanks)
            throw new IllegalStateException("cannot store blank books");
        return !grid.getCraftingService().getCraftingFor(copy).isEmpty();
    }

    @Override protected AEItemKey inputKey() { return CONSUMABLE; }
    @Override protected AEItemKey outputKey() { return copy; }

    @Override
    void configureAmount(appeng.client.gui.me.crafting.CraftAmountScreen screen) {
        ((CraftAmountScreenAccessor) screen).ae2craftingtime_test_driver$amount().setLongValue(3);
    }

    @Override
    String planRequestEvidence(ServerPlayer player) {
        var evidence = super.planRequestEvidence(player);
        if (evidence == null || !nativeCompleted()) return evidence;
        var requests = JsonParser.parseString(evidence).getAsJsonArray();
        if (requests.size() != 2 || !requests.get(0).getAsJsonObject().get("encodedTarget")
                .equals(requests.get(1).getAsJsonObject().get("encodedTarget"))
                || !"NET_NEW".equals(requests.get(0).getAsJsonObject().get("quantityMode").getAsString())
                || !"NET_NEW".equals(requests.get(1).getAsJsonObject().get("quantityMode").getAsString()))
            throw new IllegalStateException("counted collection requires two identical exact NET_NEW requests");
        if (collection == null) collection = collectCompletedCopies(player);
        requests.get(requests.size() - 1).getAsJsonObject()
                .add("completedCopyCollection", new com.google.gson.Gson().toJsonTree(collection));
        return requests.toString();
    }

    private CollectionEvidence collectCompletedCopies(ServerPlayer player) {
        // shortcut: Isolate Data 3.3.5's repeat-Plan balance; restore the in-ME probe when native planning supports it.
        if (jobId == null || link == null || !jobId.equals(link.getCraftingID())
                || !link.isStandalone() || link.isCanceled() || !nativeCompleted())
            throw new IllegalStateException("counted collection has no completed native job identity");
        var grid = submittedGrid;
        if (grid == null) throw new IllegalStateException("counted collection lost its native grid");
        var storage = grid.getStorageService().getInventory();
        var available = storage.getAvailableStacks();
        long copiesBefore = available.get(copy);
        long originalNow = available.get(original);
        long blanksNow = available.get(CONSUMABLE);
        int samplesBefore = ProfilerBridge.stats(ProfilerBridge.key(ProfilerBridge.networkId(grid), copy))
                .map(stats -> stats.sampleCount()).orElse(0);
        if (beforeOutput != 0 || beforeInput != 64 || copiesBefore != 3 || originalNow != 1
                || blanksNow != 61 || samplesBefore != 1 || !nativeCompleted())
            throw new IllegalStateException("counted collection requires frozen three-copy completion and one sample");
        var inventory = player.getInventory();
        int slot = -1;
        for (int candidate = 0; candidate < Math.min(36, inventory.getContainerSize()); candidate++) {
            if (inventory.getItem(candidate).isEmpty()) { slot = candidate; break; }
        }
        if (slot < 0 || copy.toStack(3).getMaxStackSize() < 3)
            throw new IllegalStateException("counted collection has no empty owned slot for three exact copies");
        var source = IActionSource.ofPlayer(player);
        if (storage.extract(copy, 3, Actionable.SIMULATE, source) != 3)
            throw new IllegalStateException("counted collection cannot simulate three exact copies");
        if (!inventory.getItem(slot).isEmpty())
            throw new IllegalStateException("counted collection destination changed before transfer");
        long extracted = storage.extract(copy, 3, Actionable.MODULATE, source);
        if (extracted > 0) inventory.setItem(slot, copy.toStack(Math.toIntExact(extracted)));
        // A partial extraction is retained in the owned slot; the scenario fails rather than inventing the remainder.
        long copiesAfter = storage.getAvailableStacks().get(copy);
        long originalAfter = storage.getAvailableStacks().get(original);
        long blanksAfter = storage.getAvailableStacks().get(CONSUMABLE);
        int destinationAfter = inventory.getItem(slot).getCount();
        if (extracted != 3 || !copy.equals(AEItemKey.of(inventory.getItem(slot)))
                || destinationAfter != 3 || copiesAfter != 0 || originalAfter != originalNow
                || blanksAfter != blanksNow)
            throw new IllegalStateException("counted collection did not conserve the three native products");
        int samplesAfter = ProfilerBridge.stats(ProfilerBridge.key(ProfilerBridge.networkId(grid), copy))
                .map(stats -> stats.sampleCount()).orElse(0);
        if (samplesAfter != samplesBefore)
            throw new IllegalStateException("counted collection changed the learned sample count");
        return new CollectionEvidence(jobId.toString(), copy.toTagGeneric(player.level().registryAccess()).toString(),
                "ME storage", copiesBefore, copiesAfter, "player inventory", slot, 0, destinationAfter,
                extracted, originalNow, originalAfter, blanksNow, blanksAfter, samplesBefore, samplesAfter);
    }

    private record CollectionEvidence(String nativeJobId, String encodedKey, String source,
            long sourceBefore, long sourceAfter, String destination, int destinationSlot,
            long destinationBefore, long destinationAfter, long transferred,
            long originalBefore, long originalAfter, long blankBooksBefore, long blankBooksAfter,
            int samplesBefore, int samplesAfter) {}

    @Override
    protected void startCraft(ServerPlayer player, Placement placement, appeng.menu.me.crafting.CraftConfirmMenu menu) {
        var host = (com.fish_dan_.data_energistics.blockentity.trinity.TrinityDataCoreBlockEntity)
                player.serverLevel().getBlockEntity(placement.origin());
        originalBefore = host.craftingExecutionRoute().serviceGrid().getStorageService()
                .getInventory().getAvailableStacks().get(original);
        if (originalBefore != 1) throw new IllegalStateException("retained original must be present exactly once");
        super.startCraft(player, placement, menu);
    }

    @Override
    protected void verifyVariant(IGrid grid, boolean complete) {
        if (complete && grid.getStorageService().getInventory().getAvailableStacks().get(original) != originalBefore)
            throw new IllegalStateException("native book cloning did not preserve the exact original");
    }

    @Override
    protected boolean nativeCompletion(TrinityNativeCompletion completion, IGrid grid,
            long blanksConsumed, long copiesProduced) {
        long originalNow = grid.getStorageService().getInventory().getAvailableStacks().get(original);
        return new TrinityCountedCompletion(completion.submittedJob(), completion.activeJob(),
                worker == null ? 0 : worker.number(), completion.sameHost(), completion.sameGrid(),
                completion.terminal(), completion.linkDone(), completion.linkStandalone(),
                completion.linkCanceled(), completion.requested(), blanksConsumed, copiesProduced,
                originalBefore, originalNow).complete();
    }

    @Override
    protected Map<String, Long> nativeVariantAmounts(IGrid grid) {
        return Map.of("originalBefore", originalBefore,
                "originalNow", grid.getStorageService().getInventory().getAvailableStacks().get(original),
                "blankBooksConsumed", beforeInput - grid.getStorageService().getInventory().getAvailableStacks().get(inputKey()),
                "copiesProduced", grid.getStorageService().getInventory().getAvailableStacks().get(outputKey()) - beforeOutput);
    }
}
