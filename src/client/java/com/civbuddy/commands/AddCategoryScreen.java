package com.civbuddy.commands;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class AddCategoryScreen extends Screen {
    private final BookmarkScreen parent;
    private final BookmarkCategory editingCategory; // null if adding new
    private EditBox nameField;
    private int selectedColor = 0xFFFFFF;

    public AddCategoryScreen(BookmarkScreen parent) {
        this(parent, null);
    }

    public AddCategoryScreen(BookmarkScreen parent, BookmarkCategory editingCategory) {
        super(Component.literal(editingCategory == null ? "Add Category" : "Edit Category"));
        this.parent = parent;
        this.editingCategory = editingCategory;
        if (editingCategory != null) {
            this.selectedColor = editingCategory.getColor();
        }
    }

    @Override
    protected void init() {
        nameField = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 40, 200, 20, Component.literal(""));
        nameField.setMaxLength(32);
        nameField.setHint(Component.literal("Category name..."));
        if (editingCategory != null) {
            nameField.setValue(editingCategory.getName());
        }
        this.addRenderableWidget(nameField);
        this.setInitialFocus(nameField);

        // Save and cancel buttons
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            if (!nameField.getValue().isEmpty()) {
                if (editingCategory != null) {
                    // Edit existing
                    editingCategory.setName(nameField.getValue());
                    editingCategory.setColor(selectedColor);
                } else {
                    // Add new
                    BookmarkCategory category = new BookmarkCategory(nameField.getValue(), selectedColor);
                    BookmarkManager.getInstance().addCategory(category);
                }
                BookmarkManager.getInstance().saveBookmarks();
                this.onClose();
            }
        }).bounds(this.width / 2 - 60, this.height / 2 + 10, 55, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
                .bounds(this.width / 2 + 5, this.height / 2 + 10, 55, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xE0101010, 0xE0101010);
        context.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}