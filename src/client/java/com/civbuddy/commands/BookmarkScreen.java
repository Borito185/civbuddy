package com.civbuddy.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class BookmarkScreen extends Screen {
    private final Screen parent;
    public BookmarkListWidget leftList;
    private CommandListWidget rightList;
    private Button addButton;
    private Button deleteButton;
    private Button editButton;
    private EditBox searchField;

    private BookmarkCategory selectedCategory;
    public BookmarkEntry selectedCommand;

    public BookmarkScreen(Screen parent) {
        super(Component.literal("CivBuddy Command Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Search field - positioned better
        searchField = new EditBox(this.font, 195, 8, 200, 16, Component.literal(""));
        searchField.setMaxLength(64);
        searchField.setHint(Component.literal("Search all commands..."));
        searchField.setResponder(text -> {
            refreshRightList();
        });
        this.addRenderableWidget(searchField);

        // Initialize left list (bookmarks/categories) - 25px height
        leftList = new BookmarkListWidget(
                this.minecraft,
                150,
                this.height - 80,
                55,
                this.height - 30,
                25,
                this
        );
        leftList.setX(10);

        // Initialize right list (commands) - custom widget aligned with left list
        rightList = new CommandListWidget(
                170,
                55,
                this.width - 170,
                this.height - 80, // Match left list height (height - 80 - 0 = height - 80)
                this
        );

        // Load and refresh
        BookmarkManager.getInstance().loadBookmarks();
        refreshLeftList();

        // Add button
        addButton = Button.builder(Component.literal("+ Add"), button -> openAddDialog())
                .bounds(10, 5, 55, 20)
                .build();

        // Edit button
        editButton = Button.builder(Component.literal("Edit"), button -> openEditDialog())
                .bounds(70, 5, 50, 20)
                .build();

        // Delete button
        deleteButton = Button.builder(Component.literal("Delete"), button -> deleteSelected())
                .bounds(125, 5, 60, 20)
                .build();

        // Close button
        Button closeButton = Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 25, 100, 20)
                .build();

        this.addRenderableWidget(addButton);
        this.addRenderableWidget(editButton);
        this.addRenderableWidget(deleteButton);
        this.addRenderableWidget(closeButton);
        this.addRenderableWidget(leftList);
        this.addRenderableWidget(rightList);
    }

    public void refreshLeftList() {
        leftList.children().clear();
        for (BookmarkCategory category : BookmarkManager.getInstance().getCategories()) {
            leftList.children().add(new BookmarkListWidget.CategoryEntry(leftList, category, this));
        }
    }

    void refreshRightList() {
        rightList.clearEntries();
        String searchText = searchField != null ? searchField.getValue().toLowerCase() : "";

        // If there's search text, always do global search
        if (!searchText.isEmpty()) {
            java.util.Set<String> seenCommands = new java.util.HashSet<>();

            for (BookmarkCategory category : BookmarkManager.getInstance().getCategories()) {
                for (BookmarkEntry entry : category.getEntries()) {
                    String cmd = entry.getCommand();
                    // Only add if matches search and not already seen (no duplicates)
                    if (cmd.toLowerCase().contains(searchText) && !seenCommands.contains(cmd)) {
                        rightList.addEntry(new CommandListWidget.CommandEntry(entry, category));
                        seenCommands.add(cmd);
                    }
                }
            }
        } else if (selectedCategory != null) {
            // Show commands from selected category only when no search
            java.util.Set<String> seenCommands = new java.util.HashSet<>();

            for (BookmarkEntry entry : selectedCategory.getEntries()) {
                String cmd = entry.getCommand();
                // Only add if not already seen (no duplicates)
                if (!seenCommands.contains(cmd)) {
                    rightList.addEntry(new CommandListWidget.CommandEntry(entry, selectedCategory));
                    seenCommands.add(cmd);
                }
            }
        }
    }

    public void selectCategory(BookmarkCategory category) {
        this.selectedCategory = category;
        this.selectedCommand = null;
        refreshRightList();
        updateButtonStates();
    }

    public void selectCommand(BookmarkEntry entry) {
        this.selectedCommand = entry;
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasCommandSelected = selectedCommand != null;
        boolean hasCategorySelected = selectedCategory != null;

        editButton.active = hasCommandSelected || hasCategorySelected;
        deleteButton.active = hasCommandSelected || hasCategorySelected;
    }

    private void openAddDialog() {
        if (selectedCategory != null) {
            // Prevent adding to History category
            if (selectedCategory.getName().equals("History")) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal("§cCannot add commands to History!"), false);
                }
                return;
            }

            // Always clear command selection when adding
            selectedCommand = null;
            updateButtonStates();
            // Add command to selected bookmark
            Minecraft.getInstance().setScreen(new AddCommandScreen(this, selectedCategory, null));
        } else {
            // No bookmark selected, add new bookmark
            Minecraft.getInstance().setScreen(new AddCategoryScreen(this));
        }
    }

    private void openEditDialog() {
        if (selectedCommand != null && selectedCategory != null) {
            // Don't allow editing history commands
            if (selectedCategory.getName().equals("History")) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal("§cCannot edit History commands!"), false);
                }
                return;
            }
            Minecraft.getInstance().setScreen(new AddCommandScreen(this, selectedCategory, selectedCommand));
        } else if (selectedCategory != null) {
            // Edit the category itself
            if (selectedCategory.getName().equals("History")) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal("§cCannot edit " + selectedCategory.getName() + " category!"), false);
                }
                return;
            }
            Minecraft.getInstance().setScreen(new AddCategoryScreen(this, selectedCategory));
        }
    }

    private void deleteSelected() {
        if (selectedCommand != null && selectedCategory != null) {
            // Don't allow deleting commands from protected categories
            if (selectedCategory.getName().equals("History")) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal("§cCannot delete commands from " + selectedCategory.getName() + "!"), false);
                }
                return;
            }
            selectedCategory.removeEntry(selectedCommand);
            BookmarkManager.getInstance().saveBookmarks();
            refreshRightList();
        } else if (selectedCategory != null) {
            // Prevent deletion of History and Destinations
            String categoryName = selectedCategory.getName();
            if (categoryName.equals("History")) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal("§cCannot delete " + categoryName + " category!"), false);
                }
                return;
            }

            BookmarkManager.getInstance().removeCategory(selectedCategory);
            BookmarkManager.getInstance().saveBookmarks();
            selectedCategory = null;
            refreshLeftList();
            refreshRightList();
        }
    }

    public void executeCommand(BookmarkEntry entry) {
        if (entry != null && minecraft != null && minecraft.player != null) {
            String command = entry.getCommand();
            if (command.startsWith("/")) {
                minecraft.player.connection.sendCommand(command.substring(1));
            } else {
                minecraft.player.connection.sendChat(command);
            }

            BookmarkManager.getInstance().addToHistory(entry);
            this.onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on the left list area but not on an entry
        if (button == 0 && mouseX >= 10 && mouseX <= 160 && mouseY >= 55) {
            boolean clickedOnEntry = false;
            for (BookmarkListWidget.CategoryEntry entry : leftList.children()) {
                if (entry.isMouseOver(mouseX, mouseY)) {
                    clickedOnEntry = true;
                    break;
                }
            }
            if (!clickedOnEntry) {
                // Clicked in left list area but not on an entry - deselect
                selectedCategory = null;
                selectedCommand = null;
                refreshRightList();
                updateButtonStates();
                leftList.setSelected(null);
                return true;
            }
        }

        // Call super to handle widgets
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fillGradient(0, 0, this.width, this.height, 0xE0101010, 0xE0101010);

        // Render widgets
        super.render(context, mouseX, mouseY, delta);

        // Draw labels on top in BRIGHT WHITE
        context.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawString(this.font, "Bookmarks", 15, 35, 0xFFFFFF);
        context.drawString(this.font, "Commands", 175, 35, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}