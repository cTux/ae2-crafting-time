package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.me.crafting.CraftingStatusScreen;
import appeng.client.gui.me.common.MEStorageScreen;
import com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Executable A1-A5 regression flow for per-card CPU totals. */
final class CpuListTtcScenario {
    static final java.util.List<String> CHECKS = java.util.List.of(
            "initial-distinct", "unknown-hidden", "idle-hidden", "badge-select", "selected-title", "tooltip",
            "scroll-down", "scroll-up", "partial", "stalled", "reordered", "finished", "cancelled",
            "replacement", "removed", "drop-expiry", "delayed-expiry", "close-reopen", "second-grid",
            "small-scale", "large-scale", "same-jvm-clear", "process-relaunch", "reconnect", "layout");

    private enum Stage { INITIAL, SELECTED, TOOLTIP, SCROLL_DOWN, SCROLL_UP, PARTIAL, RESTORE, STALLED,
        RENAME, FINISH, CANCEL, REPLACE_EMPTY, REPLACE_STARTED, REPLACE, RESTART, RESTART_CLEARED, RESTART_FRESH,
        REMOVE_SCROLL, REMOVE, DROP, DROP_EXPIRED, HOLD, HOLD_EXPIRED, HOLD_RELEASED,
        REOPEN, REOPENED, SECOND_PREPARE, SECOND_OPEN, SECOND_SCREEN, SCALE_SMALL, SCALE_LARGE,
        REJOIN_REQUEST, REJOIN_PREPARE, REJOIN_OPEN, REJOIN_EMPTY, REJOIN_REFRESH, REJOIN_FRESH, RELAUNCH_READY,
        RELAUNCH_PREPARE, RELAUNCH_OPEN, RELAUNCH_EMPTY, RELAUNCH_REFRESH, RELAUNCH_FRESH, DONE }

    private final StandardCraftFixture first;
    private final String world;
    private final java.nio.file.Path evidence;
    private final boolean connectedDedicated;
    private final Map<Integer, String> originalJobs = new LinkedHashMap<>();
    private final Map<Integer, String> originalTotals = new LinkedHashMap<>();
    private final java.util.Set<String> captured = new java.util.HashSet<>();
    private StandardCraftFixture second;
    private CompletableFuture<Boolean> operation;
    private CompletableFuture<String> observation;
    private Stage stage = Stage.INITIAL;
    private long stageStarted = System.nanoTime();
    private int firstVisibleSerial = -1;
    private int selectedSerial = -1;
    private int finishSerial = -1;
    private int removeSerial = -1;
    private long stalledElapsed;
    private String activeAction;
    private boolean actionComplete;
    private long lastFrame = -1;
    private long releasedFrame = -1;
    private long replacedElapsed;
    private boolean opening;
    private boolean reconnectRequested;
    private boolean continuationWritten;
    private final CpuListContinuation continuation;
    private java.util.List<Long> stalledProgress;
    private volatile String authoritativeState;
    private CpuListTtcControl.ServerState partialState;

    CpuListTtcScenario(StandardCraftFixture first, String world, java.nio.file.Path output, boolean connectedDedicated,
            java.util.List<String> resultScreenshots) {
        this.first = first;
        this.world = world;
        this.connectedDedicated = connectedDedicated;
        var path = System.getProperty("ae2craftingtime.test.continuation", "");
        this.evidence = output.resolve(checkpointFile(!path.isBlank()));
        if (path.isBlank()) {
            continuation = null;
        } else {
            try {
                continuation = CpuListContinuation.read(java.nio.file.Path.of(path), world,
                        System.getProperty("ae2craftingtime.test.campaign", "local"));
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Cannot read CPU-list relaunch continuation", error);
            }
            first.cpuListScenario = true;
            first.bindTerminal(new net.minecraft.core.BlockPos(
                    continuation.terminalX(), continuation.terminalY(), continuation.terminalZ()));
            second = first;
            authoritativeState = continuation.serverState();
            captured.addAll(continuation.screenshots());
            resultScreenshots.addAll(continuation.screenshots());
            CpuTtcPacketControl.holdLatest();
            stage = Stage.RELAUNCH_PREPARE;
        }
    }

