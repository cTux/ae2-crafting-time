package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.core.definitions.AEBlocks;
import appeng.crafting.CraftingLink;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityDataCoreBlockEntity;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityInformationExchangeDepotBlockEntity;
import com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu;
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
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Disposable real Trinity main, CPU, crafting child and furnace job. */
final class DataEnergisticsTrinityFixture extends AddonCpuFixture<DataEnergisticsTrinityFixture.Placement> {
    private static final AEItemKey INPUT = AEItemKey.of(Items.SAND);
    private static final AEItemKey OUTPUT = AEItemKey.of(Items.GLASS);
    private static final Gson JSON = new Gson();
    private Placement placement;
    private TrinityDataCoreVirtualCpu worker;
    private TrinityDataCoreBlockEntity submittedHost;
    private IGrid submittedGrid;
    private CraftingLink link;
    private UUID jobId;
    private UUID activeJobId;
    private long requested;
    private long beforeInput;
    private long beforeOutput;
    private BigInteger lastWaiting = BigInteger.ZERO;
    private BigInteger maxWaiting = BigInteger.ZERO;
    private BigInteger waitingDecrement = BigInteger.ZERO;
    private boolean sawAcceptedOutput;
    private boolean sawTerminal;
    private volatile boolean nativeDone;
    private final SetupStageProgress setup = new SetupStageProgress();

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
    private static Set<BlockPos> rotatedUnion(BlockPos origin) {
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
        setup.observe("ready", System.nanoTime());
        return true;
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
        submittedHost = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(placement.origin());
        var grid = Objects.requireNonNull(submittedHost.craftingExecutionRoute(),
                "Trinity worker has no crafting route").serviceGrid();
        if (submittedHost.getCpuPartitions().stream().anyMatch(TrinityDataCoreVirtualCpu::isBusy))
            throw new IllegalStateException("Trinity fixture host already has a busy worker");
        var output = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result().finalOutput();
        if (!OUTPUT.equals(output.what()) || output.amount() <= 0)
            throw new IllegalStateException("Trinity fixture plan has unexpected final output: " + output);
        requested = output.amount();
        submittedGrid = grid;
        beforeInput = grid.getStorageService().getInventory().getAvailableStacks().get(INPUT);
        beforeOutput = grid.getStorageService().getInventory().getAvailableStacks().get(OUTPUT);
        super.startCraft(player, placement, menu);
    }

    @Override
    String nativeEvidence(ServerPlayer player, IGrid grid) {
        var host = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(
                placement.origin());
        if (worker == null) {
            worker = host.getCpuPartitions().stream()
                    .filter(cpu -> cpu.number() > 0 && cpu.isBusy() && cpu.getJobStatus() != null
                            && OUTPUT.equals(cpu.getJobStatus().crafting().what())
                            && cpu.getJobStatus().crafting().amount() == requested
                            && grid.getCraftingService().getCpus().contains(cpu))
                    .findFirst().orElse(null);
            if (worker != null) {
                link = jobLink(worker);
                jobId = link.getCraftingID();
                activeJobId = jobId;
            }
        }
        if (worker == null) return null;
        if (worker.isBusy()) activeJobId = jobLink(worker).getCraftingID();
        var waiting = worker.getWaitingFor(OUTPUT);
        if (waiting.compareTo(maxWaiting) > 0) maxWaiting = waiting;
        if (lastWaiting.compareTo(waiting) > 0) {
            waitingDecrement = waitingDecrement.add(lastWaiting.subtract(waiting));
            sawAcceptedOutput = true;
        }
        lastWaiting = waiting;
        if (!worker.isBusy() && worker.getJobStatus() == null) sawTerminal = true;
        long input = grid.getStorageService().getInventory().getAvailableStacks().get(INPUT);
        long output = grid.getStorageService().getInventory().getAvailableStacks().get(OUTPUT);
        var completion = new TrinityNativeCompletion(jobId, activeJobId,
                host == submittedHost, grid == submittedGrid && host.craftingExecutionRoute() != null
                        && host.craftingExecutionRoute().serviceGrid() == grid,
                sawTerminal, link.isDone(), link.isStandalone(), link.isCanceled(), requested,
                beforeInput - input, output - beforeOutput, maxWaiting, waitingDecrement);
        nativeDone = completion.complete();
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
                output - beforeOutput));
    }

    @Override
    boolean nativeCompleted() {
        return nativeDone;
    }

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

    private static CraftingLink jobLink(TrinityDataCoreVirtualCpu worker) {
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
        return "minecraft:glass";
    }

    @Override
    protected void cleanup(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
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
            long outputBefore, long outputNow, long networkOutputDelta) {}

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
