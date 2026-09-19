package com.civbuddy.ui;

import com.civbuddy.commands.ui.CommandManagerScreen;
import com.civbuddy.veins.ui.VeinConfigMenu;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ConfigMenu extends BaseOwoScreen<FlowLayout> {
    private final Screen parent;

    public ConfigMenu(Screen parent) {
        this.parent = parent;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .gap(5)
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(10))
                .surface(Surface.optionsBackground());

        rootComponent.child(
                UIComponents.button(
                        Component.literal("Commands"),
                        button -> minecraft.setScreen(new CommandManagerScreen(this))
                )
        );

        rootComponent.child(
                UIComponents.button(
                        Component.literal("Veins"),
                        button -> minecraft.setScreen(new VeinConfigMenu(this))
                )
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
