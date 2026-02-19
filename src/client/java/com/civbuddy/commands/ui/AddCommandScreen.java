package com.civbuddy.commands.ui;

import com.civbuddy.commands.data.CommandCategory;
import com.civbuddy.commands.data.CommandEntry;
import com.civbuddy.commands.data.CommandManager;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AddCommandScreen extends SpruceScreen {
    private final CommandManagerScreen parent;
    private final CommandCategory category;
    private final CommandEntry editing;
    private SpruceTextFieldWidget commandField;

    public AddCommandScreen(CommandManagerScreen parent, CommandCategory category) {
        this(parent, category, null);
    }

    public AddCommandScreen(CommandManagerScreen parent, CommandCategory category, CommandEntry editing) {
        super(Component.literal(editing == null ? "Add Command" : "Edit Command"));
        this.parent = parent;
        this.category = category;
        this.editing = editing;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        commandField = new SpruceTextFieldWidget(
                Position.of(cx - 120, cy - 20), 240, 20,
                Component.literal("Command"), Component.literal("/command or chat message...")
        );
        if (editing != null) commandField.setText(editing.getCommand());
        this.addRenderableWidget(commandField);
        this.setInitialFocus(commandField);

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
        String cmd = commandField.getText().trim();
        if (!cmd.isEmpty()) {
            if (editing != null) {
                editing.setCommand(cmd);
                editing.setName(cmd);
            } else {
                category.addEntry(new CommandEntry(cmd, cmd));
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
