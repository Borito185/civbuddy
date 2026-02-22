package com.civbuddy.commands.ui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Generic single-field add/edit dialog used for both categories and commands.
 */
public class AddEditScreen extends BaseOwoScreen<FlowLayout> {
    private final Screen parent;
    private final String fieldDefault;
    private final int maxLength;
    private final Consumer<String> onSave;

    public AddEditScreen(Screen parent, String title, String fieldDefault, int maxLength, Consumer<String> onSave) {
        super(Component.literal(title));
        this.parent = parent;
        this.fieldDefault = fieldDefault != null ? fieldDefault : "";
        this.maxLength = maxLength;
        this.onSave = onSave;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, (BiFunction<Sizing, Sizing, FlowLayout>) UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        root.horizontalAlignment(HorizontalAlignment.CENTER);
        root.verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(260), Sizing.content());
        panel.surface(Surface.PANEL);
        panel.padding(Insets.of(10));
        panel.horizontalAlignment(HorizontalAlignment.CENTER);
        panel.gap(8);

        LabelComponent titleLabel = UIComponents.label(this.title);
        titleLabel.shadow(true);
        panel.child(titleLabel);

        TextBoxComponent field = UIComponents.textBox(Sizing.fixed(240));
        field.setMaxLength(maxLength);
        field.id("inputField");
        if (!fieldDefault.isEmpty()) field.text(fieldDefault);
        panel.child(field);

        FlowLayout buttonRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        buttonRow.gap(10);

        ButtonComponent saveBtn = UIComponents.button(Component.literal("Save"), btn -> save());
        saveBtn.horizontalSizing(Sizing.fixed(55));
        buttonRow.child(saveBtn);

        ButtonComponent cancelBtn = UIComponents.button(Component.literal("Cancel"), btn -> this.onClose());
        cancelBtn.horizontalSizing(Sizing.fixed(55));
        buttonRow.child(cancelBtn);

        panel.child(buttonRow);
        root.child(panel);
    }

    private void save() {
        TextBoxComponent field = this.component(TextBoxComponent.class, "inputField");
        String value = field.getValue().trim();
        if (!value.isEmpty()) {
            onSave.accept(value);
            this.onClose();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
