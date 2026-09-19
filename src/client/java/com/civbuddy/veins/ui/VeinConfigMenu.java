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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.util.List;
import java.util.Objects;

import static com.civbuddy.ui.Inputs.*;

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
                UIComponents.label(Component.literal("Vein Settings"))
        );

        var options = UIContainers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        options.gap(4);

        root.child(
                UIContainers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.fill(100),
                        options
                )
        );

        general(options);
        controls(options);
        display(options);

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
        VeinConfig config = config();

        header(options, "Display");

        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());

        FlowLayout col = UIContainers.verticalFlow(Sizing.fill(33), Sizing.content());
        FlowLayout head = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        head.child(UIComponents.label(Component.literal("Border")));
        head.child(toggleButton(() -> config.borderHasGrid, "Grid", "No Grid", b -> {
            update(c -> c.veins.borderHasGrid = b);
        }));
        col.child(head);
        col.child(colorInput(() -> config.borderWallColor, v -> {
            update(c -> c.veins.borderWallColor = v);
        }));
        row.child(col);

        col = UIContainers.verticalFlow(Sizing.fill(33), Sizing.content());
        head = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        head.child(UIComponents.label(Component.literal("Marking")));
        head.child(toggleButton(() -> config.markingHasGrid, "Grid", "No Grid", b -> {
            update(c -> c.veins.markingHasGrid = b);
        }));
        col.child(head);
        col.child(colorInput(() -> config.markingWallColor, v -> {
            update(c -> c.veins.markingWallColor = v);
        }));
        row.child(col);

        col = UIContainers.verticalFlow(Sizing.fill(33), Sizing.content());
        head = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        head.child(UIComponents.label(Component.literal("Highlight")));
        head.child(toggleButton(() -> config.highlightHasGrid, "Grid", "No Grid", b -> {
            update(c -> c.veins.highlightHasGrid = b);
        }));
        col.child(head);
        col.child(colorInput(() -> config.highlightWallColor, v -> {
            update(c -> c.veins.highlightWallColor = v);
        }));
        row.child(col);
        options.child(row);
    }

    private UIComponent seperator() {
        return UIComponents.label(Component.literal(""));
    }

    private void header(FlowLayout options, String text) {
        options.child(UIComponents.label(Component.literal(text)));
    }

    private FlowLayout row(String name, UIComponent control) {
        var row = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        row.verticalAlignment(VerticalAlignment.CENTER);

        row.child(UIComponents.label(Component.literal(name)));
        row.child(control);

        return row;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}