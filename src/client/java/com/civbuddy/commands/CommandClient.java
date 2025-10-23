package com.civbuddy.commands;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.Save;
import com.civbuddy.commands.models.CommandGroup;
import com.civbuddy.commands.models.CommandSave;
import com.civbuddy.commands.sceens.BookmarkScreen;
import com.civbuddy.utils.JsonFileHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import java.io.File;

public class CommandClient {
    private static final String BOOKMARK_FILE = "config/" + CivBuddyClient.MODID + "/commands.json";
    private static final String PREBUILT_FILE = "config/" + CivBuddyClient.MODID + "/commands-default.json";
    public static final CommandClient COMMAND_CLIENT = new CommandClient();

    private KeyBinding openCommandListKey;
    public CommandSave data;

    private CommandClient() {}

    public static void initialize() {
        COMMAND_CLIENT.init();
    }

    private void init() {
        openCommandListKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.civbuddy.open_bookmarks",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                "category.civbuddy"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openCommandListKey.isPressed()) {
                MinecraftClient.getInstance().setScreen(new BookmarkScreen(null));
            }
        });

        Save.SAVE_LOADED.register(save -> {
            load();
        });
    }

    public void load() {
        File file = new File(MinecraftClient.getInstance().runDirectory, BOOKMARK_FILE);
        data = JsonFileHelper.load(file, CommandSave.class);
        if (data == null) {
            file = new File(MinecraftClient.getInstance().runDirectory, PREBUILT_FILE);
            data = JsonFileHelper.load(file, CommandSave.class);
            if (data != null) save();
        }
        if (data == null) {
            data = new CommandSave();
        }

        ensureHistoryExists();
    }

    public void save() {
        ensureHistoryExists();
        File file = new File(MinecraftClient.getInstance().runDirectory, BOOKMARK_FILE);
        JsonFileHelper.save(file, data);
    }

    private void ensureHistoryExists() {
        CommandGroup historyGroup = getHistoryGroup();
        if (historyGroup != null) return;

        data.groups.add(new CommandGroup("History", 0xAAAAAA));
        save();
    }

    public CommandGroup getHistoryGroup() {
        return data.groups.stream()
                .filter(CommandGroup::isHistoryGroup)
                .findFirst()
                .orElse(null);
    }
}