    static String checkpointFile(boolean resumed) {
        return resumed ? "cpu-list-checkpoints.phase-2.jsonl" : "cpu-list-checkpoints.jsonl";
    }

    String checkpoint() { return "cpu-list=" + stage; }
    boolean resumed() { return continuation != null; }
    boolean reconnectRequested() { return reconnectRequested; }
    void reconnected() {
        reconnectRequested = false;
        if (second != null) second.refreshCpuIdentities();
        next(Stage.REJOIN_PREPARE);
    }

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> capture, BiConsumer<Integer, Integer> moveMouse) {
        Consumer<String> screenshot = name -> { if (captured.add(name)) capture.accept(name); };
        if (continuation != null) continuation.checks().forEach(check -> mark(checks, check));
        var snapshot = UiObservationStore.latest();
        if (requiresObservation(stage)) {
            if (!(minecraft.screen instanceof CraftingStatusScreen) || snapshot == null
                    || snapshot.frame() == lastFrame) return false;
            lastFrame = snapshot.frame();
            if (!observe(minecraft)) return false;
            retain(snapshot);
        }

        switch (stage) {
            case INITIAL -> {
                if (snapshot.cpuCards().size() != 6) return false;
                var known = snapshot.cpuCards().stream().filter(card -> card.ttc() != null).toList();
                if (known.size() != 3 || known.stream().map(card -> card.ttc().rendered()).distinct().count() != 3) return false;
                if (!totalsMatch(snapshot)) return false;
                if (snapshot.cpuCards().stream().filter(card -> card.jobId() == null).anyMatch(card -> card.ttc() != null))
                    throw new IllegalStateException("idle CPU rendered a total");
                if (snapshot.cpuCards().stream().filter(card -> "minecraft:glass".equals(card.jobId())).anyMatch(card -> card.ttc() != null))
                    throw new IllegalStateException("unknown busy CPU rendered a total");
                known.forEach(card -> {
                    originalJobs.put(card.serial(), job(card));
                    originalTotals.put(card.serial(), card.ttc().rendered());
                });
                firstVisibleSerial = firstVisibleSerial(snapshot.cpuCards());
                selectedSerial = serialForJob(snapshot.cpuCards(), "minecraft:smooth_stone", 8);
                finishSerial = serialForJob(snapshot.cpuCards(), "minecraft:glass", 4);
                removeSerial = serialForJob(snapshot.cpuCards(), "minecraft:smooth_stone", 12);
                validateLayout(snapshot);
                mark(checks, "initial-distinct", "unknown-hidden", "idle-hidden", "layout");
                screenshot.accept("cpu-list-total-ttc-unselected-large.png");
                var badge = selectionBadge(snapshot.cpuCards(), selectedSerial);
                releasedFrame = snapshot.frame();
                DriverPlatform.clickAndRelease(minecraft, badge.centerX(), badge.centerY());
                next(Stage.SELECTED);
            }
            case SELECTED -> {
                if (snapshot.frame() <= releasedFrame) return false;
                var selected = card(snapshot, selectedSerial);
                if (!selected.selected() || selected.ttc() == null) return false;
                var title = title(snapshot);
                if (title == null || !title.rendered().equals(selected.ttc().rendered())) return false;
                if (snapshot.text().stream().noneMatch(text -> text.bounds() != null
                        && text.bounds().inside(selected.nameArea()) && text.rendered().endsWith("...")
                        && selected.name().startsWith(text.rendered().substring(0, text.rendered().length() - 3)))) return false;
                mark(checks, "badge-select", "selected-title");
                screenshot.accept("cpu-list-total-ttc-selected-large.png");
                moveMouse.accept(selected.badge().centerX(), selected.badge().centerY());
                next(Stage.TOOLTIP);
            }
            case TOOLTIP -> {
                var selected = card(snapshot, selectedSerial);
                if (!selected.name().contains("deliberately long")
                        || snapshot.tooltip().stream().noneMatch(text -> text.rendered().equals(selected.name()))) return false;
                mark(checks, "tooltip");
                screenshot.accept("cpu-list-total-ttc-tooltip.png");
                moveMouse.accept(0, 0);
                CpuListScrollControl.scrollTo(1);
                next(Stage.SCROLL_DOWN);
            }
            case SCROLL_DOWN -> {
                if (snapshot.cpuCards().stream().anyMatch(card -> card.serial() == firstVisibleSerial)
                        || snapshot.cpuCards().stream().noneMatch(card -> card.name().equals("Idle CPU 7"))
                        || !totalsMatch(snapshot)) return false;
                mark(checks, "scroll-down");
                screenshot.accept("cpu-list-total-ttc-scroll-down.png");
                CpuListScrollControl.scrollTo(0);
                next(Stage.SCROLL_UP);
            }
            case SCROLL_UP -> {
                if (snapshot.cpuCards().stream().noneMatch(card -> card.serial() == firstVisibleSerial)
                        || snapshot.cpuCards().stream().anyMatch(card -> card.name().equals("Idle CPU 7"))
                        || !totalsMatch(snapshot)) return false;
                mark(checks, "scroll-up");
                screenshot.accept("cpu-list-total-ttc-scroll-up.png");
                if (server(minecraft, "partial", player -> { first.makeCpuListPartial(player); return true; })) next(Stage.PARTIAL);
            }
            case PARTIAL -> {
                var state = serverState();
                if (partialState == null && state.stoneProfile() && !state.smoothProfile()
                        && state.cpus().stream().filter(cpu -> "minecraft:smooth_stone".equals(cpu.jobId()))
                                .noneMatch(cpu -> cpu.seconds() == null)) partialState = state;
                if (partialState == null
                        || snapshot.cpuCards().stream().filter(card -> "minecraft:smooth_stone".equals(card.jobId()))
                                .noneMatch(card -> card.ttc() != null)
                        || !totalsMatch(snapshot, partialState)) return false;
                mark(checks, "partial");
                screenshot.accept("cpu-list-total-ttc-partial.png");
                if (server(minecraft, "restore", player -> { first.restoreCpuListSamples(player); return true; })) {
                    stalledElapsed = snapshot.cpuCards().stream().filter(card -> card.jobId() != null)
                            .mapToLong(UiSnapshot.CpuCard::elapsedNanos).max().orElse(0);
                    stalledProgress = serverState().cpus().stream().map(CpuListTtcControl.CpuState::progress).toList();
                    next(Stage.STALLED);
                }
            }
            case STALLED -> {
                if (elapsedMillis() < 1200 || snapshot.cpuCards().stream().filter(card -> card.jobId() != null)
                        .mapToLong(UiSnapshot.CpuCard::elapsedNanos).max().orElse(0) <= stalledElapsed) return false;
                if (snapshot.cpuCards().stream().noneMatch(card -> card.ttc() != null)) return false;
                if (!serverState().cpus().stream().map(CpuListTtcControl.CpuState::progress).toList().equals(stalledProgress))
                    throw new IllegalStateException("held jobs progressed during the stalled checkpoint");
                if (!totalsMatch(snapshot)) return false;
                mark(checks, "stalled");
                screenshot.accept("cpu-list-total-ttc-stalled.png");
                if (server(minecraft, "rename", player -> { first.renameCpuList(player); return true; })) next(Stage.RENAME);
            }
            case RENAME -> {
                if (snapshot.cpuCards().stream().filter(card -> card.serial() == selectedSerial)
                        .noneMatch(card -> card.name().startsWith("Alpha CPU with"))) return false;
                for (var card : snapshot.cpuCards()) if (originalJobs.containsKey(card.serial())
                        && !originalJobs.get(card.serial()).equals(job(card)))
                    throw new IllegalStateException("rename/reorder changed serial-job binding");
                mark(checks, "reordered");
                screenshot.accept("cpu-list-total-ttc-reordered.png");
                next(Stage.FINISH);
            }
            case FINISH -> {
                if (!server(minecraft, "finish", first::finishFirstCpu)) return false;
                if (snapshot.cpuCards().stream().filter(card -> card.serial() == finishSerial)
                        .anyMatch(card -> card.jobId() != null || card.ttc() != null)) return false;
                mark(checks, "finished");
                screenshot.accept("cpu-list-total-ttc-finished.png");
                next(Stage.CANCEL);
            }
            case CANCEL -> {
                if (!server(minecraft, "cancel", player -> { first.cancelSecondCpu(player); return true; })) return false;
                var selected = snapshot.cpuCards().stream().filter(card -> card.serial() == selectedSerial).findFirst();
                if (selected.isPresent() && (selected.get().jobId() != null || selected.get().ttc() != null)) return false;
                mark(checks, "cancelled");
                screenshot.accept("cpu-list-total-ttc-cancelled.png");
                next(Stage.REPLACE_EMPTY);
            }
            case REPLACE_EMPTY -> {
                if (card(snapshot, selectedSerial).ttc() != null) throw new IllegalStateException("cancelled job retained stale total");
                if (server(minecraft, "replace", first::replaceSecondCpu)) next(Stage.REPLACE_STARTED);
            }
            case REPLACE_STARTED -> {
                var replacement = card(snapshot, selectedSerial);
                if (!"minecraft:smooth_stone".equals(replacement.jobId()) || replacement.amount() != 8) return false;
                next(Stage.REPLACE);
            }
            case REPLACE -> {
                var replacement = card(snapshot, selectedSerial);
                if (!"minecraft:smooth_stone".equals(replacement.jobId()) || replacement.amount() != 8
                        || replacement.ttc() == null || replacement.elapsedNanos() < 2_000_000_000L) return false;
                replacedElapsed = replacement.elapsedNanos();
                CpuTtcPacketControl.hold();
                next(Stage.RESTART);
            }
            case RESTART -> {
                if (server(minecraft, "restart", first::restartSecondCpu)) next(Stage.RESTART_CLEARED);
            }
            case RESTART_CLEARED -> {
                var replacement = card(snapshot, selectedSerial);
                if (!"minecraft:smooth_stone".equals(replacement.jobId()) || replacement.amount() != 8
                        || replacement.elapsedNanos() >= replacedElapsed) return false;
                if (replacement.ttc() != null) throw new IllegalStateException("same-output same-amount restart retained stale TTC");
                screenshot.accept("cpu-list-total-ttc-restart-cleared.png");
                CpuTtcPacketControl.resume();
                next(Stage.RESTART_FRESH);
            }
            case RESTART_FRESH -> {
                if (card(snapshot, selectedSerial).ttc() == null || !totalsMatch(snapshot)) return false;
                mark(checks, "replacement");
                screenshot.accept("cpu-list-total-ttc-replacement.png");
                CpuListScrollControl.scrollTo(1);
                next(Stage.REMOVE_SCROLL);
            }
            case REMOVE_SCROLL -> {
                if (snapshot.cpuCards().stream().noneMatch(card -> card.serial() == removeSerial)) return false;
                next(Stage.REMOVE);
            }
            case REMOVE -> {
                if (!server(minecraft, "remove", player -> { first.removeThirdCpu(player); return true; })) return false;
                if (snapshot.cpuCards().stream().anyMatch(card -> card.serial() == removeSerial)) return false;
                mark(checks, "removed");
                screenshot.accept("cpu-list-total-ttc-removed.png");
                CpuTtcPacketControl.drop();
                next(Stage.DROP);
            }
            case DROP -> { if (elapsedMillis() >= 3600) next(Stage.DROP_EXPIRED); }
            case DROP_EXPIRED -> {
                if (snapshot.cpuCards().stream().anyMatch(card -> card.ttc() != null)) return false;
                mark(checks, "drop-expiry");
                screenshot.accept("cpu-list-total-ttc-drop-expiry.png");
                CpuTtcPacketControl.resume();
                next(Stage.HOLD);
            }
            case HOLD -> {
                if (snapshot.cpuCards().stream().noneMatch(card -> card.ttc() != null)) return false;
                CpuTtcPacketControl.hold();
                next(Stage.HOLD_EXPIRED);
            }
            case HOLD_EXPIRED -> {
                if (elapsedMillis() < 3600 || !CpuTtcPacketControl.hasHeld()) return false;
                if (snapshot.cpuCards().stream().anyMatch(card -> card.ttc() != null)) return false;
                CpuTtcPacketControl.releaseHeld();
                CpuTtcPacketControl.drop();
                releasedFrame = snapshot.frame();
                next(Stage.HOLD_RELEASED);
            }
            case HOLD_RELEASED -> {
                if (snapshot.frame() <= releasedFrame) return false;
                if (snapshot.cpuCards().stream().anyMatch(card -> card.ttc() != null))
                    throw new IllegalStateException("superseded delayed response repopulated cache");
                CpuTtcPacketControl.resume();
                mark(checks, "delayed-expiry");
                screenshot.accept("cpu-list-total-ttc-delayed-expiry.png");
                minecraft.player.closeContainer();
                next(Stage.REOPEN);
            }
            case REOPEN -> {
                openTerminal(minecraft, first);
                if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                    var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                    DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                    next(Stage.REOPENED);
                }
            }
            case REOPENED -> {
                if (snapshot.cpuCards().stream().noneMatch(card -> card.ttc() != null)) return false;
                mark(checks, "close-reopen");
                screenshot.accept("cpu-list-total-ttc-close-reopen.png");
                minecraft.player.closeContainer();
                second = first.secondGrid();
                next(Stage.SECOND_PREPARE);
            }
            case SECOND_PREPARE -> {
                if (server(minecraft, "second-grid", player -> {
                    if (!second.prepare(player, marker) || !second.prepareCpuListJobs(player)) return false;
                    return true;
                })) {
                    if (connectedDedicated) {
                        var state = CpuListTtcControl.state();
                        second.bindTerminal(new net.minecraft.core.BlockPos(state.x(), state.y(), state.z()));
                    }
                    next(Stage.SECOND_OPEN);
                }
            }
            case SECOND_OPEN -> {
                openTerminal(minecraft, second);
                if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                    var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                    DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                    next(Stage.SECOND_SCREEN);
                }
            }
            case SECOND_SCREEN -> {
                if (snapshot.cpuCards().stream().filter(card -> card.ttc() != null).count() != 3) return false;
                if (snapshot.cpuCards().stream().filter(card -> card.ttc() != null)
                        .anyMatch(card -> originalTotals.get(card.serial()) != null
                                && originalTotals.get(card.serial()).equals(card.ttc().rendered())))
                    throw new IllegalStateException("second grid accepted first-grid total for an overlapping serial");
                mark(checks, "second-grid");
                screenshot.accept("cpu-list-total-ttc-second-grid.png");
                minecraft.options.guiScale().set(1);
                DriverPlatform.resizeDisplay(minecraft);
                next(Stage.SCALE_SMALL);
            }
            case SCALE_SMALL -> {
                if (snapshot.guiScale() != 1 || snapshot.cpuCards().stream().noneMatch(card -> card.ttc() != null)) return false;
                validateLayout(snapshot);
                mark(checks, "small-scale");
                screenshot.accept("cpu-list-total-ttc-smallest-scale.png");
                minecraft.options.guiScale().set(0);
                DriverPlatform.resizeDisplay(minecraft);
                next(Stage.SCALE_LARGE);
            }
            case SCALE_LARGE -> {
                if (snapshot.cpuCards().stream().noneMatch(card -> card.ttc() != null)) return false;
                if (!fits(snapshot)) {
                    if (snapshot.guiScale() <= 1) throw new IllegalStateException("No GUI scale fits the CPU-list screen");
                    minecraft.options.guiScale().set((int) snapshot.guiScale() - 1);
                    DriverPlatform.resizeDisplay(minecraft);
                    return false;
                }
                validateLayout(snapshot);
                mark(checks, "large-scale");
                screenshot.accept("cpu-list-total-ttc-largest-scale.png");
                minecraft.player.closeContainer();
                CpuTtcPacketControl.holdLatest();
                next(Stage.REJOIN_REQUEST);
            }
            case REJOIN_REQUEST -> {
                reconnectRequested = true;
            }
            case REJOIN_PREPARE, RELAUNCH_PREPARE -> {
                var action = stage == Stage.REJOIN_PREPARE ? "rejoin-prepare" : "relaunch-prepare";
                var prepared = server(minecraft, action, player -> second.prepare(player, marker));
                if (prepared) {
                    next(stage == Stage.REJOIN_PREPARE ? Stage.REJOIN_OPEN : Stage.RELAUNCH_OPEN);
                }
            }
            case REJOIN_OPEN, RELAUNCH_OPEN -> {
                if (minecraft.player == null || minecraft.gameMode == null) {
                    return false;
                }
                openTerminal(minecraft, second);
                if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                    var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                    next(stage == Stage.REJOIN_OPEN ? Stage.REJOIN_EMPTY : Stage.RELAUNCH_EMPTY);
                    DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                }
            }
            case REJOIN_EMPTY, RELAUNCH_EMPTY -> {
                if (snapshot.cpuCards().isEmpty() || !CpuTtcPacketControl.hasHeld()) return false;
                if (snapshot.cpuCards().stream().anyMatch(card -> card.ttc() != null)) {
                    throw new IllegalStateException("reconnected menu inherited TTC before a fresh response");
                }
                if (stage == Stage.REJOIN_EMPTY) {
                    mark(checks, "same-jvm-clear");
                    screenshot.accept("cpu-list-total-ttc-disconnect-cleared.png");
                    CpuTtcPacketControl.releaseHeld();
                    next(Stage.REJOIN_REFRESH);
                } else {
                    if (!samePhysicalJobs(new com.google.gson.Gson().fromJson(
                            continuation.serverState(), CpuListTtcControl.ServerState.class), serverState())) {
                        throw new IllegalStateException("relaunch changed the physical CPU, network, or job identity");
                    }
                    mark(checks, "process-relaunch");
                    screenshot.accept("cpu-list-total-ttc-relaunch-cleared.png");
                    CpuTtcPacketControl.releaseHeld();
                    next(Stage.RELAUNCH_REFRESH);
                }
            }
            case REJOIN_REFRESH, RELAUNCH_REFRESH -> {
                var state = serverState();
                if (requiresJobRefresh(connectedDedicated, state)) {
                    var action = stage == Stage.REJOIN_REFRESH ? "rejoin-refresh" : "relaunch-refresh";
                    if (!server(minecraft, action, second::restartSecondCpu)) return false;
                }
                next(stage == Stage.REJOIN_REFRESH ? Stage.REJOIN_FRESH : Stage.RELAUNCH_FRESH);
            }
            case REJOIN_FRESH -> {
                if (snapshot.cpuCards().stream().noneMatch(card -> card.ttc() != null) || !totalsMatch(snapshot)) return false;
                screenshot.accept("cpu-list-total-ttc-rejoin-fresh.png");
                next(Stage.RELAUNCH_READY);
            }
            case RELAUNCH_READY -> {
                if (!continuationWritten) {
                    var state = connectedDedicated ? CpuListTtcControl.state() : null;
                    var serverSequence = state == null ? 0 : state.ack();
                    var clientSequence = Math.max(serverSequence + 1, CpuListTtcControl.clientSequence() + 1);
                    try {
                        CpuListContinuation.write(evidence.resolveSibling("cpu-list-continuation.json"),
                                new CpuListContinuation(1, "relaunch-ready", world,
                                        System.getProperty("ae2craftingtime.test.campaign", "local"),
                                        authoritativeState, second.terminal.getX(), second.terminal.getY(), second.terminal.getZ(),
                                        checks.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList(),
                                        captured.stream().sorted().toList(), serverSequence, clientSequence));
                    } catch (java.io.IOException error) {
                        throw new IllegalStateException("Cannot save CPU-list relaunch continuation", error);
                    }
                    continuationWritten = true;
                    minecraft.stop();
                }
            }
            case RELAUNCH_FRESH -> {
                if (snapshot.cpuCards().stream().noneMatch(card -> card.ttc() != null) || !totalsMatch(snapshot)) return false;
                if (observe(minecraft)) {
                    mark(checks, "reconnect");
                    screenshot.accept("cpu-list-total-ttc-reconnect.png");
                    next(Stage.DONE);
                }
            }
            case DONE -> {
                if (connectedDedicated && !CpuListTtcControl.request("complete")) return false;
                return true;
            }
            default -> { }
        }
        return false;
    }

    private boolean server(Minecraft minecraft, String name, Function<ServerPlayer, Boolean> action) {
        if (!name.equals(activeAction)) {
            activeAction = name;
            actionComplete = false;
            operation = null;
        }
        if (actionComplete) return true;
        if (connectedDedicated) {
            actionComplete = CpuListTtcControl.request(name);
            if (actionComplete) authoritativeState = CpuListTtcControl.state().serverState();
            return actionComplete;
        }
        if (operation == null) {
            var server = minecraft.getSingleplayerServer();
            if (server == null) throw new IllegalStateException("integrated fixture operation used without integrated server");
            var id = minecraft.player.getUUID();
            operation = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                boolean result = action.apply(player);
                if (result) authoritativeState = (second == null ? first : second).cpuListServerState(player);
                return result;
            });
        }
        if (!operation.isDone()) return false;
        boolean result = operation.join();
        operation = null;
        if (result) actionComplete = true;
        return result;
    }

    private void openTerminal(Minecraft minecraft, StandardCraftFixture fixture) {
        if (minecraft.screen == null && !opening) {
            opening = true;
            minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(fixture.terminal).add(0, 0, -0.5), Direction.NORTH,
                        fixture.terminal, false));
        }
    }

    private void next(Stage value) { stage = value; stageStarted = System.nanoTime(); opening = false; }
    private long elapsedMillis() { return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stageStarted); }
    private static String job(UiSnapshot.CpuCard card) { return card.jobId() + ":" + card.amount(); }
    private static UiSnapshot.CpuCard card(UiSnapshot snapshot, int serial) {
        return snapshot.cpuCards().stream().filter(value -> value.serial() == serial).findFirst()
                .orElseThrow(() -> new IllegalStateException("CPU serial " + serial + " is not visible"));
    }
    static Rect selectionBadge(java.util.List<UiSnapshot.CpuCard> cards, int serial) {
        return cards.stream().filter(value -> value.serial() == serial).findFirst()
                .orElseThrow(() -> new IllegalStateException("CPU serial " + serial + " is not visible")).badge();
    }
    static int firstVisibleSerial(java.util.List<UiSnapshot.CpuCard> cards) {
        return cards.stream().findFirst().orElseThrow(() -> new IllegalStateException("CPU list is empty")).serial();
    }
    static int serialForJob(java.util.List<UiSnapshot.CpuCard> cards, String jobId, long amount) {
        return cards.stream().filter(value -> jobId.equals(value.jobId()) && value.amount() == amount)
                .mapToInt(UiSnapshot.CpuCard::serial).findFirst()
                .orElseThrow(() -> new IllegalStateException("CPU job " + jobId + " x" + amount + " is not visible"));
    }
    private static UiSnapshot.ObservedText title(UiSnapshot snapshot) {
        return snapshot.text().stream().filter(text -> text.key().equals("text.ae2craftingtime.ttc")
                && text.bounds() != null && text.bounds().y() < snapshot.gui().y() + 19).findFirst().orElse(null);
    }
    private static void validateLayout(UiSnapshot snapshot) {
        if (!fits(snapshot)) throw new IllegalStateException("CPU-list screen does not fit the viewport");
        for (var card : snapshot.cpuCards()) if (card.ttc() != null) {
            if (card.badge() == null || !card.badge().inside(card.bounds()) || !card.ttc().bounds().inside(card.badge())
                    || card.badge().overlaps(card.infoArea()) || card.badge().overlaps(card.progressArea()))
                throw new IllegalStateException("CPU card layout collision for serial " + card.serial());
            if (snapshot.text().stream().filter(text -> text != card.ttc() && text.bounds() != null
                    && text.bounds().inside(card.nameArea())).anyMatch(text -> text.bounds().overlaps(card.badge())))
                throw new IllegalStateException("CPU name overlaps its TTC badge");
        }
    }
    private static void mark(Map<String, Boolean> checks, String... names) {
        for (var name : names) if (checks.containsKey(name)) checks.put(name, true);
    }

    private static boolean fits(UiSnapshot snapshot) {
        var viewport = new Rect(0, 0, snapshot.screenWidth(), snapshot.screenHeight());
        return snapshot.gui().inside(viewport) && snapshot.cpuCards().stream().allMatch(card -> card.bounds().inside(viewport));
    }

    private CpuListTtcControl.ServerState serverState() {
        return new com.google.gson.Gson().fromJson(authoritativeState, CpuListTtcControl.ServerState.class);
    }

    private boolean totalsMatch(UiSnapshot snapshot) {
        return totalsMatch(snapshot, serverState());
    }

    private static boolean totalsMatch(UiSnapshot snapshot, CpuListTtcControl.ServerState state) {
        for (var card : snapshot.cpuCards()) {
            var cpu = state.cpus().stream().filter(value -> value.serial() == card.serial()).findFirst().orElse(null);
            if (cpu == null || !java.util.Objects.equals(cpu.jobId(), card.jobId()) || cpu.amount() != card.amount()) return false;
            var expected = com.ctux.ae2craftingtime.core.TimeEstimate.formatTotal(java.util.List.of(
                    cpu.seconds() == null ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(cpu.seconds())))
                    .map(value -> com.ctux.ae2craftingtime.mc1201.TtcText.ttc(value).getString()).orElse(null);
            if (!java.util.Objects.equals(expected, card.ttc() == null ? null : card.ttc().rendered())) return false;
        }
        return true;
    }

    static boolean samePhysicalJobs(CpuListTtcControl.ServerState before, CpuListTtcControl.ServerState after) {
        if (before == null || after == null || !java.util.Objects.equals(before.network(), after.network())) return false;
        var first = before.cpus().stream().map(CpuListTtcScenario::physicalJob).sorted().toList();
        var second = after.cpus().stream().map(CpuListTtcScenario::physicalJob).sorted().toList();
        return first.equals(second);
    }

    static boolean requiresJobRefresh(boolean connectedDedicated, CpuListTtcControl.ServerState state) {
        return !connectedDedicated && state != null
                && state.cpus().stream().anyMatch(CpuListTtcControl.CpuState::busy)
                && state.cpus().stream().filter(CpuListTtcControl.CpuState::busy)
                        .noneMatch(cpu -> cpu.seconds() != null);
    }

    private static String physicalJob(CpuListTtcControl.CpuState state) {
        return state.position() + "|" + state.jobId() + "|" + state.amount() + "|" + state.live() + "|" + state.busy();
    }

    private boolean observe(Minecraft minecraft) {
        if (connectedDedicated) {
            authoritativeState = CpuListTtcControl.state().serverState();
            return !authoritativeState.isBlank();
        }
        if (observation == null) {
            var server = minecraft.getSingleplayerServer();
            if (server == null || minecraft.player == null) return false;
            var id = minecraft.player.getUUID();
            observation = server.submit(() -> (second == null ? first : second)
                    .cpuListServerState(server.getPlayerList().getPlayer(id)));
        }
        if (!observation.isDone()) return false;
        authoritativeState = observation.join();
        observation = null;
        return true;
    }

    private static boolean requiresObservation(Stage value) {
        return switch (value) {
            case INITIAL, SELECTED, TOOLTIP, SCROLL_DOWN, SCROLL_UP, PARTIAL, STALLED, RENAME, FINISH,
                    CANCEL, REPLACE_EMPTY, REPLACE_STARTED, REPLACE, RESTART_CLEARED, RESTART_FRESH, REMOVE_SCROLL,
                    REMOVE, DROP_EXPIRED, HOLD, HOLD_EXPIRED, HOLD_RELEASED, REOPENED, SECOND_SCREEN, SCALE_SMALL,
                    SCALE_LARGE, REJOIN_EMPTY, REJOIN_FRESH, RELAUNCH_EMPTY, RELAUNCH_FRESH -> true;
            default -> false;
        };
    }

    private void retain(UiSnapshot snapshot) {
        try {
            java.nio.file.Files.createDirectories(evidence.getParent());
            var value = new com.google.gson.Gson().toJson(java.util.Map.of(
                    "stage", stage.name(), "frame", snapshot.frame(), "screen", snapshot.screen(),
                    "menu", snapshot.menu(), "clientCards", snapshot.cpuCards(), "serverState", serverState()));
            java.nio.file.Files.writeString(evidence, value + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot retain CPU-list checkpoint evidence", error);
        }
    }
}
