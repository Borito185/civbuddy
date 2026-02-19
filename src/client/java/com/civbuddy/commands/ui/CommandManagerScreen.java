package com.civbuddy.commands.ui;

import com.civbuddy.commands.data.CommandCategory;
import com.civbuddy.commands.data.CommandEntry;
import com.civbuddy.commands.data.CommandManager;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

public class CommandManagerScreen extends SpruceScreen {
    private final Screen parentScreen;

    private CategoryListWidget categoryList;
    private CommandListWidget commandList;
    private SpruceTextFieldWidget searchField;

    private CommandCategory selectedCategory;
    private CommandEntry selectedCommand;

    private SpruceButtonWidget addCommandButton;
    private SpruceButtonWidget editButton;
    private SpruceButtonWidget deleteButton;
    private SpruceButtonWidget executeButton;

    public CommandManagerScreen(Screen parentScreen) {
        super(Component.literal("Command Manager"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int catWidth = 150;
        int btnY = 28;
        int listTop = 60;
        int listBottom = this.height - 30;

        // Top buttons
        int bx = 10;
        this.addRenderableWidget(new SpruceButtonWidget(
                Position.of(bx, btnY), 70, 20,
                Component.literal("+ Category"), btn ->
                        Minecraft.getInstance().setScreen(new AddCategoryScreen(this))
        ));
        bx += 75;
        addCommandButton = new SpruceButtonWidget(
                Position.of(bx, btnY), 80, 20,
                Component.literal("+ Command"), btn -> {
                    if (selectedCategory != null && !selectedCategory.isProtected()) {
                        Minecraft.getInstance().setScreen(new AddCommandScreen(this, selectedCategory));
                    }
                }
        );
        this.addRenderableWidget(addCommandButton);
        bx += 85;
        editButton = new SpruceButtonWidget(
                Position.of(bx, btnY), 40, 20,
                Component.literal("Edit"), btn -> editSelected()
        );
        this.addRenderableWidget(editButton);
        bx += 45;
        deleteButton = new SpruceButtonWidget(
                Position.of(bx, btnY), 50, 20,
                Component.literal("Delete"), btn -> deleteSelected()
        );
        this.addRenderableWidget(deleteButton);
        bx += 55;

        // Search field (after buttons)
        int searchStart = Math.max(bx, catWidth + 20);
        int searchWidth = this.width - searchStart - 80;
        if (searchWidth > 60) {
            searchField = new SpruceTextFieldWidget(
                    Position.of(searchStart, btnY), searchWidth, 20,
                    Component.literal("Search"), Component.literal("Search commands...")
            );
            searchField.setChangedListener(text -> refreshCommandList());
            this.addRenderableWidget(searchField);
        }

        // Category list (left)
        categoryList = new CategoryListWidget(
                Position.of(10, listTop), catWidth, listBottom - listTop, this
        );
        this.addRenderableWidget(categoryList);



        // Command list (right)
        commandList = new CommandListWidget(
                Position.of(catWidth + 20, listTop), this.width - catWidth - 30, listBottom - listTop, this
        );
        this.addRenderableWidget(commandList);

        // Bottom buttons
        this.addRenderableWidget(new SpruceButtonWidget(
                Position.of(10, this.height - 25), 60, 20,
                Component.literal("Close"), btn -> this.onClose()
        ));
        executeButton = new SpruceButtonWidget(
                Position.of(this.width - 70, this.height - 25), 60, 20,
                Component.literal("Execute"), btn -> executeSelectedCommand()
        );
        this.addRenderableWidget(executeButton);

        // Load data
        categoryList.refreshEntries();
        if (selectedCategory != null) refreshCommandList();
        updateButtonStates();
    }

    public void selectCategory(CommandCategory category) {
        this.selectedCategory = category;
        this.selectedCommand = null;
        refreshCommandList();
        updateButtonStates();
    }

    public void selectCommand(CommandEntry entry) {
        this.selectedCommand = entry;
        updateButtonStates();
    }

    public CommandCategory getSelectedCategory() { return selectedCategory; }
    public CommandEntry getSelectedCommand() { return selectedCommand; }

    private void refreshCommandList() {
        if (selectedCategory == null) {
            commandList.setEntries(List.of());
            return;
        }
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        List<CommandEntry> entries = selectedCategory.getEntries();
        if (!search.isEmpty()) {
            entries = entries.stream()
                    .filter(e -> e.getCommand().toLowerCase().contains(search)
                              || e.getName().toLowerCase().contains(search))
                    .collect(Collectors.toList());
        }
        commandList.setEntries(entries);
    }

    private void updateButtonStates() {
        boolean hasCat = selectedCategory != null;
        boolean hasCmd = selectedCommand != null;
        boolean editable = hasCat && !selectedCategory.isProtected();

        addCommandButton.setActive(editable);
        editButton.setActive(editable || hasCmd);
        deleteButton.setActive(editable || hasCmd);
        executeButton.setActive(hasCmd);
    }

    private void editSelected() {
        if (selectedCommand != null) {
            Minecraft.getInstance().setScreen(new AddCommandScreen(this, selectedCategory, selectedCommand));
        } else if (selectedCategory != null && !selectedCategory.isProtected()) {
            Minecraft.getInstance().setScreen(new AddCategoryScreen(this, selectedCategory));
        }
    }

    private void deleteSelected() {
        if (selectedCommand != null && selectedCategory != null) {
            selectedCategory.removeEntry(selectedCommand);
            CommandManager.getInstance().saveCommands();
            selectedCommand = null;
            refreshCommandList();
        } else if (selectedCategory != null && !selectedCategory.isProtected()) {
            CommandManager.getInstance().removeCategory(selectedCategory);
            CommandManager.getInstance().saveCommands();
            selectedCategory = null;
            selectedCommand = null;
            categoryList.refreshEntries();
            commandList.setEntries(List.of());
        }
        updateButtonStates();
    }

    public void executeSelectedCommand() {
        if (selectedCommand != null && this.minecraft != null && this.minecraft.player != null) {
            String cmd = selectedCommand.getCommand();
            this.onClose();
            if (cmd.startsWith("/")) {
                this.minecraft.player.connection.sendCommand(cmd.substring(1));
            } else {
                this.minecraft.player.connection.sendChat(cmd);
            }
            CommandManager.getInstance().addToHistory(selectedCommand);
        }
    }

    @Override
    public void render(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderWidgets(graphics, mouseX, mouseY, delta);
        var gui = graphics.vanilla();
        gui.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        gui.drawString(this.font, "Categories", 10, 50, 0xFFAAAAAA, true);
        gui.drawString(this.font, "Commands", 170, 50, 0xFFAAAAAA, true);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
