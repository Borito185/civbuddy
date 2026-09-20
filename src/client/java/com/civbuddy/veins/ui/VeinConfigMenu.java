package com.civbuddy.veins.ui;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.storage.config.GlobalConfig;
import com.civbuddy.storage.config.JsonConfig;
import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.config.VeinConfig;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.civbuddy.ui.Inputs.*;
import static com.civbuddy.ui.Layout.*;

public class VeinConfigMenu extends BaseOwoScreen<FlowLayout> {

    private final Screen parent;

    public VeinConfigMenu(Screen parent) {
        this.parent = parent;
    }

    private VeinConfig config() {
        return CivBuddyClient.config.get().veins;
    }

    private void update(JsonConfig.ConfigUpdate<GlobalConfig> func) {
        CivBuddyClient.config.updateAndSave(func);
        VeinClient.notifyChange();
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root
                .surface(Surface.optionsBackground())
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(20));


        root.child(
                UIComponents.label(Component.literal("Vein Settings").withStyle(ChatFormatting.BOLD))
                        .shadow(true)
                        .margins(Insets.bottom(8))
        );

        var options = UIContainers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        options.gap(6);

        root.child(
                UIContainers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.expand(),
                        options
                )
        );

        general(options);
        controls(options);
        display(options);

        options.child(UIContainers.horizontalFlow(Sizing.fixed(100), Sizing.fixed(100)));

        root.child(
                UIComponents.button(
                        Component.literal("Done"),
                        button -> onClose()
                ).horizontalSizing(Sizing.fixed(200))
        );
    }

    public void general(FlowLayout options) {
        header(options, "General");

        options.child(row("Use Veins", toggleButton(() -> config().doRender, "Enabled", "Disabled", v -> update(c -> c.veins.doRender = v))));

        var shapes = List.of(VeinConfig.ShapeMode.Cuboid.name(), VeinConfig.ShapeMode.Spheroid.name());
        options.child(row("Marking Shape", enumButton(() -> config().shapeMode.name(), shapes, newValue -> {
            update(c -> c.veins.shapeMode = VeinConfig.ShapeMode.valueOf(newValue));
        })));
    }

    public void controls(FlowLayout options) {
        VeinConfig config = config();

        header(options, "Controls");

        options.child(row("Mark Radius", vector3iInput(() -> config.markRange, v -> {
            if (!Objects.equals(v.absolute(), v)) return;

            update(c -> c.veins.markRange = v);
        })));

        options.child(row("Place Delay Ticks", numberInput(() -> config.placeDelayTicks, value -> {
            if (value <= 1) return;

            update(c -> c.veins.placeDelayTicks = value.intValue());
        })));

        options.child(row("Max Place Range", numberInput(() -> config.placeRange, value -> {
            if (value <= 1) return;

            update(c -> c.veins.placeRange = value.floatValue());
        })));

        options.child(row("Place Move Speed", numberInput(() -> config.placeMoveSpeed, value -> {
            if (value <= 0) return;

            update(c -> c.veins.placeMoveSpeed = value.floatValue());
        })));
    }

    public void display(FlowLayout options) {
        header(options, "Display");

        var columns = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        columns.child(renderer("Border", () -> config().border, v -> {
            update(c -> c.veins.border = v);
        }).sizing(Sizing.fill(33), Sizing.content()));
        columns.child(renderer("Marking", () -> config().marking, v -> {
            update(c -> c.veins.marking = v);
        }).sizing(Sizing.fill(33), Sizing.content()));
        columns.child(renderer("Highlight", () -> config().highlight, v -> {
            update(c -> c.veins.highlight = v);
        }).sizing(Sizing.fill(33), Sizing.content()));

        options.child(columns);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
