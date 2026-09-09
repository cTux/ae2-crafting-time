package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEParts;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

/** Two actual vanilla smelters; the fixture supplies fuel and imports their output. */
final class StandardCraftFixture {
    BlockPos terminal;
    private boolean initialized;
    boolean returnedStone;
    boolean holdFinalOutput;
    boolean missingPlanInput;
    boolean unprofiledPlan;
    boolean cpuListScenario;
    private java.util.List<java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan>> cpuListPlans;
    private boolean cpuListSubmitted;
    private java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan> replacementPlan;
    private boolean replacementSubmitted;
    /** Stable identities survive removal so later actions never reinterpret shifted fixture slots. */
    private java.util.List<CraftingBlockEntity> cpuListIdentities;
    private int sampleMultiplier = 1;
    private boolean restarting;
    private int originShift;
    private int[] initialSamples;
    String checkpoint = "new";

    void bindTerminal(BlockPos value) { terminal = value; }
    void refreshCpuIdentities() { cpuListIdentities = null; }

    boolean prepare(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        if (terminal == null) {
            checkpoint = "placing";
            terminal = new BlockPos(marker.terminal().x() + 60, marker.terminal().y(), marker.terminal().z() + originShift);
            for (var pos : BlockPos.betweenClosed(terminal.offset(cpuListScenario ? -16 : -3, -2, -3),
                    terminal.offset(cpuListScenario ? 13 : 9, 3, 3))) {
                level.setBlockAndUpdate(pos, pos.getY() == terminal.getY() - 2
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            PartHelper.setPart(level, terminal, null, player, (cpuListScenario
                    ? AEParts.COVERED_DENSE_CABLE : AEParts.GLASS_CABLE).item(appeng.api.util.AEColor.TRANSPARENT));
            PartHelper.setPart(level, terminal, Direction.NORTH, player, AEParts.CRAFTING_TERMINAL.asItem());
            DispatchStatusFixture.place(player, terminal.west(2), "16k_crafting_storage");
            if (cpuListScenario) {
                for (int offset : new int[] { 4, 6, 8, 10, 12, 14 }) {
                    DispatchStatusFixture.place(player, terminal.west(offset), "16k_crafting_storage");
                }
            }
            DispatchStatusFixture.place(player, terminal.east(2), "drive");
            DispatchStatusFixture.place(player, terminal.below(), "creative_energy_cell");
            for (int offset : new int[] {4, 8}) {
                DispatchStatusFixture.place(player, terminal.east(offset), "pattern_provider");
                level.setBlockAndUpdate(terminal.east(offset).below(), Blocks.FURNACE.defaultBlockState());
            }
            if (cpuListScenario) {
                DispatchStatusFixture.place(player, terminal.east(12), "pattern_provider");
                level.setBlockAndUpdate(terminal.east(12).below(), Blocks.FURNACE.defaultBlockState());
            }
            if (holdFinalOutput) {
                var obstruction = terminal.east(8).north();
                PartHelper.setPart(level, obstruction, null, player, AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
                PartHelper.setPart(level, obstruction, Direction.NORTH, player, AEParts.CRAFTING_TERMINAL.asItem());
            }
            player.teleportTo(terminal.getX() + 0.5, terminal.getY() - 1, terminal.getZ() - 2.5);
            return false;
        }
        checkpoint = "terminal-node";
        var node = ((IInWorldGridNodeHost) level.getBlockEntity(terminal)).getGridNode(Direction.NORTH);
        if (node == null) return false;
        var nodes = new java.util.ArrayList<>(java.util.List.of(terminal.west(2), terminal.east(2), terminal.below(),
                terminal.east(4), terminal.east(8)));
        if (cpuListScenario) nodes.add(terminal.east(12));
        if (cpuListScenario) for (int offset : new int[] { 4, 6, 8, 10, 12, 14 }) nodes.add(terminal.west(offset));
        for (var pos : nodes) {
            checkpoint = "node " + pos;
            var other = ((IInWorldGridNodeHost) level.getBlockEntity(pos)).getGridNode(Direction.UP);
            if (other == null) return false;
            if (node.getGrid() != other.getGrid()) GridHelper.createConnection(node, other);
        }
        checkpoint = "cpu";
        var cpu = cpu(player);
        if (!cpu.isFormed()) {
            var calculator = new CraftingCPUCalculator(cpu);
            var pos = terminal.west(2);
            calculator.updateBlockEntities(calculator.createCluster(level, pos, pos), level, pos, pos);
        }
        if (cpuListScenario) {
            var index = 0;
            for (var extra : cpuListCpus(player)) {
                if (!extra.isFormed()) {
                    var calculator = new CraftingCPUCalculator(extra);
                    var pos = terminal.west(2 + index * 2);
                    calculator.updateBlockEntities(calculator.createCluster(level, pos, pos), level, pos, pos);
                }
                extra.setName(index < 4 ? java.util.List.of("Alpha CPU",
                        "Beta CPU with a deliberately long English tooltip name", "Gamma CPU", "Delta CPU").get(index)
                        : "Idle CPU " + (index + 1));
                index++;
            }
            if (index == 7 && cpuListIdentities == null) {
                cpuListIdentities = cpuListCpus(player);
            }
        }
        checkpoint = "cpu-active";
        if (!cpu.getCluster().isActive()) return false;
        checkpoint = "patterns";
        if (!initialized) {
            var drive = (DriveBlockEntity) level.getBlockEntity(terminal.east(2));
            drive.getInternalInventory().setItemDirect(0, appeng.core.definitions.AEItems.ITEM_CELL_1K.stack());
            if (!missingPlanInput) {
                drive.getCellInventory(0).insert(AEItemKey.of(Items.COBBLESTONE), cpuListScenario ? 4096 : 2, Actionable.MODULATE,
                        IActionSource.empty());
                if (cpuListScenario) drive.getCellInventory(0).insert(AEItemKey.of(Items.SAND), 256,
                        Actionable.MODULATE, IActionSource.empty());
            }
            pattern(player, 4, Items.COBBLESTONE, Items.STONE);
            pattern(player, 8, Items.STONE, Items.SMOOTH_STONE);
            if (cpuListScenario) pattern(player, 12, Items.SAND, Items.GLASS);
            if (!unprofiledPlan) seed(player);
            initialized = true;
        }
        checkpoint = "craftable";
        return node.getGrid().getCraftingService().isCraftable(AEItemKey.of(Items.SMOOTH_STONE));
    }

    void seed(ServerPlayer player) {
        seed(player, Items.STONE);
        seed(player, Items.SMOOTH_STONE);
    }

    void seed(ServerPlayer player, net.minecraft.world.item.Item item) {
        var grid = cpu(player).getMainNode().getGrid();
        var network = ProfilerBridge.networkId(grid);
        var tick = player.serverLevel().getGameTime();
        var key = AEItemKey.of(item);
        ProfilerBridge.start(network, this, key, 1, tick);
        ProfilerBridge.complete(network, this, key, 1,
                tick + sampleMultiplier * (item == Items.STONE ? 100 : 40));
    }

    void preparePartialJob(ServerPlayer player) {
        var grid = cpu(player).getMainNode().getGrid();
        grid.getStorageService().getInventory().extract(AEItemKey.of(Items.SMOOTH_STONE),
                Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty());
        ProfilerBridge.clearStats(ProfilerBridge.key(ProfilerBridge.networkId(grid), AEItemKey.of(Items.SMOOTH_STONE)));
        initialSamples = null;
        returnedStone = false;
    }

    private void pattern(ServerPlayer player, int offset, net.minecraft.world.item.Item input, net.minecraft.world.item.Item output) {
        var provider = (PatternProviderBlockEntity) player.serverLevel().getBlockEntity(terminal.east(offset));
        provider.getLogic().getPatternInv().setItemDirect(0, ServerDriverPlatform.processingPattern(
                new GenericStack(AEItemKey.of(input), 1), new GenericStack(AEItemKey.of(output), 1)));
        provider.getLogic().updatePatterns();
    }

    CraftingBlockEntity cpu(ServerPlayer player) {
        return (CraftingBlockEntity) player.serverLevel().getBlockEntity(terminal.west(2));
    }

    boolean prepareCpuListJobs(ServerPlayer player) {
        if (sampleMultiplier == 1) {
            sampleMultiplier = 3_600;
            var network = ProfilerBridge.networkId(cpu(player).getMainNode().getGrid());
            ProfilerBridge.clearStats(ProfilerBridge.key(network, AEItemKey.of(Items.STONE)));
            ProfilerBridge.clearStats(ProfilerBridge.key(network, AEItemKey.of(Items.SMOOTH_STONE)));
            seed(player);
        }
        var cpus = cpuListCpus(player);
        if (cpus.stream().anyMatch(cpu -> !cpu.getCluster().isActive())) return false;
        var service = cpu(player).getMainNode().getGrid().getCraftingService();
        if (cpuListPlans == null) {
            var jobs = java.util.List.of(new CpuJob(Items.SMOOTH_STONE, 4), new CpuJob(Items.SMOOTH_STONE, 8),
                    new CpuJob(Items.SMOOTH_STONE, 12), new CpuJob(Items.GLASS, 16));
            cpuListPlans = jobs.stream().map(job ->
                    service.beginCraftingCalculation(player.serverLevel(), () -> IActionSource.ofMachine(cpu(player)),
                            AEItemKey.of(job.item()), job.amount(),
                            appeng.api.networking.crafting.CalculationStrategy.REPORT_MISSING_ITEMS)).toList();
            return false;
        }
        if (cpuListPlans.stream().anyMatch(plan -> !plan.isDone())) return false;
        if (!cpuListSubmitted) {
            for (var i = 0; i < 4; i++) {
                try {
                    var result = service.submitJob(cpuListPlans.get(i).get(), null, cpus.get(i).getCluster(), false,
                            IActionSource.ofMachine(cpus.get(i)));
                    if (!result.successful()) throw new IllegalStateException("multi-CPU submission failed: " + result);
                } catch (Exception error) {
                    throw new IllegalStateException("multi-CPU calculation failed", error);
                }
            }
            cpuListSubmitted = true;
        }
        return cpus.subList(0, 4).stream().allMatch(cpu -> cpu.getCluster().isBusy());
    }

    void makeCpuListPartial(ServerPlayer player) {
        var grid = cpu(player).getMainNode().getGrid();
        ProfilerBridge.clearStats(ProfilerBridge.key(ProfilerBridge.networkId(grid), AEItemKey.of(Items.SMOOTH_STONE)));
    }

    void restoreCpuListSamples(ServerPlayer player) { seed(player, Items.SMOOTH_STONE); }

    void renameCpuList(ServerPlayer player) {
        var cpus = cpuListCpus(player);
        cpus.get(0).setName("Zulu CPU with a deliberately long English tooltip name");
        cpus.get(1).setName("Alpha CPU with a deliberately long English tooltip name");
        cpus.get(2).setName("Дуже довга назва процесора для перевірки макета");
    }

    boolean finishFirstCpu(ServerPlayer player) {
        pump(player, true);
        return !cpuListCpus(player).get(0).getCluster().isBusy();
    }

    void cancelSecondCpu(ServerPlayer player) { cpuListCpus(player).get(1).getCluster().craftingLogic.cancel(); }

    boolean replaceSecondCpu(ServerPlayer player) {
        var replacementCpu = cpuListCpus(player).get(1);
        var service = cpu(player).getMainNode().getGrid().getCraftingService();
        if (replacementPlan == null) {
            replacementPlan = service.beginCraftingCalculation(player.serverLevel(),
                    () -> IActionSource.ofMachine(replacementCpu), AEItemKey.of(Items.SMOOTH_STONE), 8,
                    appeng.api.networking.crafting.CalculationStrategy.REPORT_MISSING_ITEMS);
            return false;
        }
        if (!replacementPlan.isDone()) return false;
        if (!replacementSubmitted) {
            try {
                var result = service.submitJob(replacementPlan.get(), null, replacementCpu.getCluster(), false,
                        IActionSource.ofMachine(replacementCpu));
                if (!result.successful()) throw new IllegalStateException("replacement submission failed: " + result);
            } catch (Exception error) { throw new IllegalStateException("replacement calculation failed", error); }
            replacementSubmitted = true;
        }
        return replacementCpu.getCluster().isBusy();
    }

    void removeThirdCpu(ServerPlayer player) {
        player.serverLevel().setBlockAndUpdate(terminal.west(6), Blocks.AIR.defaultBlockState());
    }

    boolean restartSecondCpu(ServerPlayer player) {
        if (!restarting) {
            cancelSecondCpu(player);
            replacementPlan = null;
            replacementSubmitted = false;
            restarting = true;
        }
        return replaceSecondCpu(player);
    }

    StandardCraftFixture secondGrid() {
        var fixture = new StandardCraftFixture();
        fixture.cpuListScenario = true;
        fixture.sampleMultiplier = sampleMultiplier * 3;
        fixture.originShift = 24;
        return fixture;
    }

    String cpuListServerEstimates(ServerPlayer player) {
        return liveCpuListCpus(player).stream().filter(cpu -> cpu.getCluster().isBusy()).map(cpu ->
                ProfilerBridge.remainingJobSeconds(cpu.getCluster()).stream().mapToObj(Long::toString)
                        .findFirst().orElse("unknown")).collect(java.util.stream.Collectors.joining(","));
    }

    String cpuListServerState(ServerPlayer player) {
        var grid = cpu(player).getMainNode().getGrid();
        var serials = player.containerMenu instanceof appeng.menu.me.crafting.CraftingStatusMenu menu
                ? ((com.ctux.ae2craftingtime.mc1201.mixin.CraftingStatusMenuAccessor) menu)
                        .ae2craftingtime$getCpuSerialMap()
                : java.util.Map.<appeng.api.networking.crafting.ICraftingCPU, Integer>of();
        var cpus = cpuListCpus(player);
        var rows = new java.util.ArrayList<CpuListTtcControl.CpuState>();
        for (int index = 0; index < cpus.size(); index++) {
            var block = cpus.get(index);
            boolean live = player.serverLevel().getBlockEntity(block.getBlockPos()) == block;
            var cluster = live ? block.getCluster() : null;
            var status = cluster == null ? null : cluster.getJobStatus();
            var job = status == null ? null : status.crafting();
            var estimate = status == null ? java.util.OptionalLong.empty() : ProfilerBridge.remainingJobSeconds(cluster);
            rows.add(new CpuListTtcControl.CpuState(block.getBlockPos().toShortString(),
                    cluster == null ? -1 : serials.getOrDefault(cluster, -1),
                    job == null ? null : job.what().getId().toString(), job == null ? 0 : job.amount(),
                    live, status != null, estimate.isPresent() ? estimate.getAsLong() : null,
                    status == null ? 0 : status.elapsedTimeNanos(), status == null ? 0 : status.progress()));
        }
        var network = ProfilerBridge.networkId(grid);
        return new com.google.gson.Gson().toJson(new CpuListTtcControl.ServerState(network.toString(),
                player.containerMenu.containerId,
                ProfilerBridge.stats(ProfilerBridge.key(network, AEItemKey.of(Items.STONE))).isPresent(),
                ProfilerBridge.stats(ProfilerBridge.key(network, AEItemKey.of(Items.SMOOTH_STONE))).isPresent(), rows));
    }

    private java.util.List<CraftingBlockEntity> cpuListCpus(ServerPlayer player) {
        if (cpuListIdentities != null) return cpuListIdentities;
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> player.serverLevel().getBlockEntity(terminal.west(2 + index * 2)))
                .filter(CraftingBlockEntity.class::isInstance).map(CraftingBlockEntity.class::cast).toList();
    }

    private java.util.List<CraftingBlockEntity> liveCpuListCpus(ServerPlayer player) {
        return cpuListCpus(player).stream().filter(cpu -> player.serverLevel().getBlockEntity(cpu.getBlockPos()) == cpu)
                .toList();
    }

    private record CpuJob(net.minecraft.world.item.Item item, long amount) { }

    private int[] sampleCounts(ServerPlayer player) {
        var network = ProfilerBridge.networkId(cpu(player).getMainNode().getGrid());
        return java.util.stream.Stream.of(Items.STONE, Items.SMOOTH_STONE)
                .mapToInt(item -> ProfilerBridge.stats(ProfilerBridge.key(network, AEItemKey.of(item)))
                        .map(stats -> stats.sampleCount()).orElse(0)).toArray();
    }

    boolean observedNewSamples(ServerPlayer player) {
        var current = sampleCounts(player);
        return initialSamples != null && current[0] > initialSamples[0] && current[1] > initialSamples[1];
    }

    boolean finalOutputReady(ServerPlayer player) {
        var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(terminal.east(8).below());
        return furnace.getItem(2).is(Items.SMOOTH_STONE) && furnace.getItem(2).getCount() == 1;
    }

    void viewFinalProvider(ServerPlayer player) {
        player.teleportTo(terminal.getX() + 9, terminal.getY() - 1, terminal.getZ() - 2.5);
        player.setYRot(9.462f);
        player.setXRot(2);
    }

    long pump(ServerPlayer player, boolean fuel) {
        if (fuel && initialSamples == null) initialSamples = sampleCounts(player);
        var storage = cpu(player).getMainNode().getGrid().getStorageService().getInventory();
        for (int offset : new int[] {4, 8}) {
            var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(terminal.east(offset).below());
            if (fuel && furnace.getItem(1).isEmpty()) furnace.setItem(1, new ItemStack(Items.COAL));
            var output = furnace.getItem(2);
            if (!output.isEmpty() && !(offset == 8 && holdFinalOutput)) {
                long inserted = storage.insert(AEItemKey.of(output), output.getCount(), Actionable.MODULATE, IActionSource.ofMachine(cpu(player)));
                if (offset == 4 && inserted > 0) returnedStone = true;
                furnace.removeItem(2, (int) inserted);
                furnace.setChanged();
            }
        }
        return storage.extract(AEItemKey.of(Items.SMOOTH_STONE), Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty());
    }
}
