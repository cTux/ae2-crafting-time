package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.core.definitions.AEBlocks;
import appeng.crafting.CraftingLink;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftAmountMenu;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityDataCoreBlockEntity;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityInformationExchangeDepotBlockEntity;
import com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu;
import com.fish_dan_.data_energistics.menu.crafting.TrinityCraftAmountMenuState;
import com.fish_dan_.data_energistics.common.trinity.autobuild.TrinityAutoBuildOptions;
import com.fish_dan_.data_energistics.common.trinity.autobuild.TrinityAutoBuildRequest;
import com.fish_dan_.data_energistics.common.trinity.autobuild.TrinityAutoBuildBlockMap;
import com.fish_dan_.data_energistics.registry.DEBlocks;
import com.fish_dan_.data_energistics.registry.DEVerticalMultiBlocks;
import com.google.gson.Gson;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Disposable real Trinity main, CPU, crafting child and furnace job. */
class DataEnergisticsTrinityFixture extends AddonCpuFixture<DataEnergisticsTrinityFixture.Placement> {
    private static final AEItemKey INPUT = AEItemKey.of(Items.SAND);
    private static final AEItemKey OUTPUT = AEItemKey.of(Items.GLASS);
    private static final Gson JSON = new Gson();
    protected Placement placement;
    protected TrinityDataCoreVirtualCpu worker;
    protected TrinityDataCoreBlockEntity submittedHost;
    protected IGrid submittedGrid;
    protected CraftingLink link;
    protected UUID jobId;
    private UUID activeJobId;
    protected long requested;
    protected long beforeInput;
    protected long beforeOutput;
    protected BigInteger lastWaiting = BigInteger.ZERO;
    protected BigInteger maxWaiting = BigInteger.ZERO;
    protected BigInteger waitingDecrement = BigInteger.ZERO;
    protected boolean sawAcceptedOutput;
    private boolean sawTerminal;
    private volatile boolean nativeDone;
    private volatile boolean nativeActive;
    private Integer statusWorkerSerial;
    private volatile String coordinatorName;
    private final TrinityActiveStatusFreshness activeStatusFreshness = new TrinityActiveStatusFreshness();
    private final SetupStageProgress setup = new SetupStageProgress();
    private int planBadgeStage;
    private long planBadgeFrame = -1;
    private boolean planBadgeOriginal;
    private volatile boolean activeStatusCaptured;
    private boolean outputHoldArmed;
    private boolean outputHoldReleased;
    private long outputHoldStartedNanos;
    private Long outputHoldDurationNanos;
    private volatile boolean twoGlassBeforeStatus;
    private volatile int furnaceGlassBeforeStatus;
    private boolean suffixBuffered;
    private String bufferedSuffixItem;
    private String bufferedPrefixJobId;
    private int bufferedPrefixGlass;
    private Long bufferedPrefixHoldNanos;
    private final List<NativePlanRequest> planRequests = new ArrayList<>();

    @Override
    AEKey exactOutputKey() { return Objects.requireNonNull(outputKey(), "Trinity exact output key is unavailable"); }

