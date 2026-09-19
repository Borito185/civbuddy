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
import org.joml.Vector4f;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        header(options, "Display");

        var columns = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        columns.gap(8);

        columns.child(displayColumn(
                "Border",
                () -> config().borderHasGrid,
                value -> update(c -> c.veins.borderHasGrid = value),
                () -> config().borderWallColor,
                value -> update(c -> c.veins.borderWallColor = value)
        ));

        columns.child(displayColumn(
                "Marking",
                () -> config().markingHasGrid,
                value -> update(c -> c.veins.markingHasGrid = value),
                () -> config().markingWallColor,
                value -> update(c -> c.veins.markingWallColor = value)
        ));

        columns.child(displayColumn(
                "Highlight",
                () -> config().highlightHasGrid,
                value -> update(c -> c.veins.highlightHasGrid = value),
                () -> config().highlightWallColor,
                value -> update(c -> c.veins.highlightWallColor = value)
        ));

        options.child(columns);
    }

    private void header(FlowLayout options, String text) {
        options.child(
                UIComponents.label(
                                Component.literal(text)
                                        .withStyle(ChatFormatting.BOLD)
                        )
                        .shadow(true)
                        .margins(Insets.top(12).withBottom(4))
                        .horizontalSizing(Sizing.fill(100))
        );
    }

    private FlowLayout row(String name, UIComponent control) {
        var row = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(8);

        var labelArea = UIContainers.horizontalFlow(
                Sizing.fill(30),
                Sizing.content()
        );

        labelArea.verticalAlignment(VerticalAlignment.CENTER);
        labelArea.child(UIComponents.label(Component.literal(name)));

        var controlArea = UIContainers.horizontalFlow(
                Sizing.fill(70),
                Sizing.content()
        );

        control.horizontalSizing(Sizing.fill(100));

        controlArea.verticalAlignment(VerticalAlignment.CENTER);
        controlArea.child(control);

        row.child(labelArea);
        row.child(controlArea);

        return row;
    }

    private FlowLayout displayColumn(
            String name,
            Supplier<Boolean> grid,
            Consumer<Boolean> setGrid,
            Supplier<Vector4f> color,
            Consumer<Vector4f> setColor
    ) {
        var column = UIContainers.verticalFlow(
                Sizing.fill(33),
                Sizing.content()
        );

        column
                .gap(6)
                .padding(Insets.of(6));

        var header = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        header.verticalAlignment(VerticalAlignment.CENTER);
        header.gap(6);

        header.child(
                UIComponents.label(
                                Component.literal(name)
                        )
                        .horizontalSizing(Sizing.expand())
        );

        header.child(
                toggleButton(
                        grid,
                        "Grid",
                        "No Grid",
                        setGrid
                ).horizontalSizing(Sizing.fixed(70))
        );

        column.child(header);

        var picker = colorInput(color, setColor);
        picker.horizontalSizing(Sizing.fill(100));

        column.child(picker);

        return column;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
