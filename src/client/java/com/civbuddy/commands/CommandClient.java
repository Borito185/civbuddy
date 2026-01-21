package com.civbuddy.commands;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class CommandClient {
    public static void initialize() {
        registerKeybinding();
        BookmarkManager.getInstance().loadBookmarks();
    }
    
    private static void registerKeybinding() {
        KeyMapping openBookmarkKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.civbuddy.open_bookmarks",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            "category.civbuddy"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBookmarkKey.consumeClick()) {
                Minecraft.getInstance().setScreen(new BookmarkScreen(null));
            }
        });
    }
}
