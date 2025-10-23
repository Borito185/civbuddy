package com.civbuddy.commands;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class AddCategoryScreen extends Screen {
    private final BookmarkScreen parent;
    private final BookmarkCategory editingCategory; // null if adding new
    private TextFieldWidget nameField;
    private int selectedColor = 0xFFFFFF;

    public AddCategoryScreen(BookmarkScreen parent) {
        this(parent, null);
    }

    public AddCategoryScreen(BookmarkScreen parent, BookmarkCategory editingCategory) {
        super(Text.literal(editingCategory == null ? "Add Category" : "Edit Category"));
        this.parent = parent;
        this.editingCategory = editingCategory;
        if (editingCategory != null) {
            this.selectedColor = editingCategory.getColor();
        }
    }

    @Override
    protected void init() {
        nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 40, 200, 20, Text.literal(""));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.literal("Category name..."));
        if (editingCategory != null) {
            nameField.setText(editingCategory.getName());
        }
        this.addDrawableChild(nameField);
        this.setInitialFocus(nameField);

        // Save and cancel buttons
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            if (!nameField.getText().isEmpty()) {
                if (editingCategory != null) {
                    // Edit existing
                    editingCategory.setName(nameField.getText());
                    editingCategory.setColor(selectedColor);
                } else {
                    // Add new
                    BookmarkCategory category = new BookmarkCategory(nameField.getText(), selectedColor);
                    BookmarkManager.getInstance().addCategory(category);
                }
                BookmarkManager.getInstance().saveBookmarks();
                this.close();
            }
        }).dimensions(this.width / 2 - 60, this.height / 2 + 10, 55, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> this.close())
                .dimensions(this.width / 2 + 5, this.height / 2 + 10, 55, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xE0101010, 0xE0101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}