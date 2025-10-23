package com.civbuddy.commands;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CommandClient {
    public static void initialize() {
        registerKeybinding();
        BookmarkManager.getInstance().loadBookmarks();
    }
    
    private static void registerKeybinding() {
        KeyBinding openBookmarkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.civbuddy.open_bookmarks",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            "category.civbuddy"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBookmarkKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new BookmarkScreen(null));
            }
        });
    }
}
