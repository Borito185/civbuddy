package com.civbuddy.commands.ui;

import com.civbuddy.commands.data.CommandCategory;
import com.civbuddy.commands.data.CommandEntry;
import com.civbuddy.commands.data.CommandDao;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class CommandManagerScreen extends BaseOwoScreen<FlowLayout> {
    private final net.minecraft.client.gui.screens.Screen parentScreen;
    private final DragDropHandler dragHandler = new DragDropHandler(this);

    // ID-based selection (-1 = none)
    long selectedCategoryId = -1;
    private long selectedCommandId = -1;
    int lastMouseX, lastMouseY;

    // Deferred scroll target (applied after next layout pass)
    private UIComponent deferredScrollTarget;
    private String deferredScrollContainerId;

    // Cached data (transient, re-read from DB on each refresh)
    List<CommandCategory> cachedCategories = new ArrayList<>();
    List<CommandEntry> cachedCommands = new ArrayList<>();

    public CommandManagerScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.literal("Command Manager"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, (BiFunction<Sizing, Sizing, FlowLayout>) UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        root.padding(Insets.of(6));
        root.gap(4);

        // Title
        LabelComponent titleLabel = UIComponents.label(this.title);
        titleLabel.shadow(true);
        titleLabel.horizontalSizing(Sizing.fill(100));
        titleLabel.horizontalTextAlignment(HorizontalAlignment.CENTER);
        root.child(titleLabel);

        // Top toolbar
        FlowLayout toolbar = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        toolbar.gap(4);
        toolbar.verticalAlignment(VerticalAlignment.CENTER);
        toolbar.child(btn("+ Category", 75, () ->
                Minecraft.getInstance().setScreen(new AddEditScreen(this, "Add Category", null, 64,
                        name -> CommandDao.getInstance().addCategory(name, 0xFFFFFF)))));

        ButtonComponent addCmdBtn = btn("+ Command", 80, () -> {
            if (selectedCategoryId >= 0 && !CommandDao.getInstance().isProtected(selectedCategoryId)) {
                long catId = selectedCategoryId;
                Minecraft.getInstance().setScreen(new AddEditScreen(this, "Add Command", null, 256,
                        cmd -> CommandDao.getInstance().addEntry(catId, cmd)));
            }
        });
        addCmdBtn.id("addCommandBtn");
        toolbar.child(addCmdBtn);

        ButtonComponent editBtn = btn("Edit", 40, this::editSelected);
        editBtn.id("editBtn");
        toolbar.child(editBtn);

        ButtonComponent deleteBtn = btn("Delete", 50, this::deleteSelected);
        deleteBtn.id("deleteBtn");
        toolbar.child(deleteBtn);

        TextBoxComponent searchField = UIComponents.textBox(Sizing.expand(100));
        searchField.id("searchField");
        searchField.onChanged().subscribe(text -> refreshCommandList());
        toolbar.child(searchField);
        root.child(toolbar);

        // Section labels
        FlowLayout labelsRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        labelsRow.child(sectionLabel("Categories", Sizing.fill(35)));
        labelsRow.child(sectionLabel("Commands", null));
        root.child(labelsRow);

        // Main content: category list (left) + command list (right)
        FlowLayout contentRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.expand(100));
        contentRow.gap(6);
        contentRow.child(scrollList("categoryList", "categoryScroll", Sizing.fill(35)));
        contentRow.child(scrollList("commandList", "commandScroll", Sizing.expand(100)));
        root.child(contentRow);

        // Bottom bar
        FlowLayout bottomBar = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        bottomBar.child(btn("Close", 60, this::onClose));
        bottomBar.child(UIContainers.horizontalFlow(Sizing.expand(100), Sizing.fixed(1)));
        ButtonComponent execBtn = btn("Execute", 60, this::executeSelectedCommand);
        execBtn.id("executeBtn");
        bottomBar.child(execBtn);
        root.child(bottomBar);

        refreshCategoryList();
        updateButtonStates();
    }

    @Override
    protected void init() {
        boolean isReshow = this.uiAdapter != null;
        super.init();
        if (isReshow) {
            refreshCategoryList();
            refreshCommandList();
            updateButtonStates();
        }
    }

    // ═════════════════ UI helpers ═════════════════

    /** Package-private accessor for component(), which is protected on BaseOwoScreen. */
    <C extends UIComponent> C comp(Class<C> type, String id) {
        return this.component(type, id);
    }

    private ButtonComponent btn(String text, int width, Runnable action) {
        ButtonComponent b = UIComponents.button(Component.literal(text), btn -> action.run());
        b.horizontalSizing(Sizing.fixed(width));
        return b;
    }

    private LabelComponent sectionLabel(String text, Sizing hSizing) {
        LabelComponent label = UIComponents.label(Component.literal(text));
        label.color(Color.ofArgb(0xFFAAAAAA));
        label.shadow(true);
        if (hSizing != null) label.horizontalSizing(hSizing);
        return label;
    }

    private ScrollContainer<?> scrollList(String listId, String scrollId, Sizing width) {
        FlowLayout content = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        content.id(listId);
        content.gap(1);
        var scroll = UIContainers.verticalScroll(width, Sizing.fill(100), content);
        scroll.surface(Surface.DARK_PANEL);
        scroll.padding(Insets.of(2));
        scroll.id(scrollId);
        return scroll;
    }

    // ═════════════════ Lookup helpers ═════════════════

    CommandCategory findCategoryById(long id) {
        for (CommandCategory c : cachedCategories) { if (c.getId() == id) return c; }
        return null;
    }

    CommandEntry findEntryById(long id) {
        for (CommandEntry e : cachedCommands) { if (e.getId() == id) return e; }
        return null;
    }

    int indexOfCategoryById(long id) {
        for (int i = 0; i < cachedCategories.size(); i++) { if (cachedCategories.get(i).getId() == id) return i; }
        return -1;
    }

    int indexOfEntryById(long id) {
        for (int i = 0; i < cachedCommands.size(); i++) { if (cachedCommands.get(i).getId() == id) return i; }
        return -1;
    }

    private boolean isInScrollbarZone(int mouseX, String scrollId) {
        UIComponent scrollComp = this.component(UIComponent.class, scrollId);
        return scrollComp != null && mouseX >= scrollComp.x() + scrollComp.width() - 10;
    }

    /** Schedule scrolling to a child after the next layout pass. */
    void deferScrollTo(String listId, String scrollId, int childIndex) {
        FlowLayout list = this.component(FlowLayout.class, listId);
        if (list == null || childIndex < 0 || childIndex >= list.children().size()) return;
        deferredScrollTarget = list.children().get(childIndex);
        deferredScrollContainerId = scrollId;
    }

    // ═════════════════ Category list ═════════════════

    void refreshCategoryList() {
        FlowLayout categoryList = this.component(FlowLayout.class, "categoryList");
        categoryList.clearChildren();
        cachedCategories = CommandDao.getInstance().getCategories();
        for (CommandCategory cat : cachedCategories) {
            long catId = cat.getId();
            FlowLayout entry = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
            entry.verticalAlignment(VerticalAlignment.CENTER);
            entry.padding(Insets.horizontal(4));
            entry.cursorStyle(CursorStyle.HAND);
            entry.surface(catId == selectedCategoryId ? Surface.flat(0xFF2060C0) : Surface.BLANK);

            LabelComponent nameLabel = UIComponents.label(Component.literal(cat.getName()));
            nameLabel.shadow(true);
            nameLabel.horizontalSizing(Sizing.expand(100));
            entry.child(nameLabel);

            LabelComponent countLabel = UIComponents.label(Component.literal("(" + cat.getEntries().size() + ")"));
            countLabel.color(Color.ofArgb(0xFFAAAAAA));
            countLabel.shadow(true);
            entry.child(countLabel);

            entry.mouseDown().subscribe((click, doubled) -> {
                if (!dragHandler.isDragging()) {
                    selectCategory(catId);
                    if (!isInScrollbarZone(lastMouseX, "categoryScroll"))
                        dragHandler.startPending(DragDropHandler.DragType.CATEGORY, catId, lastMouseX, lastMouseY);
                }
                return true;
            });

            categoryList.child(entry);
        }
    }

    // ═════════════════ Command list ═════════════════
    private void refreshCommandList() {
        FlowLayout commandList = this.component(FlowLayout.class, "commandList");
        commandList.clearChildren();
        cachedCommands.clear();

        TextBoxComponent searchField = this.component(TextBoxComponent.class, "searchField");
        String search = searchField != null ? searchField.getValue().toLowerCase() : "";

        if (!search.isEmpty()) {
            // Global search across all categories
            for (CommandCategory cat : cachedCategories) {
                for (CommandEntry cmdEntry : cat.getEntries()) {
                    if (cmdEntry.getCommand().toLowerCase().contains(search)) {
                        cachedCommands.add(cmdEntry);
                        addCommandRow(commandList, cmdEntry, cat.getName());
                    }
                }
            }
        } else {
            CommandCategory selCat = findCategoryById(selectedCategoryId);
            if (selCat == null) return;
            for (CommandEntry cmdEntry : selCat.getEntries()) {
                cachedCommands.add(cmdEntry);
                addCommandRow(commandList, cmdEntry, null);
            }
        }
    }

    private void addCommandRow(FlowLayout commandList, CommandEntry cmdEntry, String categoryPrefix) {
        long entryId = cmdEntry.getId();
        FlowLayout entry = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(20));
        entry.verticalAlignment(VerticalAlignment.CENTER);
        entry.padding(Insets.horizontal(4));
        entry.cursorStyle(CursorStyle.HAND);
        entry.surface(entryId == selectedCommandId ? Surface.flat(0xFF2060C0) : Surface.BLANK);

        if (categoryPrefix != null) {
            LabelComponent catLabel = UIComponents.label(Component.literal("[" + categoryPrefix + "] "));
            catLabel.color(Color.ofArgb(0xFF88AACC));
            catLabel.shadow(true);
            entry.child(catLabel);
        }

        LabelComponent cmdLabel = UIComponents.label(Component.literal(cmdEntry.getCommand()));
        cmdLabel.shadow(true);
        entry.child(cmdLabel);

        entry.mouseDown().subscribe((click, doubled) -> {
            if (!dragHandler.isDragging()) {
                selectCommand(entryId);
                if (!isInScrollbarZone(lastMouseX, "commandScroll"))
                    dragHandler.startPending(DragDropHandler.DragType.COMMAND, entryId, lastMouseX, lastMouseY);
                if (doubled) executeSelectedCommand();
            }
            return true;
        });

        commandList.child(entry);
    }

    // ═════════════════ Input overrides ═════════════════

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        dragHandler.checkActivation(lastMouseX, lastMouseY);
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragHandler.completeIfActive(lastMouseX, lastMouseY)) return true;
        return super.mouseReleased(click);
    }

    // ═════════════════ Rendering ═════════════════

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        super.render(context, mouseX, mouseY, delta);

        if (deferredScrollTarget != null && deferredScrollContainerId != null) {
            UIComponent scrollComp = this.component(UIComponent.class, deferredScrollContainerId);
            if (scrollComp instanceof ScrollContainer<?> scroll) scroll.scrollTo(deferredScrollTarget);
            deferredScrollTarget = null;
            deferredScrollContainerId = null;
        }

        if (dragHandler.isDragging()) dragHandler.renderOverlay(context, mouseX, mouseY);
    }

    // ═════════════════ Selection ═════════════════
    private void selectCategory(long categoryId) {
        this.selectedCategoryId = categoryId;
        this.selectedCommandId = -1;
        updateHighlights("categoryList", cachedCategories.stream().map(CommandCategory::getId).toList(), selectedCategoryId);
        refreshCommandList();
        updateButtonStates();
    }

    private void selectCommand(long entryId) {
        this.selectedCommandId = entryId;
        updateHighlights("commandList", cachedCommands.stream().map(CommandEntry::getId).toList(), selectedCommandId);
        updateButtonStates();
    }

    /** Update highlight surfaces in a list to reflect the selected ID. */
    private void updateHighlights(String listId, List<Long> ids, long selectedId) {
        FlowLayout list = this.component(FlowLayout.class, listId);
        if (list == null) return;
        List<UIComponent> children = list.children();
        for (int i = 0; i < children.size() && i < ids.size(); i++) {
            if (children.get(i) instanceof FlowLayout flow) {
                flow.surface(ids.get(i) == selectedId ? Surface.flat(0xFF2060C0) : Surface.BLANK);
            }
        }
    }

    // ═════════════════ Button state ═════════════════
    private void updateButtonStates() {
        boolean hasCat = selectedCategoryId >= 0;
        boolean hasCmd = selectedCommandId >= 0;
        boolean editable = hasCat && !CommandDao.getInstance().isProtected(selectedCategoryId);

        ButtonComponent addCmdBtn = this.component(ButtonComponent.class, "addCommandBtn");
        ButtonComponent editBtn = this.component(ButtonComponent.class, "editBtn");
        ButtonComponent deleteBtn = this.component(ButtonComponent.class, "deleteBtn");
        ButtonComponent execBtn = this.component(ButtonComponent.class, "executeBtn");

        if (addCmdBtn != null) addCmdBtn.active = editable;
        if (editBtn != null) editBtn.active = editable || hasCmd;
        if (deleteBtn != null) deleteBtn.active = editable || hasCmd;
        if (execBtn != null) execBtn.active = hasCmd;
    }

    // ═════════════════ Actions ═════════════════
    private void editSelected() {
        if (selectedCommandId >= 0) {
            CommandEntry entry = findEntryById(selectedCommandId);
            if (entry == null) return;
            long entryId = entry.getId();
            Minecraft.getInstance().setScreen(new AddEditScreen(this, "Edit Command", entry.getCommand(), 256,
                    cmd -> CommandDao.getInstance().updateEntry(entryId, cmd)));
        } else if (selectedCategoryId >= 0 && !CommandDao.getInstance().isProtected(selectedCategoryId)) {
            CommandCategory cat = findCategoryById(selectedCategoryId);
            if (cat == null) return;
            long catId = cat.getId();
            int color = cat.getColor();
            Minecraft.getInstance().setScreen(new AddEditScreen(this, "Edit Category", cat.getName(), 64,
                    name -> CommandDao.getInstance().updateCategory(catId, name, color)));
        }
    }

    private void deleteSelected() {
        if (selectedCommandId >= 0) {
            CommandDao.getInstance().removeEntry(selectedCommandId);
            selectedCommandId = -1;
            refreshCategoryList(); // update entry count
            refreshCommandList();
        } else if (selectedCategoryId >= 0 && !CommandDao.getInstance().isProtected(selectedCategoryId)) {
            CommandDao.getInstance().removeCategory(selectedCategoryId);
            selectedCategoryId = -1;
            selectedCommandId = -1;
            refreshCategoryList();
            FlowLayout commandList = this.component(FlowLayout.class, "commandList");
            if (commandList != null) commandList.clearChildren();
            cachedCommands.clear();
        }
        updateButtonStates();
    }

    public void executeSelectedCommand() {
        var mc = Minecraft.getInstance();
        CommandEntry entry = findEntryById(selectedCommandId);
        if (entry != null && mc != null && mc.player != null) {
            String cmd = entry.getCommand();
            this.onClose();
            if (cmd.startsWith("/")) {
                mc.player.connection.sendCommand(cmd.substring(1));
            } else {
                mc.player.connection.sendChat(cmd);
            }
            CommandDao.getInstance().addToHistory(cmd);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
