package com.civbuddy.commands.ui;

import com.civbuddy.commands.data.CommandEntry;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.widget.container.SpruceEntryListWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CommandListWidget extends SpruceEntryListWidget<CommandListWidget.CommandEntryWidget> {
    private final CommandManagerScreen parent;

    public CommandListWidget(Position pos, int width, int height, CommandManagerScreen parent) {
        super(pos, width, height, 4, CommandEntryWidget.class);
        this.parent = parent;
    }

    public void setEntries(List<CommandEntry> entries) {
        this.clearEntries();
        for (CommandEntry entry : entries) {
            this.addEntry(new CommandEntryWidget(this, entry));
        }
    }

    public static class CommandEntryWidget extends SpruceEntryListWidget.Entry {
        private final CommandListWidget list;
        private final CommandEntry entry;

        public CommandEntryWidget(CommandListWidget list, CommandEntry entry) {
            this.list = list;
            this.entry = entry;
            this.width = list.getWidth();
            this.height = 20;
        }

        public CommandEntry getEntry() { return entry; }

        @Override
        protected void renderWidget(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
            GuiGraphics gui = graphics.vanilla();
            Font font = this.client.font;
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            boolean selected = list.parent.getSelectedCommand() == entry;
            boolean hovered = this.isMouseHovered();
            int bg = selected ? 0xFF2060C0 : (hovered ? 0xFF404040 : 0x00000000);
            if (bg != 0) gui.fill(x, y, x + w, y + h, bg);

            String cmd = entry.getCommand();
            if (font.width(cmd) > w - 8) {
                cmd = font.plainSubstrByWidth(cmd, w - 12) + "...";
            }
            gui.drawString(font, cmd, x + 4, y + 5, 0xFFFFFFFF, true);
        }

        @Override
        protected boolean onMouseClick(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                list.parent.selectCommand(entry);
                if (doubleClick) {
                    list.parent.executeSelectedCommand();
                }
                return true;
            }
            return false;
        }

        @Override
        protected Component getNarrationMessage() {
            return Component.literal(entry.getCommand());
        }
    }
}
