package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.ServerConfig;
import com.ctux.ae2craftingtime.core.ServerOptionsWire;
import com.ctux.ae2craftingtime.mc1201.net.ServerOptionsUpdateC2S;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Server-effective options; only an operator can submit edits. */
public final class ServerOptionsScreen extends Screen {
    private static final List<OptionFeature.Group> GROUPS = List.of(OptionFeature.Group.GENERAL,
            OptionFeature.Group.DIAGNOSTICS, OptionFeature.Group.NOTIFICATIONS,
            OptionFeature.Group.INTEGRATIONS, OptionFeature.Group.ADVANCED);
    private final Screen parent;
    private final ServerOptionsWire.Snapshot source;
    private final ServerConfig draft;
    private final OptionFeature.Group group;
    private final int page;
    private final List<EditBox> inputs = new ArrayList<>();
    private Button doneButton;

    @Override
    public void tick() {
        super.tick();
        if (source == null && ClientServerOptions.snapshot() != null)
            Minecraft.getInstance().setScreen(new ServerOptionsScreen(parent));
    }

    public ServerOptionsScreen(Screen parent) {
        this(parent, ClientServerOptions.snapshot(),
                ClientServerOptions.snapshot() == null ? null : ClientServerOptions.snapshot().config().copy(),
                OptionFeature.Group.GENERAL, 0);
    }

    private ServerOptionsScreen(Screen parent, ServerOptionsWire.Snapshot source, ServerConfig draft,
            OptionFeature.Group group, int page) {
        super(Component.translatable("config.ae2craftingtime.title"));
        this.parent = parent;
        this.source = source;
        this.draft = draft;
        this.group = group;
        this.page = page;
    }

