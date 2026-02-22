package com.civbuddy.commands.ui;

import com.civbuddy.commands.data.CommandCategory;
import com.civbuddy.commands.data.CommandEntry;
import com.civbuddy.commands.data.CommandDao;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.UIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Encapsulates all drag-and-drop state, activation, drop completion,
 * and overlay rendering for CommandManagerScreen.
 */
class DragDropHandler {
    enum DragType { CATEGORY, COMMAND }

    private static final int DRAG_THRESHOLD = 5;

    private final CommandManagerScreen screen;

    private DragType dragType;
    private long draggedCategoryId = -1;
    private long draggedEntryId = -1;
    private String dragLabel;
    private boolean isDragging;

    // Pending drag (activated once cursor moves past threshold)
    private DragType pendingDragType;
    private long pendingDragId = -1;
    private int dragStartX, dragStartY;

    DragDropHandler(CommandManagerScreen screen) {
        this.screen = screen;
    }

    boolean isDragging() { return isDragging; }

    /** Register a pending drag that will activate once the cursor moves past the threshold. */
    void startPending(DragType type, long id, int x, int y) {
        pendingDragType = type;
        pendingDragId = id;
        dragStartX = x;
        dragStartY = y;
    }

    /** Called on mouseDragged — activates the drag once the threshold distance is exceeded. */
    void checkActivation(int mouseX, int mouseY) {
        if (pendingDragType == null || isDragging) return;
        int dx = mouseX - dragStartX;
        int dy = mouseY - dragStartY;
        if (dx * dx + dy * dy < DRAG_THRESHOLD * DRAG_THRESHOLD) return;

        isDragging = true;
        dragType = pendingDragType;
        if (dragType == DragType.CATEGORY) {
            draggedCategoryId = pendingDragId;
            draggedEntryId = -1;
            CommandCategory cat = screen.findCategoryById(pendingDragId);
            dragLabel = cat != null ? cat.getName() : "?";
        } else {
            draggedEntryId = pendingDragId;
            draggedCategoryId = -1;
            CommandEntry ent = screen.findEntryById(pendingDragId);
            dragLabel = ent != null ? ent.getCommand() : "?";
        }
    }

    /**
     * Called on mouseReleased — completes the drop if a drag was active.
     * @return true if a drag was in progress (event consumed).
     */
    boolean completeIfActive(int mouseX, int mouseY) {
        if (isDragging) {
            completeDrop(mouseX, mouseY);
            reset();
            return true;
        }
        reset();
        return false;
    }

    void reset() {
        isDragging = false;
        dragType = null;
        draggedCategoryId = -1;
        draggedEntryId = -1;
        dragLabel = null;
        pendingDragType = null;
        pendingDragId = -1;
    }

    // ═════════════════ Drop completion ═════════════════

    private void completeDrop(int mouseX, int mouseY) {
        if (dragType == DragType.CATEGORY) {
            completeCategoryDrop(mouseX, mouseY);
        } else if (dragType == DragType.COMMAND) {
            completeCommandDrop(mouseX, mouseY);
        }
    }

    private void completeCategoryDrop(int mouseX, int mouseY) {
        if (draggedCategoryId < 0) return;
        FlowLayout list = screen.comp(FlowLayout.class, "categoryList");
        if (list == null) return;
        int targetIndex = insertIndex(list, mouseX, mouseY);
        if (targetIndex < 0) return;

        int fromIndex = screen.indexOfCategoryById(draggedCategoryId);
        if (fromIndex < 0 || fromIndex == targetIndex) return;

        List<Long> ids = new ArrayList<>();
        for (CommandCategory c : screen.cachedCategories) ids.add(c.getId());
        reorderAndMoveUI(list, fromIndex, targetIndex, ids, CommandDao.getInstance()::reorderCategories);
        screen.cachedCategories = CommandDao.getInstance().getCategories();
    }

    private void completeCommandDrop(int mouseX, int mouseY) {
        if (draggedEntryId < 0 || screen.selectedCategoryId < 0) return;

        // Drop on category list → copy to that category (if not the same and not History)
        CommandCategory targetCat = categoryAtMouse(mouseX, mouseY);
        if (targetCat != null && targetCat.getId() != screen.selectedCategoryId && !targetCat.isProtected()) {
            CommandDao.getInstance().copyEntryToCategory(draggedEntryId, targetCat.getId());
            screen.refreshCategoryList();
            screen.deferScrollTo("categoryList", "categoryScroll",
                    screen.indexOfCategoryById(screen.selectedCategoryId));
            return;
        }

        // Reorder within current command list
        FlowLayout list = screen.comp(FlowLayout.class, "commandList");
        if (list == null) return;
        int targetIndex = insertIndex(list, mouseX, mouseY);
        if (targetIndex < 0) return;

        int fromIndex = screen.indexOfEntryById(draggedEntryId);
        if (fromIndex < 0 || fromIndex == targetIndex) return;

        List<Long> ids = new ArrayList<>();
        for (CommandEntry e : screen.cachedCommands) ids.add(e.getId());
        reorderAndMoveUI(list, fromIndex, targetIndex, ids, CommandDao.getInstance()::reorderEntries);
        screen.cachedCommands = CommandDao.getInstance().getEntries(screen.selectedCategoryId);
    }

