package com.civbuddy.commands.ui;

import com.civbuddy.commands.data.CommandCategory;
import com.civbuddy.commands.data.CommandManager;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AddCategoryScreen extends SpruceScreen {
    private final CommandManagerScreen parent;
    private final CommandCategory editing;
    private SpruceTextFieldWidget nameField;

    public AddCategoryScreen(CommandManagerScreen parent) {
        this(parent, null);
    }

    public AddCategoryScreen(CommandManagerScreen parent, CommandCategory editing) {
        super(Component.literal(editing == null ? "Add Category" : "Edit Category"));
        this.parent = parent;
        this.editing = editing;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        nameField = new SpruceTextFieldWidget(
                Position.of(cx - 100, cy - 20), 200, 20,
                Component.literal("Name"), Component.literal("Category name...")
        );
        if (editing != null) nameField.setText(editing.getName());
        this.addRenderableWidget(nameField);
        this.setInitialFocus(nameField);

        this.addRenderableWidget(new SpruceButtonWidget(
                Position.of(cx - 60, cy + 10), 55, 20,
                Component.literal("Save"), btn -> save()
        ));
        this.addRenderableWidget(new SpruceButtonWidget(
                Position.of(cx + 5, cy + 10), 55, 20,
                Component.literal("Cancel"), btn -> this.onClose()
        ));
    }

    private void save() {
        String name = nameField.getText().trim();
        if (!name.isEmpty()) {
            if (editing != null) {
                editing.setName(name);
            } else {
                CommandManager.getInstance().addCategory(new CommandCategory(name, 0xFFFFFF));
            }
            CommandManager.getInstance().saveCommands();
            this.onClose();
        }
    }

    @Override
    public void render(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderWidgets(graphics, mouseX, mouseY, delta);
        graphics.vanilla().drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 45, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
