package com.civbuddy.commands.sceens;


import com.civbuddy.commands.models.CommandGroup;
import com.civbuddy.commands.models.Command;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import static com.civbuddy.commands.CommandClient.COMMAND_CLIENT;

public class CommandInputScreen extends Screen {
    private final BookmarkScreen parent;
    private final CommandGroup category;
    private final Command editingEntry; // null if adding new
    private TextFieldWidget commandField;

    public CommandInputScreen(BookmarkScreen parent, CommandGroup category, Command editingEntry) {
        super(Text.literal(editingEntry == null ? "Add Command" : "Edit Command"));
        this.parent = parent;
        this.category = category;
        this.editingEntry = editingEntry;
    }

    @Override
    protected void init() {
        commandField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Text.literal(""));
        commandField.setMaxLength(256);
        commandField.setPlaceholder(Text.literal("/command or message..."));
        if (editingEntry != null) {
            commandField.setText(editingEntry.getCommand());
        }
        this.addDrawableChild(commandField);
        this.setInitialFocus(commandField);

        // Save and cancel buttons
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            if (commandField.getText().isBlank()) {
                return;
            }
            if (editingEntry != null) {
                // Edit existing
                editingEntry.setCommand(commandField.getText());
            } else {
                // Add new - use command as name too
                Command entry = new Command(commandField.getText());
                category.addEntry(entry);
            }
            COMMAND_CLIENT.save();
            parent.focusOn(editingEntry);
            this.close();
        }).dimensions(this.width / 2 - 60, this.height / 2 + 20, 55, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> this.close())
                .dimensions(this.width / 2 + 5, this.height / 2 + 20, 55, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xE0101010, 0xE0101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Command:", this.width / 2 - 100, this.height / 2 - 32, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
