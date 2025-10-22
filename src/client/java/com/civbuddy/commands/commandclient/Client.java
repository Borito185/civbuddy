package com.civbuddy.commands.commandclient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class Client {
    
    // ========== INITIALIZATION ==========
    public static void initialize() {
        registerKeybinding();
        BookmarkManager.getInstance().loadBookmarks();
    }
    
    private static void registerKeybinding() {
        KeyBinding openBookmarkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.civbuddy.open_bookmarks",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            "category.civbuddy"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBookmarkKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new BookmarkScreen(null));
            }
        });
    }
    
    // ========== BOOKMARK SCREEN ==========
    public static class BookmarkScreen extends Screen {
        private final Screen parent;
        private BookmarkListWidget leftList;
        private CommandListWidget rightList;
        private ButtonWidget addButton;
        private ButtonWidget deleteButton;
        private ButtonWidget editButton;
        private TextFieldWidget searchField;
        
        private BookmarkCategory selectedCategory;
        private BookmarkEntry selectedCommand;

        public BookmarkScreen(Screen parent) {
            super(Text.literal("CivBuddy Command Manager"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            // Search field - positioned better
            searchField = new TextFieldWidget(this.textRenderer, 195, 8, 200, 16, Text.literal(""));
            searchField.setMaxLength(64);
            searchField.setPlaceholder(Text.literal("Search all commands..."));
            searchField.setChangedListener(text -> {
                refreshRightList();
            });
            this.addDrawableChild(searchField);
            
            // Initialize left list (bookmarks/categories) - 25px height
            leftList = new BookmarkListWidget(
                this.client, 
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
            addButton = ButtonWidget.builder(Text.literal("+ Add"), button -> openAddDialog())
                .dimensions(10, 5, 55, 20)
                .build();
            
            // Edit button
            editButton = ButtonWidget.builder(Text.literal("Edit"), button -> openEditDialog())
                .dimensions(70, 5, 50, 20)
                .build();
            
            // Delete button
            deleteButton = ButtonWidget.builder(Text.literal("Delete"), button -> deleteSelected())
                .dimensions(125, 5, 60, 20)
                .build();
            
            // Close button
            ButtonWidget closeButton = ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 25, 100, 20)
                .build();
            
            this.addDrawableChild(addButton);
            this.addDrawableChild(editButton);
            this.addDrawableChild(deleteButton);
            this.addDrawableChild(closeButton);
            this.addDrawableChild(leftList);
            this.addDrawableChild(rightList);
        }

        private void refreshLeftList() {
            leftList.children().clear();
            for (BookmarkCategory category : BookmarkManager.getInstance().getCategories()) {
                leftList.children().add(new BookmarkListWidget.CategoryEntry(leftList, category, this));
            }
        }

        void refreshRightList() {
            rightList.clearEntries();
            String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
            
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
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§cCannot add commands to History!"), false);
                    }
                    return;
                }
                
                // Always clear command selection when adding
                selectedCommand = null;
                updateButtonStates();
                // Add command to selected bookmark
                MinecraftClient.getInstance().setScreen(new AddCommandScreen(this, selectedCategory, null));
            } else {
                // No bookmark selected, add new bookmark
                MinecraftClient.getInstance().setScreen(new AddCategoryScreen(this));
            }
        }

        private void openEditDialog() {
            if (selectedCommand != null && selectedCategory != null) {
                // Don't allow editing history commands
                if (selectedCategory.getName().equals("History")) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§cCannot edit History commands!"), false);
                    }
                    return;
                }
                MinecraftClient.getInstance().setScreen(new AddCommandScreen(this, selectedCategory, selectedCommand));
            } else if (selectedCategory != null) {
                // Edit the category itself
                if (selectedCategory.getName().equals("History") || selectedCategory.getName().equals("Destinations")) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§cCannot edit " + selectedCategory.getName() + " category!"), false);
                    }
                    return;
                }
                MinecraftClient.getInstance().setScreen(new AddCategoryScreen(this, selectedCategory));
            }
        }

        private void deleteSelected() {
            if (selectedCommand != null && selectedCategory != null) {
                // Don't allow deleting commands from protected categories
                if (selectedCategory.getName().equals("History") || selectedCategory.getName().equals("Destinations")) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§cCannot delete commands from " + selectedCategory.getName() + "!"), false);
                    }
                    return;
                }
                selectedCategory.removeEntry(selectedCommand);
                BookmarkManager.getInstance().saveBookmarks();
                refreshRightList();
            } else if (selectedCategory != null) {
                // Prevent deletion of History and Destinations
                String categoryName = selectedCategory.getName();
                if (categoryName.equals("History") || categoryName.equals("Destinations")) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§cCannot delete " + categoryName + " category!"), false);
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
            if (entry != null && client != null && client.player != null) {
                String command = entry.getCommand();
                if (command.startsWith("/")) {
                    client.player.networkHandler.sendChatCommand(command.substring(1));
                } else {
                    client.player.networkHandler.sendChatMessage(command);
                }
                
                BookmarkManager.getInstance().addToHistory(entry);
                this.close();
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
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            // Dark background
            context.fillGradient(0, 0, this.width, this.height, 0xE0101010, 0xE0101010);
            
            // Render widgets
            super.render(context, mouseX, mouseY, delta);
            
            // Draw labels on top in BRIGHT WHITE
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "Bookmarks", 15, 35, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "Commands", 175, 35, 0xFFFFFF);
        }

        @Override
        public void close() {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
    
    // ========== BOOKMARK LIST WIDGET ==========
    public static class BookmarkListWidget extends AlwaysSelectedEntryListWidget<BookmarkListWidget.CategoryEntry> {
        private final BookmarkScreen parent;
        private CategoryEntry draggingEntry = null;

        public BookmarkListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight, BookmarkScreen parent) {
            super(client, width, height, top, itemHeight);
            this.parent = parent;
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                // Start dragging
                for (CategoryEntry entry : this.children()) {
                    if (entry.isMouseOver(mouseX, mouseY)) {
                        draggingEntry = entry;
                        break;
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0 && draggingEntry != null) {
                // Find where to drop
                int dropIndex = -1;
                for (int i = 0; i < this.children().size(); i++) {
                    CategoryEntry entry = this.children().get(i);
                    if (entry.isMouseOver(mouseX, mouseY)) {
                        dropIndex = i;
                        break;
                    }
                }
                
                if (dropIndex != -1 && draggingEntry != this.children().get(dropIndex)) {
                    // Reorder categories
                    List<BookmarkCategory> categories = BookmarkManager.getInstance().getCategories();
                    int fromIndex = categories.indexOf(draggingEntry.category);
                    int toIndex = categories.indexOf(this.children().get(dropIndex).category);
                    
                    if (fromIndex != -1 && toIndex != -1) {
                        BookmarkCategory moving = categories.remove(fromIndex);
                        categories.add(toIndex, moving);
                        BookmarkManager.getInstance().saveBookmarks();
                        parent.refreshLeftList();
                    }
                }
                
                draggingEntry = null;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        public static class CategoryEntry extends AlwaysSelectedEntryListWidget.Entry<CategoryEntry> {
            private final BookmarkListWidget widget;
            private final BookmarkCategory category;
            private final BookmarkScreen parent;

            public CategoryEntry(BookmarkListWidget widget, BookmarkCategory category, BookmarkScreen parent) {
                this.widget = widget;
                this.category = category;
                this.parent = parent;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, 
                              int mouseX, int mouseY, boolean hovered, float tickDelta) {
                MinecraftClient client = MinecraftClient.getInstance();
                
                // Use the actual entry height (25px)
                boolean isDragging = widget.draggingEntry == this;
                if (hovered || isDragging) {
                    context.fill(x, y, x + entryWidth, y + entryHeight, 0xFF0080FF); // BRIGHT BLUE
                } else {
                    context.fill(x, y, x + entryWidth, y + entryHeight, 0xFF404040); // MEDIUM GRAY
                }
                
                // Borders
                context.fill(x, y, x + entryWidth, y + 1, 0xFF888888);
                context.fill(x, y + entryHeight - 1, x + entryWidth, y + entryHeight, 0xFF888888);
                
                // Draw count on the right side (accounting for scrollbar)
                String countText = "(" + category.getEntries().size() + ")";
                int countWidth = client.textRenderer.getWidth(countText);
                int countX = x + entryWidth - countWidth - 12; // 12px padding from right (scrollbar space)
                context.drawTextWithShadow(client.textRenderer, countText, countX, y + 8, 0xFFAAAAAA);
                
                // Truncate name if needed to not overlap with count
                String name = category.getName();
                int maxNameWidth = entryWidth - countWidth - 20; // Space for count + padding + scrollbar
                String displayName = name;
                
                while (client.textRenderer.getWidth(displayName) > maxNameWidth && displayName.length() > 0) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                if (displayName.length() < name.length()) {
                    displayName = displayName.substring(0, Math.max(0, displayName.length() - 3)) + "...";
                }
                
                // Draw name on the left - BRIGHT WHITE text - centered for 25px height
                context.drawTextWithShadow(client.textRenderer, displayName, x + 5, y + 8, 0xFFFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    parent.selectCategory(category);
                    widget.setSelected(this);
                    return true;
                }
                return false;
            }

            @Override
            public Text getNarration() {
                return Text.literal(category.getName());
            }
        }
    }
    
    // ========== COMMAND LIST WIDGET ==========
    public static class CommandListWidget extends ClickableWidget {
        private final BookmarkScreen parent;
        private final List<CommandEntry> entries = new ArrayList<>();
        private CommandEntry draggingEntry;
        private long dragStartTime = 0;
        private static final int ENTRY_HEIGHT = 20; // Increased from 14 to 20 for more spacing
        private static final int VISUAL_HEIGHT = 16; // Actual box height
        private int scrollOffset = 0;
        private double dragMouseX = 0;
        private double dragMouseY = 0;

        public CommandListWidget(int x, int y, int width, int height, BookmarkScreen parent) {
            super(x, y, width, height, Text.literal("Commands"));
            this.parent = parent;
        }
        
        public void clearEntries() {
            entries.clear();
            scrollOffset = 0;
        }
        
        public void addEntry(CommandEntry entry) {
            entries.add(entry);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            // Track mouse position for drag preview
            dragMouseX = mouseX;
            dragMouseY = mouseY;
            
            // Dark background
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF202020);
            
            // Calculate visible entries
            int visibleEntries = this.height / ENTRY_HEIGHT;
            int maxScroll = Math.max(0, entries.size() - visibleEntries);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            
            // Render entries
            int y = this.getY();
            for (int i = scrollOffset; i < entries.size() && y < this.getY() + this.height; i++) {
                CommandEntry entry = entries.get(i);
                boolean isDragging = draggingEntry == entry;
                boolean isSelected = parent.selectedCommand == entry.entry;
                entry.render(context, this.getX(), y, this.width - 6, mouseX, mouseY, isDragging, isSelected); // -6 for scrollbar space
                y += ENTRY_HEIGHT;
            }
            
            // Render scrollbar if needed
            if (entries.size() > visibleEntries) {
                int scrollbarX = this.getX() + this.width - 6;
                int scrollbarHeight = this.height;
                int thumbHeight = Math.max(20, (visibleEntries * scrollbarHeight) / entries.size());
                int thumbY = this.getY() + (scrollOffset * (scrollbarHeight - thumbHeight)) / maxScroll;
                
                // Scrollbar track
                context.fill(scrollbarX, this.getY(), scrollbarX + 6, this.getY() + scrollbarHeight, 0xFF202020);
                
                // Scrollbar thumb
                context.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF808080);
            }
            
            // Render drag preview at cursor - small horizontal rectangle after 0.15 second delay
            if (draggingEntry != null) {
                long elapsedTime = System.currentTimeMillis() - dragStartTime;
                if (elapsedTime >= 150) { // 150ms = 0.15 seconds
                    int rectWidth = 40;
                    int rectHeight = 8;
                    int dragX = (int)dragMouseX - rectWidth / 2;
                    int dragY = (int)dragMouseY - rectHeight / 2;
                    
                    // Draw outline only (dark/gray)
                    context.fill(dragX, dragY, dragX + rectWidth, dragY + 1, 0xFF505050);
                    context.fill(dragX, dragY + rectHeight - 1, dragX + rectWidth, dragY + rectHeight, 0xFF505050);
                    context.fill(dragX, dragY, dragX + 1, dragY + rectHeight, 0xFF505050);
                    context.fill(dragX + rectWidth - 1, dragY, dragX + rectWidth, dragY + rectHeight, 0xFF505050);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isMouseOver(mouseX, mouseY)) {
                int relativeY = (int)(mouseY - this.getY());
                int index = scrollOffset + (relativeY / ENTRY_HEIGHT);
                
                if (index >= 0 && index < entries.size()) {
                    CommandEntry entry = entries.get(index);
                    
                    // Calculate where the :: marker is (right side of box)
                    int entryWidth = this.width - 6; // -6 for scrollbar space
                    int markerStartX = this.getX() + entryWidth - 20; // :: area is last 20 pixels
                    
                    // Check if in drop zone (:: area) - right side of the box
                    if (mouseX >= markerStartX) {
                        // In drop zone - select the command (for editing) and prepare for dragging
                        parent.selectCommand(entry.entry);
                        draggingEntry = entry;
                        dragStartTime = System.currentTimeMillis();
                        return true;
                    } else {
                        // Execute command
                        parent.executeCommand(entry.entry);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0 && draggingEntry != null) {
                boolean handled = false;
                
                // Check if dropped on a category
                for (BookmarkListWidget.CategoryEntry categoryEntry : parent.leftList.children()) {
                    if (categoryEntry.isMouseOver(mouseX, mouseY)) {
                        BookmarkCategory targetCategory = categoryEntry.category;
                        
                        // Prevent dropping on History category
                        if (targetCategory.getName().equals("History")) {
                            handled = true;
                            break;
                        }
                        
                        if (draggingEntry.sourceCategory != targetCategory) {
                            BookmarkEntry newEntry = new BookmarkEntry(draggingEntry.entry.getName(), draggingEntry.entry.getCommand());
                            targetCategory.addEntry(newEntry);
                            BookmarkManager.getInstance().saveBookmarks();
                            parent.refreshRightList();
                        }
                        handled = true;
                        break;
                    }
                }
                
                if (!handled && isMouseOver(mouseX, mouseY)) {
                    // Reorder within list
                    int relativeY = (int)(mouseY - this.getY());
                    int dropIndex = scrollOffset + (relativeY / ENTRY_HEIGHT);
                    
                    if (dropIndex >= 0 && dropIndex < entries.size()) {
                        CommandEntry target = entries.get(dropIndex);
                        if (draggingEntry != target && draggingEntry.sourceCategory == target.sourceCategory) {
                            BookmarkCategory category = draggingEntry.sourceCategory;
                            int fromIndex = category.getEntries().indexOf(draggingEntry.entry);
                            int toIndex = category.getEntries().indexOf(target.entry);
                            
                            if (fromIndex != -1 && toIndex != -1) {
                                BookmarkEntry moving = category.getEntries().remove(fromIndex);
                                category.getEntries().add(toIndex, moving);
                                BookmarkManager.getInstance().saveBookmarks();
                                parent.refreshRightList();
                            }
                        }
                    }
                }
                
                draggingEntry = null;
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (isMouseOver(mouseX, mouseY)) {
                scrollOffset -= (int)verticalAmount;
                int visibleEntries = this.height / ENTRY_HEIGHT;
                int maxScroll = Math.max(0, entries.size() - visibleEntries);
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
                return true;
            }
            return false;
        }
        
        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            // Add narration for accessibility
        }

        public static class CommandEntry {
            private final BookmarkEntry entry;
            private final BookmarkCategory sourceCategory;

            public CommandEntry(BookmarkEntry entry, BookmarkCategory sourceCategory) {
                this.entry = entry;
                this.sourceCategory = sourceCategory;
            }

            public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY, boolean isDragging, boolean isSelected) {
                MinecraftClient client = MinecraftClient.getInstance();
                
                // Check if mouse over
                boolean isMouseOver = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + VISUAL_HEIGHT;
                
                // Background - full width (:: is now inside)
                if (isMouseOver) {
                    context.fill(x, y, x + width, y + VISUAL_HEIGHT, 0xFF0080FF);
                } else {
                    context.fill(x, y, x + width, y + VISUAL_HEIGHT, 0xFF404040);
                }
                
                // White outline when hovered OR selected
                if (isMouseOver || isSelected) {
                    // Top border
                    context.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
                    // Bottom border
                    context.fill(x, y + VISUAL_HEIGHT - 1, x + width, y + VISUAL_HEIGHT, 0xFFFFFFFF);
                    // Left border
                    context.fill(x, y, x + 1, y + VISUAL_HEIGHT, 0xFFFFFFFF);
                    // Right border
                    context.fill(x + width - 1, y, x + width, y + VISUAL_HEIGHT, 0xFFFFFFFF);
                }
                
                // Drop zone marker inside the box on the right
                int markerX = x + width - 15; // 15 pixels from right edge
                context.drawTextWithShadow(client.textRenderer, "⁝⁝⁝", markerX, y + 4, 0xFFAAAAAA);
                
                // Text (always show)
                String cmd = entry.getCommand();
                String truncated = cmd;
                int maxWidth = width - 25; // Leave room for ⁝⁝⁝ marker on the right
                
                while (client.textRenderer.getWidth(truncated) > maxWidth && truncated.length() > 0) {
                    truncated = truncated.substring(0, truncated.length() - 1);
                }
                if (truncated.length() < cmd.length()) {
                    truncated = truncated.substring(0, Math.max(0, truncated.length() - 3)) + "...";
                }
                
                context.drawTextWithShadow(client.textRenderer, truncated, x + 5, y + 4, 0xFFFFFFFF);
            }
        }
    }
    
    // ========== ADD/EDIT CATEGORY SCREEN ==========
    public static class AddCategoryScreen extends Screen {
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
    
    // ========== ADD/EDIT COMMAND SCREEN ==========
    public static class AddCommandScreen extends Screen {
        private final BookmarkScreen parent;
        private final BookmarkCategory category;
        private final BookmarkEntry editingEntry; // null if adding new
        private TextFieldWidget commandField;

        public AddCommandScreen(BookmarkScreen parent, BookmarkCategory category, BookmarkEntry editingEntry) {
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
                if (!commandField.getText().isEmpty()) {
                    if (editingEntry != null) {
                        // Edit existing
                        editingEntry.setCommand(commandField.getText());
                    } else {
                        // Add new - use command as name too
                        BookmarkEntry entry = new BookmarkEntry(commandField.getText(), commandField.getText());
                        category.addEntry(entry);
                    }
                    BookmarkManager.getInstance().saveBookmarks();
                    parent.selectCategory(null); // Deselect category (also deselects command)
                    this.close();
                }
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
    
    // ========== DATA CLASSES ==========
    public static class BookmarkCategory {
        private String name;
        private int color;
        private List<BookmarkEntry> entries;

        public BookmarkCategory(String name, int color) {
            this.name = name;
            this.color = color;
            this.entries = new ArrayList<>();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getColor() { return color; }
        public void setColor(int color) { this.color = color; }
        public List<BookmarkEntry> getEntries() { return entries; }
        
        public void addEntry(BookmarkEntry entry) { 
            // Check for duplicates - don't add if command already exists
            for (BookmarkEntry existing : entries) {
                if (existing.getCommand().equals(entry.getCommand())) {
                    return; // Duplicate found, don't add
                }
            }
            entries.add(entry); 
        }
        
        public void removeEntry(BookmarkEntry entry) { entries.remove(entry); }
    }

    public static class BookmarkEntry {
        private String name;
        private String command;

        public BookmarkEntry(String name, String command) {
            this.name = name;
            this.command = command;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
    }
    
    // ========== BOOKMARK MANAGER ==========
    public static class BookmarkManager {
        private static BookmarkManager instance;
        private final List<BookmarkCategory> categories = new ArrayList<>();
        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        private static final String BOOKMARK_FILE = "config/civbuddy_bookmarks.json";
        private static final String PREBUILT_FILE = "config/civbuddy_prebuilt_commands.json";

        private BookmarkManager() {}

        public static BookmarkManager getInstance() {
            if (instance == null) {
                instance = new BookmarkManager();
            }
            return instance;
        }

        public void loadBookmarks() {
            File file = new File(MinecraftClient.getInstance().runDirectory, BOOKMARK_FILE);
            
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    Type listType = new TypeToken<List<BookmarkCategory>>(){}.getType();
                    List<BookmarkCategory> loaded = gson.fromJson(reader, listType);
                    if (loaded != null) {
                        categories.clear();
                        categories.addAll(loaded);
                        ensureHistoryExists();
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            createDefaultCategories();
        }

        private void ensureHistoryExists() {
            // Always ensure History category exists
            boolean hasHistory = categories.stream().anyMatch(cat -> cat.getName().equals("History"));
            if (!hasHistory) {
                categories.add(new BookmarkCategory("History", 0xAAAAAA));
                saveBookmarks();
            }
            
            // Always ensure Destinations category exists
            boolean hasDestinations = categories.stream().anyMatch(cat -> cat.getName().equals("Destinations"));
            if (!hasDestinations) {
                categories.add(new BookmarkCategory("Destinations", 0x55FF55));
                saveBookmarks();
            }
        }

        public void saveBookmarks() {
            File file = new File(MinecraftClient.getInstance().runDirectory, BOOKMARK_FILE);
            file.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(categories, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void createDefaultCategories() {
            categories.clear();
            
            // Load prebuilt commands from config
            PrebuiltCommands prebuilt = loadPrebuiltCommands();
            
            // PVP category
            BookmarkCategory pvp = new BookmarkCategory("PVP", 0xFF5555);
            if (prebuilt != null && prebuilt.pvp != null) {
                for (CommandEntry entry : prebuilt.pvp) {
                    pvp.addEntry(new BookmarkEntry(entry.command, entry.command));
                }
            }
            categories.add(pvp);
            
            // Destinations category
            BookmarkCategory destinations = new BookmarkCategory("Destinations", 0x55FF55);
            if (prebuilt != null && prebuilt.destinations != null) {
                for (CommandEntry entry : prebuilt.destinations) {
                    destinations.addEntry(new BookmarkEntry(entry.command, entry.command));
                }
            }
            categories.add(destinations);
            
            // History category (always empty at start)
            categories.add(new BookmarkCategory("History", 0xAAAAAA));
            
            saveBookmarks();
        }

        private PrebuiltCommands loadPrebuiltCommands() {
            File file = new File(MinecraftClient.getInstance().runDirectory, PREBUILT_FILE);
            
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    return gson.fromJson(reader, PrebuiltCommands.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        // Inner class for deserializing prebuilt commands JSON
        private static class PrebuiltCommands {
            List<CommandEntry> destinations;
            List<CommandEntry> pvp;
        }

        private static class CommandEntry {
            String command;
        }

        public void addCategory(BookmarkCategory category) {
            categories.add(category);
        }

        public void removeCategory(BookmarkCategory category) {
            categories.remove(category);
        }

        public List<BookmarkCategory> getCategories() {
            return categories;
        }

        public BookmarkCategory getHistoryCategory() {
            return categories.stream()
                .filter(cat -> cat.getName().equals("History"))
                .findFirst()
                .orElse(null);
        }

        public void addToHistory(BookmarkEntry entry) {
            BookmarkCategory history = getHistoryCategory();
            if (history != null) {
                history.getEntries().removeIf(e -> e.getCommand().equals(entry.getCommand()));
                history.getEntries().add(0, new BookmarkEntry(entry.getName(), entry.getCommand()));
                while (history.getEntries().size() > 20) {
                    history.getEntries().remove(history.getEntries().size() - 1);
                }
                saveBookmarks();
            }
        }
    }
}
