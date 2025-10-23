package com.civbuddy.commands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CommandListWidget extends ClickableWidget {
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