    /** Reorder IDs, persist via callback, and move the UI child in-place (avoids scroll reset). */
    private void reorderAndMoveUI(FlowLayout list, int from, int target, List<Long> ids, Consumer<List<Long>> persist) {
        long movedId = ids.remove(from);
        int insertAt = target > from ? target - 1 : target;
        if (insertAt > ids.size()) insertAt = ids.size();
        ids.add(insertAt, movedId);
        persist.accept(ids);

        List<UIComponent> children = list.children();
        if (from >= 0 && from < children.size()) {
            UIComponent movedChild = children.get(from);
            list.removeChild(movedChild);
            list.child(Math.min(insertAt, list.children().size()), movedChild);
        }
    }

    // ═════════════════ Rendering ═════════════════

    void renderOverlay(GuiGraphics context, int mx, int my) {
        if (dragLabel == null) return;
        var font = Minecraft.getInstance().font;
        int textW = font.width(dragLabel);
        context.fill(mx + 8, my - 2, mx + 14 + textW, my + 11, 0xCC000000);
        context.drawString(font, dragLabel, mx + 10, my, 0xFFFFFFFF);

        if (dragType == DragType.CATEGORY) {
            highlightInsert(context, screen.comp(FlowLayout.class, "categoryList"), mx, my);
        } else if (dragType == DragType.COMMAND) {
            CommandCategory target = categoryAtMouse(mx, my);
            if (target != null && target.getId() != screen.selectedCategoryId && !target.isProtected()) {
                highlightCategoryRow(context, target);
            } else {
                highlightInsert(context, screen.comp(FlowLayout.class, "commandList"), mx, my);
            }
        }
    }

    private void highlightInsert(GuiGraphics context, FlowLayout list, int mx, int my) {
        if (list == null) return;
        int idx = insertIndex(list, mx, my);
        if (idx < 0) return;
        List<UIComponent> children = list.children();
        int lineY;
        if (idx < children.size()) {
            lineY = children.get(idx).y();
        } else if (!children.isEmpty()) {
            UIComponent last = children.get(children.size() - 1);
            lineY = last.y() + last.height();
        } else {
            return;
        }
        context.fill(list.x(), lineY - 1, list.x() + list.width(), lineY + 1, 0xFF40FF40);
    }

    private void highlightCategoryRow(GuiGraphics context, CommandCategory cat) {
        FlowLayout categoryList = screen.comp(FlowLayout.class, "categoryList");
        if (categoryList == null) return;
        List<UIComponent> children = categoryList.children();
        for (int i = 0; i < children.size() && i < screen.cachedCategories.size(); i++) {
            if (screen.cachedCategories.get(i).getId() == cat.getId()) {
                UIComponent child = children.get(i);
                context.fill(child.x(), child.y(), child.x() + child.width(), child.y() + child.height(), 0x4040FF40);
                break;
            }
        }
    }

    // ═════════════════ Utilities ═════════════════

    /** Determine the insert index in a list based on cursor position. */
    private int insertIndex(FlowLayout list, int mx, int my) {
        if (!isOver(list, mx, my)) return -1;
        List<UIComponent> children = list.children();
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            if (my < child.y() + child.height() / 2) return i;
        }
        return children.size();
    }

    /** Find the category whose row the cursor is directly over. */
    private CommandCategory categoryAtMouse(int mx, int my) {
        UIComponent scroll = screen.comp(UIComponent.class, "categoryScroll");
        if (scroll == null || !isOver(scroll, mx, my)) return null;
        FlowLayout list = screen.comp(FlowLayout.class, "categoryList");
        if (list == null) return null;
        List<UIComponent> children = list.children();
        for (int i = 0; i < children.size() && i < screen.cachedCategories.size(); i++) {
            UIComponent child = children.get(i);
            if (my >= child.y() && my < child.y() + child.height()) {
                return screen.cachedCategories.get(i);
            }
        }
        return null;
    }

    static boolean isOver(UIComponent comp, int mx, int my) {
        return mx >= comp.x() && mx < comp.x() + comp.width()
            && my >= comp.y() && my < comp.y() + comp.height();
    }
}
