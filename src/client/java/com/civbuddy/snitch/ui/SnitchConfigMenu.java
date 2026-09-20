package com.civbuddy.snitch.ui;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.snitch.SnitchClient;
import com.civbuddy.snitch.config.SnitchConfig;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.civbuddy.ui.Inputs.*;
import static com.civbuddy.ui.Layout.*;
import static io.wispforest.owo.ui.component.UIComponents.button;

public class SnitchConfigMenu extends BaseOwoScreen<FlowLayout> {
    private final Screen parent;

    public SnitchConfigMenu(Screen parent) {
        this.parent = parent;
    }

    private SnitchConfig config() {
        return CivBuddyClient.config.get().snitch;
    }

    private void update(Consumer<SnitchConfig> func) {
        CivBuddyClient.config.updateAndSave(c -> func.accept(c.snitch));
        SnitchClient.notifyChange();
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
                UIComponents.label(Component.literal("Snitch Inspect Settings").withStyle(ChatFormatting.BOLD))
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

        // menu
        general(options);
        colors(options);

        options.child(UIContainers.horizontalFlow(Sizing.fixed(100), Sizing.fixed(100)));

        root.child(
                button(
                        Component.literal("Done"),
                        button -> onClose()
                ).horizontalSizing(Sizing.fixed(200))
        );
    }

    private void general(FlowLayout options) {
        header(options, "General");
        options.child(row("Toggle Enabled", toggleButton(
                () -> config().enabled,
                "Enabled",
                "Disabled",
                v -> update(c -> c.enabled=v)))
        );
        options.child(row("Filter by Highlights", toggleButton(
                () -> SnitchClient.filterByPositions,
                "Enabled",
                "Disabled",
                v -> {
                    SnitchClient.filterByPositions = !SnitchClient.filterByPositions;
                    SnitchClient.notifyChange();
                }))
        );
        options.child(renderer("Highlight", () -> config().highlight, v -> update(c -> c.highlight = v)));
        options.child(button(Component.literal("Clear Highlights"), btn -> {
            SnitchClient.positions.clear();
            SnitchClient.notifyChange();
            btn.active(false);
        }));
    }

    private void colors(FlowLayout options) {
        header(options, "JA Event Coloring");

        var grid = UIContainers.grid(
                Sizing.content(),
                Sizing.content(),
                3,
                3
        );

        options.child(grid);

        colorCell(grid, 0, 0, "Killed",
                () -> config().killedColor,
                v -> update(c -> c.killedColor = v));

        colorCell(grid, 0, 1, "Break",
                () -> config().breakColor,
                v -> update(c -> c.breakColor = v));

        colorCell(grid, 0, 2, "Place",
                () -> config().placeColor,
                v -> update(c -> c.placeColor = v));

        colorCell(grid, 1, 0, "Enter",
                () -> config().enterColor,
                v -> update(c -> c.enterColor = v));

        colorCell(grid, 1, 1, "Leave",
                () -> config().leaveColor,
                v -> update(c -> c.leaveColor = v));

        colorCell(grid, 1, 2, "Opened",
                () -> config().openedColor,
                v -> update(c -> c.openedColor = v));

        colorCell(grid, 2, 0, "Login",
                () -> config().loginColor,
                v -> update(c -> c.loginColor = v));

        colorCell(grid, 2, 1, "Logout",
                () -> config().logoutColor,
                v -> update(c -> c.logoutColor = v));

        colorCell(grid, 2, 2, "Item Exchange",
                () -> config().itemExchangeColor,
                v -> update(c -> c.itemExchangeColor = v));
    }

    private void colorCell(
            GridLayout grid,
            int row,
            int column,
            String name,
            Supplier<Vector4f> value,
            Consumer<Vector4f> onChanged
    ) {
        var layout = UIContainers.verticalFlow(
                Sizing.content(),
                Sizing.content()
        );

        layout.child(label(name));
        layout.child(colorInput(value, onChanged));

        grid.child(layout, row, column);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
