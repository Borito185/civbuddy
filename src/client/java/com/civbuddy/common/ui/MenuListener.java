package com.civbuddy.common.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import static com.civbuddy.CivBuddyClient.CIVBUDDY_CATEGORY;

public class MenuListener {
    public static void initialize() {
        KeyMapping openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.civbuddy.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CIVBUDDY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                Minecraft.getInstance().setScreen(new ConfigMenu(null));
            }
        });
    }
}
