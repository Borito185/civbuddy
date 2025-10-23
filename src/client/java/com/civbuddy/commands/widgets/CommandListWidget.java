package com.civbuddy.commands.widgets;

import com.civbuddy.commands.CommandClient;
import com.civbuddy.commands.models.CommandGroup;
import com.civbuddy.commands.models.Command;
import com.civbuddy.commands.sceens.BookmarkScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.civbuddy.commands.CommandClient.COMMAND_CLIENT;

public class CommandListWidget extends ClickableWidget {
    private static final int ENTRY_HEIGHT = 20; // Increased from 14 to 20 for more spacing
    private static final int VISUAL_HEIGHT = 16; // Actual box height
    private final BookmarkScreen parent;
    private CommandGroup activeGroup;
    private final List<CommandEntry> children = new ArrayList<>();
    private CommandEntry draggingEntry;
    private long dragStartTime = 0;
    private int scrollOffset = 0;


    public CommandListWidget(Vector2i pos, Vector2i size, BookmarkScreen parent) {
        super(pos.x, pos.y, size.x, size.y, Text.literal("Commands"));
        this.parent = parent;
    }

    public CommandGroup getActiveGroup() {
        return activeGroup;
    }

    public void setActiveGroup(CommandGroup group) {
        activeGroup = group;
        reset();
    }

    public void reset() {
        scrollOffset = 0;
        dragStartTime = 0;
        draggingEntry = null;
        List<CommandGroup> groups = COMMAND_CLIENT.data.groups;
        if (!groups.contains(activeGroup)) {
            activeGroup = null;
        }
    }

    public void refresh() {
        children.clear();
        String searchText = parent.getSearchString();

        // If there's search text, always do global search
        if (!searchText.isEmpty()) {
            Set<String> seenCommands = new HashSet<>();

            for (CommandGroup category : COMMAND_CLIENT.data.groups) {
                for (Command entry : category.getEntries()) {
                    String cmd = entry.getCommand();
                    // Only add if matches search and not already seen (no duplicates)
                    if (cmd.toLowerCase().contains(searchText) && !seenCommands.contains(cmd)) {
                        children.add(new CommandListWidget.CommandEntry(entry));
                        seenCommands.add(cmd);
                    }
                }
            }
        } else if (activeGroup != null) {
            // Show commands from selected category only when no search
            java.util.Set<String> seenCommands = new java.util.HashSet<>();

            for (Command entry : activeGroup.getEntries()) {
                String cmd = entry.getCommand();
                // Only add if not already seen (no duplicates)
                if (!seenCommands.contains(cmd)) {
                    children.add(new CommandListWidget.CommandEntry(entry));
                    seenCommands.add(cmd);
                }
            }
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        var entries = children;
        Vector2i mousePos = new Vector2i(mouseX, mouseY);

        // Dark background
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF202020);

        // Calculate visible entries
        int visibleEntries = this.height / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, entries.size() - visibleEntries);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        // Render entries
        int y = this.getY();
        for (int i = scrollOffset; i < entries.size() && y < this.getY() + this.height; i++) {
            CommandEntry entry = entries.get(i);
            entry.render(context, this.getX(), y, this.width - 6, mouseX, mouseY);
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
                int dragX = mouseX - rectWidth / 2;
                int dragY = mouseY - rectHeight / 2;

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
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;

        int relativeY = (int)(mouseY - this.getY());
        int index = scrollOffset + (relativeY / ENTRY_HEIGHT);

        List<CommandEntry> entries = children;

        if (index >= 0 && index < entries.size()) {
            CommandEntry entry = entries.get(index);

            // Calculate where the :: marker is (right side of box)
            int entryWidth = this.width - 6; // -6 for scrollbar space
            int markerStartX = this.getX() + entryWidth - 20; // :: area is last 20 pixels

            // Check if in drop zone (:: area) - right side of the box
            if (mouseX >= markerStartX) {
                // In drop zone - select the command (for editing) and prepare for dragging
                parent.focusOn(entry.command);
                draggingEntry = entry;
                dragStartTime = System.currentTimeMillis();
                return true;
            } else {
                // Execute command
                parent.executeCommand(entry.command);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || draggingEntry == null) return false;

        boolean handled = false;

        // Check if dropped on a category
        for (CommandGroupListWidget.CommandGroupEntry commandGroupEntry : parent.leftList.children()) {
            if (!commandGroupEntry.isMouseOver(mouseX, mouseY)) continue;
            CommandGroup targetCategory = commandGroupEntry.group;

            // Prevent dropping on History category
            if (targetCategory.isHistoryGroup()) {
                handled = true;
                break;
            }

            if (targetCategory.getEntries().contains(draggingEntry.command)) {
                Command newEntry = draggingEntry.command.clone();
                targetCategory.addEntry(newEntry);
                COMMAND_CLIENT.save();
                parent.refresh();
            }
            handled = true;
            break;
        }

        if (!handled && isMouseOver(mouseX, mouseY)) {
            // Reorder within list
            int relativeY = (int)(mouseY - this.getY());
            int dropIndex = scrollOffset + (relativeY / ENTRY_HEIGHT);

            List<CommandEntry> entries = children;

            if (dropIndex >= 0 && dropIndex < entries.size()) {
                CommandEntry target = entries.get(dropIndex);
                if (draggingEntry != target && entries.contains(draggingEntry)) {
                    int fromIndex = entries.indexOf(draggingEntry);
                    int toIndex = entries.indexOf(target);

                    if (fromIndex != -1 && toIndex != -1) {
                        CommandEntry moving = entries.remove(fromIndex);
                        entries.add(toIndex, moving);
                        COMMAND_CLIENT.save();
                        refresh();
                    }
                }
            }
        }

        draggingEntry = null;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        scrollOffset -= (int)verticalAmount;
        int visibleEntries = this.height / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, children.size() - visibleEntries);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Add narration for accessibility
    }

    public class CommandEntry {
        private final Command command;

        public CommandEntry(Command command) {
            this.command = command;
        }

        public void render(DrawContext context, int x, int y, int entryWidth, int mouseX, int mouseY) {
            MinecraftClient client = MinecraftClient.getInstance();

            // Check if mouse over
            boolean isMouseOver = mouseX >= x && mouseX < x + entryWidth && mouseY >= y && mouseY < y + VISUAL_HEIGHT;

            // Background - full width (:: is now inside)
            int backGroundColor = isMouseOver ? 0xFF0080FF : 0xFF404040;
            context.fill(x, y, x + width, y + VISUAL_HEIGHT, backGroundColor);

            // White outline when hovered OR selected
            if (parent.isFocused(command)) {
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
            String cmd = command.getCommand();
            String truncated = cmd.trim();
            int maxWidth = width - 25; // Leave room for ⁝⁝⁝ marker on the right

            while (client.textRenderer.getWidth(truncated) > maxWidth && !truncated.isEmpty()) {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            if (truncated.length() < cmd.length()) {
                truncated = truncated.substring(0, Math.max(0, truncated.length() - 3)) + "...";
            }

            context.drawTextWithShadow(client.textRenderer, truncated, x + 5, y + 4, 0xFFFFFFFF);
        }
    }
}