    @Override
    protected void init() {
        int left = Math.max(4, (width - 430) / 2);
        int right = Math.min(width - 4, left + 430);
        int rows = Math.max(2, (height - 145) / 28);
        addRenderableWidget(Button.builder(Component.translatable("config.ae2craftingtime.client"), button ->
                Minecraft.getInstance().setScreen(new OptionsScreen(parent)))
                .bounds(left, 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("config.ae2craftingtime.server"), button -> {})
                .bounds(left + 105, 30, 100, 20).build()).active = false;
        if (source == null) {
            addRenderableWidget(Button.builder(Component.translatable("config.ae2craftingtime.server_loading"), button -> {})
                    .bounds(left + 112, 65, 300, 23).build()).active = false;
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                    .bounds(right - 78, height - 54, 76, 20).build());
            return;
        }
        for (int i = 0; i < GROUPS.size(); i++) {
            var next = GROUPS.get(i);
            var button = addRenderableWidget(Button.builder(groupLabel(next), pressed -> {
                if (commitInputs()) Minecraft.getInstance().setScreen(new ServerOptionsScreen(parent, source, draft, next, 0));
            }).bounds(left, 61 + i * 25, 104, 20).build());
            button.active = next != group;
        }
        var features = Arrays.stream(OptionFeature.values())
                .filter(value -> value.owner() == OptionFeature.Owner.SERVER && value.group() == group).toList();
        int totalRows = features.size() + (group == OptionFeature.Group.ADVANCED ? 4 : 0);
        for (int i = page * rows; i < Math.min(totalRows, (page + 1) * rows); i++) {
            int x = left + 112;
            int y = 61 + (i - page * rows) * 28;
            int rowWidth = right - x;
            if (i >= features.size()) {
                addNumericRow(i - features.size(), x, y, rowWidth);
                continue;
            }
            var feature = features.get(i);
            boolean enabled = draft.features().enabled(feature);
            boolean available = integrationAvailable(feature);
            var label = Component.translatable("config.ae2craftingtime." + feature.key())
                    .append(": ").append(Component.translatable(enabled ? "options.on" : "options.off"));
            if (!available) label.append(" (").append(Component.translatable("config.ae2craftingtime.unavailable"))
                    .append(")");
            var toggle = addRenderableWidget(Button.builder(label, pressed -> {
                draft.features().setEnabled(feature, !draft.features().enabled(feature));
                Minecraft.getInstance().setScreen(new ServerOptionsScreen(parent, source, draft, group, page));
            }).bounds(x, y, rowWidth, 23).build());
            toggle.active = source.editable() && available;
        }
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("<"), pressed -> {
            if (commitInputs()) Minecraft.getInstance().setScreen(new ServerOptionsScreen(parent, source, draft, group, page - 1));
        }).bounds(right - 56, height - 84, 24, 20).build());
        if ((page + 1) * rows < totalRows) addRenderableWidget(Button.builder(Component.literal(">"), pressed -> {
            if (commitInputs()) Minecraft.getInstance().setScreen(new ServerOptionsScreen(parent, source, draft, group, page + 1));
        }).bounds(right - 28, height - 84, 24, 20).build());
        if (!source.editable()) addRenderableWidget(Button.builder(
                Component.translatable("config.ae2craftingtime.read_only"), button -> {})
                .bounds(left + 112, height - 104, right - left - 112, 20).build()).active = false;
        var resetGroup = addRenderableWidget(Button.builder(Component.translatable("config.ae2craftingtime.reset_group"), pressed -> {
            var defaults = new ServerConfig();
            for (var feature : features) draft.features().setEnabled(feature, defaults.features().enabled(feature));
            if (group == OptionFeature.Group.ADVANCED) resetNumeric();
            Minecraft.getInstance().setScreen(new ServerOptionsScreen(parent, source, draft, group, 0));
        }).bounds(left, height - 54, 100, 20).build());
        resetGroup.active = source.editable();
        var resetAll = addRenderableWidget(Button.builder(Component.translatable("config.ae2craftingtime.reset_all"), pressed -> {
            draft.reset();
            Minecraft.getInstance().setScreen(new ServerOptionsScreen(parent, source, draft, group, 0));
        }).bounds(left + 104, height - 54, 84, 20).build());
        resetAll.active = source.editable();
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), pressed -> onClose())
                .bounds(right - 158, height - 54, 76, 20).build());
        doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), pressed -> save())
                .bounds(right - 78, height - 54, 76, 20).build());
    }

    private void addNumericRow(int index, int x, int y, int width) {
        String[] keys = {"maxSamples", "outlierMultiplier", "minimumNoProgressSeconds", "typicalDurationMultiplier"};
        String[] values = {Integer.toString(draft.maxSamples()), Double.toString(draft.outlierMultiplier()),
                Integer.toString(draft.minimumNoProgressSeconds()), Double.toString(draft.typicalDurationMultiplier())};
        var label = Component.translatable("config.ae2craftingtime." + keys[index]);
        addRenderableWidget(Button.builder(label, button -> {}).bounds(x, y, width - 84, 23).build()).active = false;
        var input = new EditBox(font, x + width - 80, y, 80, 23, label);
        input.setMaxLength(16);
        input.setValue(values[index]);
        input.setEditable(source.editable());
        addRenderableWidget(input);
        inputs.add(input);
    }

    private boolean commitInputs() {
        if (source == null || !source.editable() || group != OptionFeature.Group.ADVANCED) return true;
        try {
            int start = page * Math.max(2, (height - 145) / 28);
            for (int i = 0; i < inputs.size(); i++) {
                String value = inputs.get(i).getValue();
                switch (start + i) {
                    case 0 -> draft.setMaxSamples(Integer.parseInt(value));
                    case 1 -> draft.setOutlierMultiplier(Double.parseDouble(value));
                    case 2 -> draft.setMinimumNoProgressSeconds(Integer.parseInt(value));
                    case 3 -> draft.setTypicalDurationMultiplier(Double.parseDouble(value));
                    default -> throw new IllegalArgumentException("Unknown option");
                }
            }
            return true;
        } catch (IllegalArgumentException error) {
            doneButton.setMessage(Component.translatable("config.ae2craftingtime.save_failed"));
            return false;
        }
    }

    private void resetNumeric() {
        var defaults = new ServerConfig();
        draft.setMaxSamples(defaults.maxSamples());
        draft.setOutlierMultiplier(defaults.outlierMultiplier());
        draft.setMinimumNoProgressSeconds(defaults.minimumNoProgressSeconds());
        draft.setTypicalDurationMultiplier(defaults.typicalDurationMultiplier());
    }

    private void save() {
        if (!commitInputs()) return;
        if (source.editable() && ClientServerOptions.snapshot() != null
                && ClientServerOptions.snapshot().revision() == source.revision()) {
            StatsNetwork.sendToServer(new ServerOptionsUpdateC2S(ServerOptionsWire.encode(
                    new ServerOptionsWire.Snapshot(source.revision(), false, draft))));
        }
        onClose();
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }

    private static Component groupLabel(OptionFeature.Group group) {
        return Component.translatable("config.ae2craftingtime.group." + group.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean integrationAvailable(OptionFeature feature) {
        String id = switch (feature) {
            case ADVANCED_AE -> "advanced_ae";
            case NEO_ECO -> "neoecoae";
            case AE2_LIGHTNING_TECH -> "ae2lt";
            case APPLIED_MEKANISTICS -> "appmek";
            default -> null;
        };
        return id == null || IntegrationLog.available(id);
    }
}
