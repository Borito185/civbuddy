package com.civbuddy.common.ui;

import com.civbuddy.commands.ui.CommandManagerScreen;
import com.civbuddy.snitch.ui.SnitchConfigMenu;
import com.civbuddy.veins.ui.VeinConfigMenu;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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

        rootComponent.child(UIComponents.label(Component.literal("CivBuddy").withStyle(ChatFormatting.BOLD)).shadow(true));

        rootComponent.child(
                UIComponents.button(
                        Component.literal("Vein Settings"),
                        button -> minecraft.setScreen(new VeinConfigMenu(this))
                ).sizing(Sizing.fill(40), Sizing.content())
        );

        rootComponent.child(
                UIComponents.button(
                        Component.literal("Snitch Inspect Settings"),
                        button -> minecraft.setScreen(new SnitchConfigMenu(this))
                ).sizing(Sizing.fill(40), Sizing.content())
        );

        rootComponent.child(
                UIComponents.button(
                        Component.literal("Commands"),
                        button -> minecraft.setScreen(new CommandManagerScreen(this))
                ).active(Minecraft.getInstance().level != null).sizing(Sizing.fill(40), Sizing.content())
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