    @Override
    String planRequestEvidence(ServerPlayer player) {
        if (!(player.containerMenu instanceof CraftAmountMenu menu)) return null;
        var request = menu.getWhatToCraft();
        if (request == null) return null;
        var target = request.what();
        var encoded = target.toTagGeneric(player.level().registryAccess()).toString();
        if (!exactOutputKey().equals(target))
            throw new IllegalStateException("Trinity Craft Amount target differs from the exact fixture output: " + encoded);
        var mode = ((TrinityCraftAmountMenuState) menu).data_energistics$quantityMode();
        planRequests.add(new NativePlanRequest(planRequests.size() + 1, target.getId().toString(), encoded,
                mode.name()));
        return JSON.toJson(planRequests);
    }

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        for (int distance : List.of(64, 80, 96)) {
            var x = terminal.getX() + distance;
            var z = terminal.getZ();
            int surface = 0;
            for (int dx = -24; dx <= 24; dx++)
                for (int dz = -24; dz <= 24; dz++)
                    surface = Math.max(surface, level.getHeight(Heightmap.Types.WORLD_SURFACE, x + dx, z + dz));
            var origin = new BlockPos(x, surface + 26, z);
            var provider = origin.above(35);
            var volume = rotatedUnion(origin);
            for (int depth = 0; depth < 4; depth++) volume.add(provider.below(depth));
            if (requiresFurnaceOutputHold()) volume.add(outputHoldPower(provider));
            if (usesPhysicalSuffix()) volume.add(outputSuffixBuffer(provider));
            if (volume.stream().anyMatch(pos -> pos.getY() < level.getMinBuildHeight()
                    || pos.getY() >= level.getMaxBuildHeight())) continue;
            for (int cx = (x - 24) >> 4; cx <= (x + 24) >> 4; cx++)
                for (int cz = (z - 24) >> 4; cz <= (z + 24) >> 4; cz++)
                    level.getChunk(cx, cz);
            if (volume.stream().anyMatch(pos -> !level.getBlockState(pos).isAir())) continue;
            level.setBlockAndUpdate(origin, DEBlocks.TRINITY_DATA_CORE.get().defaultBlockState());
            level.setBlockAndUpdate(provider, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState()
                    .setValue(PatternProviderBlock.PUSH_DIRECTION, PushDirection.DOWN));
            level.setBlockAndUpdate(provider.below(), Blocks.FURNACE.defaultBlockState());
            level.setBlockAndUpdate(provider.below(2), Blocks.HOPPER.defaultBlockState());
            level.setBlockAndUpdate(provider.below(3), AEBlocks.INTERFACE.block().defaultBlockState());
            if (usesPhysicalSuffix()) level.setBlockAndUpdate(outputSuffixBuffer(provider), Blocks.BARREL.defaultBlockState());
            ((FurnaceBlockEntity) level.getBlockEntity(provider.below()))
                    .setItem(1, new ItemStack(Items.COAL, 64));
            ((PatternProviderBlockEntity) level.getBlockEntity(provider)).getLogic().getPatternInv()
                    .setItemDirect(0, PatternDetailsHelper.encodeProcessingPattern(
                            List.of(new GenericStack(INPUT, 1)), List.of(new GenericStack(OUTPUT, 1))));
            ((PatternProviderBlockEntity) level.getBlockEntity(provider)).getLogic().updatePatterns();
            placement = new Placement(origin, terminal, provider, volume);
            setup.observe("placed", System.nanoTime());
            return placement;
        }
        throw new IllegalStateException("no empty loaded volume for Trinity main and child structures");
    }

    // Shipped main offsets are left [-13,13], up [-2,25], front [-24,7]; both
    // child layouts stay inside the checked bounds for every horizontal facing.
    protected static Set<BlockPos> rotatedUnion(BlockPos origin) {
        var positions = new HashSet<BlockPos>();
        for (var front : Direction.Plane.HORIZONTAL)
            for (int x = -13; x <= 13; x++)
                for (int z = -24; z <= 24; z++)
                    for (int y = -25; y <= 25; y++)
                        positions.add(origin.relative(front.getClockWise(), x).relative(front, z).above(y));
        return positions;
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        var host = (TrinityDataCoreBlockEntity) level.getBlockEntity(placement.origin());
        if (host == null || !host.getMainNode().isReady()) return await("host-node");
        if (!host.isStructureFormed()) {
            if (!placement.mainRequested) {
                host.autoBuildTrinityStructure(player, request(TrinityAutoBuildRequest.MAIN_STRUCTURE_INDEX));
                placement.mainRequested = true;
            }
            return await("main-formation");
        }
        var terminalHost = (IInWorldGridNodeHost) level.getBlockEntity(placement.terminal());
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (terminalNode == null) return await("terminal-node");
        var depot = host.compartmentHost$getCompartments(DEVerticalMultiBlocks.trinityDataCoreMainKey().structureName()).stream()
                .filter(TrinityInformationExchangeDepotBlockEntity.class::isInstance)
                .map(TrinityInformationExchangeDepotBlockEntity.class::cast)
                .findFirst().orElse(null);
        if (depot == null || depot.getGridNode() == null) return await("information-exchange-depot");
        if (depot.getGridNode().getGrid() != terminalNode.getGrid())
            GridHelper.createConnection(terminalNode, depot.getGridNode());
        if (!host.isCpuStructureFormed()) {
            if (!placement.cpuRequested) {
                host.autoBuildTrinityStructure(player, request(TrinityAutoBuildRequest.CPU_STRUCTURE_INDEX));
                placement.cpuRequested = true;
            }
            return await("cpu-formation");
        }
        if (!host.isCraftingStructureFormed()) {
            if (!placement.craftingRequested) {
                host.autoBuildTrinityStructure(player, request(TrinityAutoBuildRequest.CRAFTING_STRUCTURE_INDEX));
                placement.craftingRequested = true;
            }
            return await("crafting-formation");
        }
        if (host.getCraftingPatternCoreCount() < 1)
            throw new IllegalStateException("Trinity crafting child has no mounted pattern core");
        for (var pos : List.of(placement.provider(), placement.provider().below(3))) {
            var machine = (AENetworkedBlockEntity) level.getBlockEntity(pos);
            if (machine == null || !machine.getMainNode().isReady()) return await("provider-node");
            var node = machine.getMainNode().getNode();
            if (node == null) return await("provider-grid-node");
            if (node.getGrid() != terminalNode.getGrid()) GridHelper.createConnection(terminalNode, node);
        }
        if (host.craftingExecutionRoute() == null || host.craftingExecutionRoute().serviceGrid() != terminalNode.getGrid())
            return await("crafting-route");
        var grid = terminalNode.getGrid();
        var storage = grid.getStorageService().getInventory();
        long present = storage.getAvailableStacks().get(INPUT);
        if (present < 64 && storage.insert(INPUT, 64 - present, Actionable.MODULATE,
                IActionSource.ofPlayer(player)) != 64 - present)
            throw new IllegalStateException("Trinity fixture cannot store sand input");
        var provider = (PatternProviderBlockEntity) level.getBlockEntity(placement.provider());
        var patterns = provider.getLogic().getAvailablePatterns();
        var registered = grid.getCraftingService().getCraftingFor(OUTPUT);
        if (registered.isEmpty()) return await("glass-pattern");
        if (patterns.size() != 1 || registered.size() != 1 || registered.iterator().next() != patterns.getFirst())
            throw new IllegalStateException("Trinity fixture provider is not the unique glass pattern source");
        if (!host.getCpuPartitions().stream().anyMatch(cpu -> cpu.number() == 0 && cpu.isActive()
                && grid.getCraftingService().getCpus().contains(cpu))) return await("coordinator-published");
        if (!finishVariant(player, grid, placement)) return await("variant-pattern");
        setup.observe("ready", System.nanoTime());
        return true;
    }

    protected boolean finishVariant(ServerPlayer player, IGrid grid, Placement placement) { return true; }

    protected AEItemKey inputKey() { return INPUT; }

    protected AEItemKey outputKey() { return OUTPUT; }

    protected void verifyVariant(IGrid grid, boolean complete) {}

    protected boolean nativeCompletion(TrinityNativeCompletion completion, IGrid grid,
            long inputDelta, long outputDelta) { return completion.complete(); }

    protected Map<String, Long> nativeVariantAmounts(IGrid grid) { return Map.of(); }

    protected boolean requiresImmediateWorkerCapture() { return false; }

    protected boolean requiresFurnaceOutputHold() { return true; }

    protected boolean usesPhysicalSuffix() { return false; }

    protected static BlockPos outputHoldPower(BlockPos provider) {
        return provider.below(2).east();
    }

    protected static BlockPos outputSuffixBuffer(BlockPos provider) {
        return provider.below(2).east(2);
    }

    protected final BarrelBlockEntity suffixBuffer(ServerPlayer player) {
        return suffixBuffer(player.serverLevel());
    }

    protected final BarrelBlockEntity suffixBuffer(net.minecraft.server.level.ServerLevel level) {
        if (placement == null || !placement.owned().contains(outputSuffixBuffer(placement.provider())))
            throw new IllegalStateException("Trinity physical suffix buffer is outside the owned fixture");
        var barrel = level.getBlockEntity(outputSuffixBuffer(placement.provider()));
        if (!(barrel instanceof BarrelBlockEntity result))
            throw new IllegalStateException("Trinity physical suffix buffer is missing");
        return result;
    }

    private static int amount(ItemStack stack, net.minecraft.world.item.Item expected) {
        if (stack.isEmpty()) return 0;
        if (!stack.is(expected)) throw new IllegalStateException("unexpected item in Trinity furnace suffix");
        return stack.getCount();
    }

    private static void requireUnusedBufferSlotsEmpty(BarrelBlockEntity barrel) {
        for (int slot = 1; slot < barrel.getContainerSize(); slot++)
            if (!barrel.getItem(slot).isEmpty())
                throw new IllegalStateException("Trinity physical suffix buffer contains unrelated items");
    }

    private void bufferOnePhysicalSuffix(ServerPlayer player) {
        var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(placement.provider().below());
        var barrel = suffixBuffer(player);
        requireUnusedBufferSlotsEmpty(barrel);
        int sand = amount(furnace.getItem(0), Items.SAND);
        int glass = amount(furnace.getItem(2), Items.GLASS);
        if (!barrel.getItem(0).isEmpty())
            throw new IllegalStateException("Trinity suffix buffer was occupied before release");
        var source = TrinityPhysicalSuffix.source(sand, glass, 0);
        int slot = source == TrinityPhysicalSuffix.Source.OUTPUT ? 2 : 0;
        var realStack = furnace.removeItem(slot, 1);
        if (realStack.getCount() != 1)
            throw new IllegalStateException("Trinity furnace could not transfer its physical suffix");
        barrel.setItem(0, realStack);
        furnace.setChanged();
        barrel.setChanged();
        suffixBuffered = true;
        bufferedSuffixItem = source == TrinityPhysicalSuffix.Source.OUTPUT ? "minecraft:glass" : "minecraft:sand";
    }

    protected final void restorePhysicalSuffix(ServerPlayer player) {
        if (!suffixBuffered) throw new IllegalStateException("Trinity has no physical suffix to restore");
        var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(placement.provider().below());
        var barrel = suffixBuffer(player);
        requireUnusedBufferSlotsEmpty(barrel);
        var realStack = barrel.getItem(0).copy();
        if (realStack.getCount() != 1 || !furnace.getItem(0).isEmpty() || !furnace.getItem(2).isEmpty())
            throw new IllegalStateException("Trinity physical suffix changed before restoration");
        int slot = "minecraft:glass".equals(bufferedSuffixItem) ? 2 : 0;
        if (!realStack.is(slot == 2 ? Items.GLASS : Items.SAND))
            throw new IllegalStateException("Trinity physical suffix item changed before restoration");
        barrel.setItem(0, ItemStack.EMPTY);
        furnace.setItem(slot, realStack);
        barrel.setChanged();
        furnace.setChanged();
        suffixBuffered = false;
        bufferedSuffixItem = null;
    }

    protected final void discardCancelledPhysicalSuffix(ServerPlayer player) {
        if (!suffixBuffered) throw new IllegalStateException("cancelled Trinity job has no held suffix");
        var barrel = suffixBuffer(player);
        requireUnusedBufferSlotsEmpty(barrel);
        if (barrel.getItem(0).getCount() != 1)
            throw new IllegalStateException("cancelled Trinity suffix changed before isolation");
        barrel.setItem(0, ItemStack.EMPTY);
        barrel.setChanged();
        suffixBuffered = false;
        bufferedSuffixItem = null;
    }

    protected final int bufferedSand(ServerPlayer player) {
        return bufferedSand(player.serverLevel());
    }

    protected final int bufferedSand(net.minecraft.server.level.ServerLevel level) {
        var stack = suffixBuffer(level).getItem(0);
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.SAND)) return stack.getCount();
        if (stack.is(Items.GLASS)) return 0;
        throw new IllegalStateException("unexpected item in Trinity physical suffix buffer");
    }

    protected final int bufferedGlass(ServerPlayer player) {
        return bufferedGlass(player.serverLevel());
    }

    protected final int bufferedGlass(net.minecraft.server.level.ServerLevel level) {
        var stack = suffixBuffer(level).getItem(0);
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.GLASS)) return stack.getCount();
        if (stack.is(Items.SAND)) return 0;
        throw new IllegalStateException("unexpected item in Trinity physical suffix buffer");
    }

    protected final boolean twoGlassBeforeStatus() { return twoGlassBeforeStatus; }

    protected final boolean physicalSuffixBuffered() { return suffixBuffered; }

    protected final long bufferedPrefixHoldNanos() {
        if (bufferedPrefixHoldNanos == null || bufferedPrefixHoldNanos <= 0)
            throw new IllegalStateException("Trinity physical prefix hold has no measured duration");
        return bufferedPrefixHoldNanos;
    }

    protected final void retainBufferedPrefix(String job, int furnaceGlass, long holdNanos) {
        if (job == null || furnaceGlass != 2 || holdNanos <= 0)
            throw new IllegalStateException("restored Trinity physical prefix proof is invalid");
        bufferedPrefixJobId = job;
        bufferedPrefixGlass = furnaceGlass;
        bufferedPrefixHoldNanos = holdNanos;
    }

    protected final void adoptPhysicalSuffix(ServerPlayer player, String itemId) {
        if (!"minecraft:sand".equals(itemId) && !"minecraft:glass".equals(itemId))
            throw new IllegalStateException("invalid restored Trinity suffix item");
        requireUnusedBufferSlotsEmpty(suffixBuffer(player));
        if (bufferedSand(player) + bufferedGlass(player) != 1
                || !(itemId.equals("minecraft:sand") ? bufferedSand(player) == 1 : bufferedGlass(player) == 1))
            throw new IllegalStateException("restored Trinity suffix differs from the physical barrel");
        suffixBuffered = true;
        bufferedSuffixItem = itemId;
    }

    protected final void adoptOutputHold(ServerPlayer player) {
        var power = outputHoldPower(placement.provider());
        var hopper = placement.provider().below(2);
        if (!placement.owned().contains(power) || !player.serverLevel().getBlockState(power).is(Blocks.REDSTONE_BLOCK)
                || !player.serverLevel().getBlockState(hopper).is(Blocks.HOPPER)
                || player.serverLevel().getBlockState(hopper).getValue(HopperBlock.ENABLED))
            throw new IllegalStateException("restored Trinity output hopper is not physically held");
        outputHoldArmed = true;
        outputHoldReleased = false;
        activeStatusCaptured = false;
        activeStatusFreshness.reset();
        outputHoldStartedNanos = System.nanoTime();
        outputHoldDurationNanos = null;
    }

    protected final void armOutputHold(ServerPlayer player, Placement placement) {
        armOutputHold(player.serverLevel(), placement);
    }

    protected final void armOutputHold(net.minecraft.server.level.ServerLevel level, Placement placement) {
        if (!requiresFurnaceOutputHold()) return;
        if (outputHoldArmed && !outputHoldReleased)
            throw new IllegalStateException("previous Trinity furnace output hold was not released");
        var power = outputHoldPower(placement.provider());
        var hopper = placement.provider().below(2);
        if (!placement.owned().contains(power) || power.getY() < level.getMinBuildHeight()
                || power.getY() >= level.getMaxBuildHeight() || !level.getBlockState(power).isAir()
                || !level.getBlockState(hopper).is(Blocks.HOPPER)
                || !level.getBlockState(hopper).getValue(HopperBlock.ENABLED))
            throw new IllegalStateException("Trinity furnace output hold has no empty enabled hopper");
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        if (!level.getBlockState(power).is(Blocks.REDSTONE_BLOCK)
                || level.getBlockState(hopper).getValue(HopperBlock.ENABLED))
            throw new IllegalStateException("redstone did not disable the owned Trinity output hopper");
        outputHoldArmed = true;
        outputHoldReleased = false;
        outputHoldStartedNanos = System.nanoTime();
        outputHoldDurationNanos = null;
    }

    private void releaseOutputHold(ServerPlayer player) {
        if (!outputHoldArmed || outputHoldReleased)
            throw new IllegalStateException("Trinity furnace output hold released more than once");
        var level = player.serverLevel();
        var power = outputHoldPower(placement.provider());
        var hopper = placement.provider().below(2);
        if (!level.getBlockState(power).is(Blocks.REDSTONE_BLOCK)
                || !level.getBlockState(hopper).is(Blocks.HOPPER)
                || level.getBlockState(hopper).getValue(HopperBlock.ENABLED))
            throw new IllegalStateException("Trinity furnace output hold changed before native Status capture");
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        if (!level.getBlockState(power).isAir()
                || !level.getBlockState(hopper).getValue(HopperBlock.ENABLED))
            throw new IllegalStateException("Trinity output hopper stayed disabled after release");
        outputHoldReleased = true;
        outputHoldDurationNanos = System.nanoTime() - outputHoldStartedNanos;
    }

    @Override
    protected String setupDetail() { return setup.stage(); }

    private boolean await(String stage) {
        return setup.observe(stage, System.nanoTime());
    }

    private static TrinityAutoBuildRequest request(int index) {
        var tiers = new Object2IntOpenHashMap<String>();
        tiers.put(TrinityAutoBuildBlockMap.categoryForStructure(index), 1);
        return new TrinityAutoBuildRequest(index, new TrinityAutoBuildOptions(true, 1, tiers));
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        var host = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(placement.origin());
        if (host == null || host.craftingExecutionRoute() == null
                || host.craftingExecutionRoute().serviceGrid() != grid) return null;
        return host.getCpuPartitions().stream()
                .filter(cpu -> cpu.number() == 0 && cpu.isActive() && !cpu.isBusy()
                        && grid.getCraftingService().getCpus().contains(cpu))
                .findFirst().orElse(null);
    }

    @Override
    protected void startCraft(ServerPlayer player, Placement placement, CraftConfirmMenu menu) {
        worker = null;
        link = null;
        jobId = null;
        activeJobId = null;
        expectedResumeJobId = null;
        nativeActive = false;
        nativeDone = false;
        sawTerminal = false;
        sawAcceptedOutput = false;
        maxWaiting = BigInteger.ZERO;
        lastWaiting = BigInteger.ZERO;
        waitingDecrement = BigInteger.ZERO;
        statusWorkerSerial = null;
        coordinatorName = null;
        activeStatusFreshness.reset();
        activeStatusCaptured = false;
        twoGlassBeforeStatus = false;
        furnaceGlassBeforeStatus = 0;
        if (suffixBuffered || usesPhysicalSuffix() && !suffixBuffer(player).getItem(0).isEmpty())
            throw new IllegalStateException("previous Trinity physical suffix was not cleared before the new job");
        submittedHost = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(placement.origin());
        var grid = Objects.requireNonNull(submittedHost.craftingExecutionRoute(),
                "Trinity worker has no crafting route").serviceGrid();
        if (submittedHost.getCpuPartitions().stream().anyMatch(TrinityDataCoreVirtualCpu::isBusy))
            throw new IllegalStateException("Trinity fixture host already has a busy worker");
        var output = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result().finalOutput();
        if (!outputKey().equals(output.what()) || output.amount() <= 0)
            throw new IllegalStateException("Trinity fixture plan has unexpected final output: " + output);
        requested = output.amount();
        submittedGrid = grid;
        beforeInput = grid.getStorageService().getInventory().getAvailableStacks().get(inputKey());
        beforeOutput = grid.getStorageService().getInventory().getAvailableStacks().get(outputKey());
        armOutputHold(player, placement);
        super.startCraft(player, placement, menu);
        captureSubmittedWorker(submittedHost, grid);
        if (requiresImmediateWorkerCapture() && worker == null)
            throw new IllegalStateException("synchronous native Trinity submission has no capturable worker/job");
    }

    private void captureSubmittedWorker(TrinityDataCoreBlockEntity host, IGrid grid) {
        if (worker != null) return;
        worker = host.getCpuPartitions().stream()
                .filter(cpu -> cpu.number() > 0 && cpu.isBusy()
                        && grid.getCraftingService().getCpus().contains(cpu))
                .findFirst().orElse(null);
        if (worker == null) return;
        link = jobLink(worker);
        jobId = link.getCraftingID();
        if (expectedResumeJobId != null && !expectedResumeJobId.equals(jobId))
            throw new IllegalStateException("restored Trinity worker has a different native job UUID");
        activeJobId = jobId;
    }

    @Override
    String nativeEvidence(ServerPlayer player, IGrid observedMenuGrid) {
        var host = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(
                placement.origin());
        if (host == null || host.craftingExecutionRoute() == null)
            throw new IllegalStateException("retained Trinity host lost its native crafting route");
        var grid = host.craftingExecutionRoute().serviceGrid();
        if (grid == null || grid != submittedGrid
                || observedMenuGrid != null && observedMenuGrid != grid)
            throw new IllegalStateException("retained Trinity job changed its native service grid");
        var coordinator = host.getCpuPartitions().stream().filter(cpu -> cpu.number() == 0
                && grid.getCraftingService().getCpus().contains(cpu)).toList();
        if (coordinator.size() != 1 || coordinator.getFirst().getName() == null)
            throw new IllegalStateException("retained Trinity coordinator is not uniquely published");
        coordinatorName = coordinator.getFirst().getName().getString();
        captureSubmittedWorker(host, grid);
        if (worker == null) return null;
        if (usesPhysicalSuffix() && !suffixBuffered && !twoGlassBeforeStatus) {
            var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(placement.provider().below());
            furnaceGlassBeforeStatus = amount(furnace.getItem(2), Items.GLASS);
            twoGlassBeforeStatus = TrinityPhysicalSuffix.twoGlassReady(
                    furnaceGlassBeforeStatus, suffixBuffer(player).getItem(0).getCount());
        }
        if (activeStatusCaptured && outputHoldArmed && !outputHoldReleased) {
            if (!worker.isBusy() || !jobId.equals(jobLink(worker).getCraftingID()))
                throw new IllegalStateException("Trinity job changed before held output was released");
            if (usesPhysicalSuffix() && !suffixBuffered) {
                if (!twoGlassBeforeStatus)
                    throw new IllegalStateException("two physical glass outputs were not observed before Status capture");
                bufferOnePhysicalSuffix(player);
            } else if (suffixBuffered) {
                restorePhysicalSuffix(player);
            }
            releaseOutputHold(player);
            if (suffixBuffered && twoGlassBeforeStatus) {
                bufferedPrefixJobId = jobId.toString();
                bufferedPrefixGlass = furnaceGlassBeforeStatus;
                bufferedPrefixHoldNanos = outputHoldDurationNanos;
            }
        }
        if (worker.isBusy()) activeJobId = jobLink(worker).getCraftingID();
        nativeActive = worker.isBusy();
        var waiting = worker.getWaitingFor(outputKey());
        if (waiting.compareTo(maxWaiting) > 0) maxWaiting = waiting;
        if (lastWaiting.compareTo(waiting) > 0) {
            waitingDecrement = waitingDecrement.add(lastWaiting.subtract(waiting));
            sawAcceptedOutput = true;
        }
        lastWaiting = waiting;
        if (!worker.isBusy() && worker.getJobStatus() == null) sawTerminal = true;
        long input = grid.getStorageService().getInventory().getAvailableStacks().get(inputKey());
        long output = grid.getStorageService().getInventory().getAvailableStacks().get(outputKey());
        var completion = new TrinityNativeCompletion(jobId, activeJobId,
                host == submittedHost, grid == submittedGrid && host.craftingExecutionRoute() != null
                        && host.craftingExecutionRoute().serviceGrid() == grid,
                sawTerminal, link.isDone(), link.isStandalone(), link.isCanceled(), requested,
                beforeInput - input, output - beforeOutput, maxWaiting, waitingDecrement);
        nativeDone = nativeCompletion(completion, grid, beforeInput - input, output - beforeOutput);
        if (nativeDone && requiresFurnaceOutputHold() && (!outputHoldArmed || !outputHoldReleased))
            throw new IllegalStateException("Trinity furnace job completed without a released output hold");
        verifyVariant(grid, nativeDone);
        return JSON.toJson(new NativeObservation(jobId.toString(), activeJobId.toString(),
                worker.number(), worker.getStableDispatchIdentity(), requested,
                placement.origin().offset(-24, -25, -24).toShortString(),
                placement.origin().offset(24, 25, 24).toShortString(),
                placement.provider().toShortString(),
                host.isStructureFormed(), host.isCpuStructureFormed(), host.isCraftingStructureFormed(),
                maxWaiting.toString(), waiting.toString(), waitingDecrement.toString(), sawAcceptedOutput,
                completion.sameHost(), completion.sameGrid(), sawTerminal,
                link.isDone(), link.isStandalone(), link.isCanceled(), nativeDone,
                beforeInput, input, beforeInput - input, beforeOutput, output,
                output - beforeOutput, outputHoldArmed, outputHoldReleased,
                outputHoldDurationNanos, twoGlassBeforeStatus, furnaceGlassBeforeStatus, suffixBuffered,
                bufferedSuffixItem, bufferedPrefixJobId, bufferedPrefixGlass,
                bufferedPrefixHoldNanos, nativeVariantAmounts(grid), activeStatusFreshness.accepted()));
    }

    @Override
    boolean nativeCompleted() {
        return nativeDone;
    }

    @Override boolean nativeActive() { return nativeActive; }

    @Override
    boolean selectCpuOnClient(net.minecraft.client.Minecraft minecraft) {
        if (!(minecraft.player.containerMenu instanceof CraftConfirmMenu menu))
            throw new IllegalStateException("native Trinity confirmation menu is unavailable");
        menu.cycleSelectedCPU(true);
        return true;
    }

    @Override boolean requiresNativeCompletion() { return true; }

    @Override int expectedSampleCount(boolean freshJob) { return freshJob ? 2 : 1; }

    @Override String nativeJobId() { return jobId == null ? null : jobId.toString(); }

    @Override
    boolean visibleCpuSelection(net.minecraft.client.Minecraft minecraft, String outputId,
            long priorFrame, Consumer<String> screenshot) {
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.frame() == priorFrame || snapshot.text().stream().noneMatch(text ->
                text.key().equals("native-trinity-cpu-selection") && text.rendered().contains("Trinity"))) return false;
        screenshot.accept("trinity-selected-cpu-plan.png");
        return true;
    }

    @Override
    boolean observePlanPresentation(net.minecraft.client.Minecraft minecraft, long frame,
            Consumer<String> screenshot) {
        var features = ClientOptionsRuntime.current().features();
        if (planBadgeStage == 0) {
            planBadgeOriginal = features.enabled(OptionFeature.BADGE_BACKGROUND);
            features.setEnabled(OptionFeature.BADGE_BACKGROUND, false);
            planBadgeFrame = frame;
            planBadgeStage = 1;
            return false;
        }
        if (frame <= planBadgeFrame) return false;
        if (planBadgeStage == 1) {
            screenshot.accept("trinity-plan-badge-off.png");
            features.setEnabled(OptionFeature.BADGE_BACKGROUND, true);
            planBadgeFrame = frame;
            planBadgeStage = 2;
            return false;
        }
        if (planBadgeStage == 2) {
            screenshot.accept("trinity-plan-badge-on.png");
            cleanupClient();
            planBadgeStage = 3;
        }
        return true;
    }

    @Override void cleanupClient() {
        if (planBadgeStage > 0)
            ClientOptionsRuntime.current().features().setEnabled(
                    OptionFeature.BADGE_BACKGROUND, planBadgeOriginal);
    }

    @Override
    boolean observeNativeStatus(net.minecraft.client.Minecraft minecraft, String outputId,
            boolean completed, long priorFrame, Consumer<String> screenshot) {
        if (!completed && usesPhysicalSuffix() && !suffixBuffered && !twoGlassBeforeStatus) return false;
        if (minecraft.screen instanceof MEStorageScreen<?> storage) {
            var button = ((MEStorageScreenAccessor) storage).ae2craftingtime_test_driver$statusButton();
            DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
            return false;
        }
        if (!(minecraft.screen instanceof CraftingCPUScreen<?>)) return false;
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.frame() <= priorFrame) return false;
        if (completed) {
            if (statusWorkerSerial == null || snapshot.cpuCards().stream()
                    .anyMatch(value -> value.serial() == statusWorkerSerial)) return false;
            var coordinator = TrinityStatusCoordinator.card(snapshot.cpuCards(), coordinatorName);
            if (coordinator == null || coordinator.jobId() != null) return false;
            if (!coordinator.selected()) {
                DriverPlatform.click(minecraft, coordinator.bounds().centerX(), coordinator.bounds().centerY());
                return false;
            }
            var status = snapshot.status();
            if (status != null && (!"native".equals(status.provenance())
                    || status.selectedJobUuid() != null || status.selectedCpuSerial() == null
                    || status.selectedCpuSerial() != coordinator.serial()
                    || Objects.equals(status.identity(), activeStatusFreshness.baselineIdentity()))) return false;
            screenshot.accept("trinity-completed-status.png");
        } else {
            var card = snapshot.cpuCards().stream()
                    .filter(value -> outputId.equals(value.jobId()) && value.amount() > 0)
                    .findFirst().orElse(null);
            if (card == null) return false;
            if (!card.selected()) {
                DriverPlatform.click(minecraft, card.bounds().centerX(), card.bounds().centerY());
                return false;
            }
            var status = snapshot.status();
            if (!activeStatusFreshness.observe(status, card.selected(), card.serial(),
                    nativeJobId(), outputId, snapshot.frame())) return false;
            if (requiresLearnedActiveStatus() && (status.startCount() <= 0 || status.remainingCount() <= 0
                    || snapshot.rows().stream().filter(row -> outputId.equals(row.outputId()))
                            .filter(row -> row.activeAmount() > 0 || row.pendingAmount() > 0)
                            .flatMap(row -> row.description().stream())
                            .noneMatch(text -> text.key().equals("text.ae2craftingtime.ttc")
                                    && !text.arguments().contains("text.ae2craftingtime.collecting_data")))) return false;
            statusWorkerSerial = card.serial();
            screenshot.accept("trinity-active-status.png");
            activeStatusCaptured = true;
        }
        minecraft.setScreen(null);
        return true;
    }

    protected boolean requiresLearnedActiveStatus() { return false; }

    @Override
    String confirmScreenName() {
        return TRINITY_CONFIRM_SCREEN;
    }

    @Override
    boolean isConfirmScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen != null && screen.getClass().getName().equals(confirmScreenName());
    }

    @Override
    boolean requiresPlanRows() { return false; }

    protected UUID expectedResumeJobId;

    protected static CraftingLink jobLink(TrinityDataCoreVirtualCpu worker) {
        try {
            var logicField = worker.getClass().getDeclaredField("logic");
            logicField.setAccessible(true);
            var logic = logicField.get(worker);
            var jobField = logic.getClass().getDeclaredField("job");
            jobField.setAccessible(true);
            var job = Objects.requireNonNull(jobField.get(logic), "Trinity worker has no accepted job");
            Field linkField = job.getClass().getDeclaredField("link");
            linkField.setAccessible(true);
            return (CraftingLink) linkField.get(job);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("cannot read native Trinity worker job identity", error);
        }
    }

    @Override
    protected String outputId(Placement placement, FixtureMarker marker) {
        return outputKey().getId().toString();
    }

    @Override
    protected void cleanup(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        if (placement.owned().contains(outputSuffixBuffer(placement.provider()))
                && level.getBlockEntity(outputSuffixBuffer(placement.provider())) instanceof BarrelBlockEntity barrel) {
            for (int slot = 0; slot < barrel.getContainerSize(); slot++) barrel.setItem(slot, ItemStack.EMPTY);
            barrel.setChanged();
        }
        for (var pos : placement.owned()) {
            if (!level.getBlockState(pos).isAir())
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private record NativeObservation(String submittedJobId, String activeJobId,
            int workerNumber, String workerIdentity, long requestedOutput,
            String checkedStructureMin, String checkedStructureMax, String checkedProvider,
            boolean mainFormed, boolean cpuFormed, boolean craftingFormed, String peakWaiting,
            String currentWaiting, String waitingDecrement, boolean acceptedOutput,
            boolean sameHost, boolean sameGrid, boolean terminal,
            boolean linkDone, boolean linkStandalone, boolean linkCanceled, boolean complete,
            long inputBefore, long inputNow, long dispatchedInput,
            long outputBefore, long outputNow, long networkOutputDelta,
            boolean outputHoldArmed, boolean outputHoldReleased, Long outputHoldDurationNanos,
            boolean twoGlassBeforeStatus, int furnaceGlassBeforeStatus,
            boolean physicalSuffixBuffered, String bufferedSuffixItem,
            String bufferedPrefixJobId, int bufferedPrefixGlass, Long bufferedPrefixHoldNanos,
            Map<String, Long> variantAmounts,
            TrinityActiveStatusFreshness.Evidence activeStatusFreshness) {}

    private record NativePlanRequest(int ordinal, String targetId, String encodedTarget, String quantityMode) {}

    static final class Placement {
        private final BlockPos origin;
        private final BlockPos terminal;
        private final BlockPos provider;
        private final Set<BlockPos> owned;
        private boolean mainRequested;
        private boolean cpuRequested;
        private boolean craftingRequested;

        Placement(BlockPos origin, BlockPos terminal, BlockPos provider, Set<BlockPos> owned) {
            this.origin = origin;
            this.terminal = terminal;
            this.provider = provider;
            this.owned = owned;
        }

        BlockPos origin() { return origin; }
        BlockPos terminal() { return terminal; }
        BlockPos provider() { return provider; }
        Set<BlockPos> owned() { return owned; }
    }
}
