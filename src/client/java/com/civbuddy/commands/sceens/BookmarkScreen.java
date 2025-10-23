package com.civbuddy.commands.sceens;

import com.civbuddy.commands.models.CommandGroup;
import com.civbuddy.commands.models.Command;
import com.civbuddy.commands.widgets.CommandGroupListWidget;
import com.civbuddy.commands.widgets.CommandListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.joml.Vector2i;

import static com.civbuddy.commands.CommandClient.COMMAND_CLIENT;

public class BookmarkScreen extends Screen {
    private final Screen parent;

    // --- Components ---
    public CommandGroupListWidget leftList;
    private CommandListWidget rightList;
    private ButtonWidget addButton;
    private ButtonWidget deleteButton;
    private ButtonWidget editButton;
    private TextFieldWidget searchField;

    // --- State ---
    private Object focusedObject;

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
            refresh();
        });
        this.addDrawableChild(searchField);

        // Initialize left list (bookmarks/categories) - 25px height
        leftList = new CommandGroupListWidget(
                this.client,
                150,
                this.height - 80,
                55,
                this.height - 30,
                25,
                this
        );
        leftList.setX(10);

        // Initialize right list (commands) - custom widget aligned with left list
        rightList = new CommandListWidget(
                new Vector2i(170, 55),
                new Vector2i(this.width - 170, this.height - 80),
                this
        );

        // Load and refresh
        COMMAND_CLIENT.load();
        refresh();

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

        // Close button
        ButtonWidget closeButton = ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 25, 100, 20)
                .build();

        this.addDrawableChild(addButton);
        this.addDrawableChild(editButton);
        this.addDrawableChild(deleteButton);
        this.addDrawableChild(closeButton);
        this.addDrawableChild(leftList);
        this.addDrawableChild(rightList);
    }

    public void focusOn(Object obj) {
        focusedObject = obj;
        if (obj instanceof CommandGroup group)
            rightList.setActiveGroup(group);
        updateButtonStates();
    }

    private void updateButtonStates() {
        editButton.active = focusedObject != null;
    }

    private void openAddDialog() {
        CommandGroup activeGroup = rightList.getActiveGroup();
        boolean isGroup = focusedObject instanceof CommandGroup;


        if (isGroup) {
            MinecraftClient.getInstance().setScreen(new CommandGroupInputScreen(this));
            return;
        }

        if (activeGroup.isHistoryGroup()) {
            warnHistoryGroup();
            return;
        }

        focusedObject = null;
        updateButtonStates();
        MinecraftClient.getInstance().setScreen(new CommandInputScreen(this, activeGroup, null));
    }

    private void openEditDialog() {
        CommandGroup activeGroup = rightList.getActiveGroup();
        boolean isGroup = focusedObject instanceof CommandGroup;

        if (activeGroup.isHistoryGroup()) {
            warnHistoryGroup();
            return;
        }

        if (isGroup) {
            MinecraftClient.getInstance().setScreen(new CommandGroupInputScreen(this, activeGroup));
        } else {
            MinecraftClient.getInstance().setScreen(new CommandInputScreen(this, activeGroup, null));
        }
    }

    private void deleteSelected() {
        if (focusedObject == null) return;

        CommandGroup activeGroup = rightList.getActiveGroup();
        boolean isHistory = activeGroup.isHistoryGroup();
        if (isHistory) { // disallow changing history

            return;
        }


        switch (focusedObject) {
            case Command cmd -> activeGroup.removeEntry(cmd);
            case CommandGroup group -> COMMAND_CLIENT.data.groups.remove(group);
            case null, default -> {}
        }

        COMMAND_CLIENT.save();
        refresh();
        focusedObject = null;
    }

    private void warnHistoryGroup() {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§cCannot modify History!"), false);
        }
    }

    public void refresh() {
        leftList.refresh();
        rightList.refresh();
    }

    public void executeCommand(Command entry) {
        if (entry == null || client == null || client.player == null) {
            return;
        }
        String command = entry.getCommand();
        if (command.startsWith("/")) {
            client.player.networkHandler.sendChatCommand(command.substring(1));
        } else {
            client.player.networkHandler.sendChatMessage(command);
        }

        COMMAND_CLIENT.getHistoryGroup().addEntry(entry);
        COMMAND_CLIENT.save();
        this.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on the left list area but not on an entry
        if (button == 0 && mouseX >= 10 && mouseX <= 160 && mouseY >= 55) {
            boolean clickedOnEntry = false;
            for (CommandGroupListWidget.CommandGroupEntry entry : leftList.children()) {
                if (entry.isMouseOver(mouseX, mouseY)) {
                    clickedOnEntry = true;
                    break;
                }
            }
            if (!clickedOnEntry) {
                // Clicked in left list area but not on an entry - deselect
                focusedObject = null;
                refresh();
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

    public boolean isFocused(Object obj) {
        return focusedObject == obj;
    }

    public String getSearchString() {
        return searchField.getText();
    }
}