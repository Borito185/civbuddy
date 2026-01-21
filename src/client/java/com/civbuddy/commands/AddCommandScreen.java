package com.civbuddy.commands;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class AddCommandScreen extends Screen {
    private final BookmarkScreen parent;
    private final BookmarkCategory category;
    private final BookmarkEntry editingEntry; // null if adding new
    private EditBox commandField;

    public AddCommandScreen(BookmarkScreen parent, BookmarkCategory category, BookmarkEntry editingEntry) {
        super(Component.literal(editingEntry == null ? "Add Command" : "Edit Command"));
        this.parent = parent;
        this.category = category;
        this.editingEntry = editingEntry;
    }

    @Override
    protected void init() {
        commandField = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal(""));
        commandField.setMaxLength(256);
        commandField.setHint(Component.literal("/command or message..."));
        if (editingEntry != null) {
            commandField.setValue(editingEntry.getCommand());
        }
        this.addRenderableWidget(commandField);
        this.setInitialFocus(commandField);

        // Save and cancel buttons
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            if (!commandField.getValue().isEmpty()) {
                if (editingEntry != null) {
                    // Edit existing
                    editingEntry.setCommand(commandField.getValue());
                } else {
                    // Add new - use command as name too
                    BookmarkEntry entry = new BookmarkEntry(commandField.getValue(), commandField.getValue());
                    category.addEntry(entry);
                }
                BookmarkManager.getInstance().saveBookmarks();
                parent.selectCategory(null); // Deselect category (also deselects command)
                this.onClose();
            }
        }).bounds(this.width / 2 - 60, this.height / 2 + 20, 55, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
                .bounds(this.width / 2 + 5, this.height / 2 + 20, 55, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xE0101010, 0xE0101010);
        context.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        context.drawString(this.font, "Command:", this.width / 2 - 100, this.height / 2 - 32, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
