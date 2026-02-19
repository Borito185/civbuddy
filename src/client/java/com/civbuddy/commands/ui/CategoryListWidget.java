package com.civbuddy.commands.ui;

import com.civbuddy.commands.data.CommandCategory;
import com.civbuddy.commands.data.CommandManager;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.widget.container.SpruceEntryListWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CategoryListWidget extends SpruceEntryListWidget<CategoryListWidget.CategoryEntry> {
    private final CommandManagerScreen parent;

    public CategoryListWidget(Position pos, int width, int height, CommandManagerScreen parent) {
        super(pos, width, height, 4, CategoryEntry.class);
        this.parent = parent;
    }

    public void refreshEntries() {
        this.clearEntries();
        for (CommandCategory cat : CommandManager.getInstance().getCategories()) {
            this.addEntry(new CategoryEntry(this, cat));
        }
    }

    public static class CategoryEntry extends SpruceEntryListWidget.Entry {
        private final CategoryListWidget list;
        private final CommandCategory category;

        public CategoryEntry(CategoryListWidget list, CommandCategory category) {
            this.list = list;
            this.category = category;
            this.width = list.getWidth();
            this.height = 25;
        }

        public CommandCategory getCategory() { return category; }

        @Override
        protected void renderWidget(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
            GuiGraphics gui = graphics.vanilla();
            Font font = this.client.font;
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            boolean selected = list.parent.getSelectedCategory() == category;
            boolean hovered = this.isMouseHovered();
            int bg = selected ? 0xFF2060C0 : (hovered ? 0xFF505050 : 0x00000000);
            if (bg != 0) gui.fill(x, y, x + w, y + h, bg);

            String name = category.getName();
            int maxTextWidth = w - 40;
            if (font.width(name) > maxTextWidth) {
                name = font.plainSubstrByWidth(name, maxTextWidth - 4) + "...";
            }
            gui.drawString(font, name, x + 4, y + 8, 0xFFFFFFFF, true);

            String count = "(" + category.getEntries().size() + ")";
            int cw = font.width(count);
            gui.drawString(font, count, x + w - cw - 4, y + 8, 0xFFAAAAAA, true);
        }

        @Override
        protected boolean onMouseClick(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                list.parent.selectCategory(category);
                return true;
            }
            return false;
        }

        @Override
        protected Component getNarrationMessage() {
            return Component.literal(category.getName());
        }
    }
}
