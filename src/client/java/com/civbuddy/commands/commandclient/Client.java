package com.civbuddy.commands.commandclient;

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
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
        private ButtonWidget pinButton;
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
            
            // Initialize right list (commands) - custom widget with 14px entries
            rightList = new CommandListWidget(
                170,
                55,
                this.width - 170,
                this.height - 85,
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
            
            // Pin button
            pinButton = ButtonWidget.builder(Text.literal("Pin"), button -> togglePin())
                .dimensions(this.width - 80, 5, 40, 20)
                .build();
            
            // Close button
            ButtonWidget closeButton = ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 25, 100, 20)
                .build();
            
            this.addDrawableChild(addButton);
            this.addDrawableChild(editButton);
            this.addDrawableChild(deleteButton);
            this.addDrawableChild(pinButton);
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

        private void refreshRightList() {
            rightList.clearEntries();
            String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
            
            if (selectedCategory != null) {
                // Show commands from selected category
                List<BookmarkEntry> entries = selectedCategory.getEntries();
                List<BookmarkEntry> pinnedEntries = new ArrayList<>();
                List<BookmarkEntry> regularEntries = new ArrayList<>();
                
                for (BookmarkEntry entry : entries) {
                    if (searchText.isEmpty() || entry.getCommand().toLowerCase().contains(searchText)) {
                        if (entry.isPinned()) {
                            pinnedEntries.add(entry);
                        } else {
                            regularEntries.add(entry);
                        }
                    }
                }
                
                for (BookmarkEntry entry : pinnedEntries) {
                    rightList.addEntry(new CommandListWidget.CommandEntry(entry, selectedCategory));
                }
                for (BookmarkEntry entry : regularEntries) {
                    rightList.addEntry(new CommandListWidget.CommandEntry(entry, selectedCategory));
                }
            } else if (!searchText.isEmpty()) {
                // Global search - show all commands from all categories
                for (BookmarkCategory category : BookmarkManager.getInstance().getCategories()) {
                    for (BookmarkEntry entry : category.getEntries()) {
                        if (entry.getCommand().toLowerCase().contains(searchText)) {
                            rightList.addEntry(new CommandListWidget.CommandEntry(entry, category));
                        }
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
            pinButton.active = hasCommandSelected && hasCategorySelected;
        }

        private void openAddDialog() {
            if (selectedCategory != null) {
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

        private void togglePin() {
            if (selectedCommand != null && selectedCategory != null) {
                selectedCommand.setPinned(!selectedCommand.isPinned());
                BookmarkManager.getInstance().saveBookmarks();
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
                
                // BRIGHT WHITE text - centered for 25px height
                String displayName = category.getName() + " (" + category.getEntries().size() + ")";
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
        private static final int DROP_ZONE_WIDTH = 30;
        private static final int ENTRY_HEIGHT = 14;
        private int scrollOffset = 0;

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
                entry.render(context, this.getX(), y, this.width, mouseX, mouseY);
                y += ENTRY_HEIGHT;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isMouseOver(mouseX, mouseY)) {
                int relativeY = (int)(mouseY - this.getY());
                int index = scrollOffset + (relativeY / ENTRY_HEIGHT);
                
                if (index >= 0 && index < entries.size()) {
                    CommandEntry entry = entries.get(index);
                    
                    // Check if in drop zone
                    int dropZoneStartX = this.getX() + this.width - DROP_ZONE_WIDTH;
                    if (mouseX >= dropZoneStartX) {
                        // In drop zone - start dragging
                        draggingEntry = entry;
                        parent.selectCommand(entry.entry);
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
                        if (draggingEntry.sourceCategory != targetCategory) {
                            BookmarkEntry newEntry = new BookmarkEntry(draggingEntry.entry.getName(), draggingEntry.entry.getCommand());
                            newEntry.setPinned(draggingEntry.entry.isPinned());
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

            public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
                MinecraftClient client = MinecraftClient.getInstance();
                
                // Check if mouse over
                boolean isMouseOver = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ENTRY_HEIGHT;
                
                int visualWidth = width - DROP_ZONE_WIDTH;
                
                // Background
                if (isMouseOver) {
                    context.fill(x, y, x + visualWidth, y + ENTRY_HEIGHT, 0xFF0080FF);
                } else if (entry.isPinned()) {
                    context.fill(x, y, x + visualWidth, y + ENTRY_HEIGHT, 0xFF505050);
                } else {
                    context.fill(x, y, x + visualWidth, y + ENTRY_HEIGHT, 0xFF404040);
                }
                
                // Drop zone marker
                context.drawTextWithShadow(client.textRenderer, "::", x + visualWidth + 5, y + 3, 0xFFAAAAAA);
                
                // Text
                String pinIndicator = entry.isPinned() ? "📌 " : "";
                String cmd = entry.getCommand();
                String truncated = pinIndicator + cmd;
                int maxWidth = visualWidth - 10;
                
                while (client.textRenderer.getWidth(truncated) > maxWidth && truncated.length() > 0) {
                    truncated = truncated.substring(0, truncated.length() - 1);
                }
                if (truncated.length() < (pinIndicator + cmd).length()) {
                    truncated = truncated.substring(0, Math.max(0, truncated.length() - 3)) + "...";
                }
                
                context.drawTextWithShadow(client.textRenderer, truncated, x + 5, y + 3, 0xFFFFFFFF);
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
        public void addEntry(BookmarkEntry entry) { entries.add(entry); }
        public void removeEntry(BookmarkEntry entry) { entries.remove(entry); }
    }

    public static class BookmarkEntry {
        private String name;
        private String command;
        private boolean pinned;
        private boolean favorite;

        public BookmarkEntry(String name, String command) {
            this.name = name;
            this.command = command;
            this.pinned = false;
            this.favorite = false;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public boolean isPinned() { return pinned; }
        public void setPinned(boolean pinned) { this.pinned = pinned; }
        public boolean isFavorite() { return favorite; }
        public void setFavorite(boolean favorite) { this.favorite = favorite; }
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
