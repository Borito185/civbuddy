package com.civbuddy.commands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;

import java.util.List;

public class BookmarkListWidget extends AlwaysSelectedEntryListWidget<BookmarkListWidget.CategoryEntry> {
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
        public final BookmarkCategory category;
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
