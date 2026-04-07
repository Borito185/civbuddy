package com.civbuddy.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ChatHelper {
    public static void say(Component text) {
        MutableComponent prefix = Component.literal("[CivBuddy]").withStyle(s -> s.withColor(ChatFormatting.GOLD)).append(": ");
        try {
            Minecraft.getInstance().player.displayClientMessage(prefix.append(text), false);
        } catch (Exception ignored) {}
    }
}
