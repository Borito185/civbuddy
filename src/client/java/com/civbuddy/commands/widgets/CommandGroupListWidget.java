package com.civbuddy.commands.widgets;

import com.civbuddy.commands.models.CommandGroup;
import com.civbuddy.commands.sceens.BookmarkScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Debug;

import java.util.List;

import static com.civbuddy.commands.CommandClient.COMMAND_CLIENT;

public class CommandGroupListWidget extends AlwaysSelectedEntryListWidget<CommandGroupListWidget.CommandGroupEntry> {
    private final BookmarkScreen parent;
    private CommandGroupEntry draggingEntry = null;

    public CommandGroupListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight, BookmarkScreen parent) {
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
            for (CommandGroupEntry entry : this.children()) {
                if (entry.isMouseOver(mouseX, mouseY)) {
                    draggingEntry = entry;
                    break;
                }
            }
            draggingEntry = this.getSelectedOrNull();
            parent.focusOn(draggingEntry == null ? null : draggingEntry.group);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || draggingEntry == null) {
            return super.mouseReleased(mouseX, mouseY, button);
        }

        // Find where to drop
        int dropIndex = -1;
        for (int i = 0; i < this.children().size(); i++) {
            CommandGroupEntry entry = this.children().get(i);
            if (entry.isMouseOver(mouseX, mouseY)) {
                dropIndex = i;
                break;
            }
        }

        if (dropIndex != -1 && draggingEntry != this.children().get(dropIndex)) {
            // Reorder categories
            List<CommandGroup> groups = COMMAND_CLIENT.data.groups;
            int fromIndex = groups.indexOf(draggingEntry.group);
            int toIndex = groups.indexOf(this.children().get(dropIndex).group);

            if (fromIndex != -1 && toIndex != -1) {
                CommandGroup moving = groups.remove(fromIndex);
                groups.add(toIndex, moving);
                COMMAND_CLIENT.save();
                parent.refresh();
            }
        }

        draggingEntry = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void refresh() {
        List<CommandGroupEntry> children = this.children();
        children.clear();
        for (CommandGroup group : COMMAND_CLIENT.data.groups) {
            children.add(new CommandGroupEntry(group));
        }
    }

    public class CommandGroupEntry extends AlwaysSelectedEntryListWidget.Entry<CommandGroupEntry> {
        public final CommandGroup group;

        public CommandGroupEntry(CommandGroup group) {
            this.group = group;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = MinecraftClient.getInstance();

            // Use the actual entry height (25px)
            boolean isDragging = draggingEntry == this;
            if (hovered || isDragging) {
                context.fill(x, y, x + entryWidth, y + entryHeight, 0xFF0080FF); // BRIGHT BLUE
            } else {
                context.fill(x, y, x + entryWidth, y + entryHeight, 0xFF404040); // MEDIUM GRAY
            }

            // Borders
            context.fill(x, y, x + entryWidth, y + 1, 0xFF888888);
            context.fill(x, y + entryHeight - 1, x + entryWidth, y + entryHeight, 0xFF888888);

            // Draw count on the right side (accounting for scrollbar)
            String countText = "(" + group.getEntries().size() + ")";
            int countWidth = client.textRenderer.getWidth(countText);
            int countX = x + entryWidth - countWidth - 12; // 12px padding from right (scrollbar space)
            context.drawTextWithShadow(client.textRenderer, countText, countX, y + 8, 0xFFAAAAAA);

            // Truncate name if needed to not overlap with count
            String name = group.getName();
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
                parent.focusOn(group);
                setSelected(this);
                return true;
            }
            return false;
        }

        @Override
        public Text getNarration() {
            return Text.literal(group.getName());
        }
    }
}
