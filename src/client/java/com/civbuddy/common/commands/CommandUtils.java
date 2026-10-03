package com.civbuddy.common.commands;

import com.civbuddy.common.utils.ChatHelper;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.sql.SQLException;


public class CommandUtils {
    public static com.mojang.brigadier.Command<FabricClientCommandSource> andRespondWith(CommandExecutor exe) {
        return ctx -> {
            try {
                Component result = exe.execute(ctx);
                // write to chat
                if (result == null)
                    result = Component.literal("Success!").withStyle(ChatFormatting.GREEN);

                ChatHelper.say(result);
                return 1;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static com.mojang.brigadier.Command<FabricClientCommandSource> help(String message) {
        return ctx -> {
            ChatHelper.say(Component.literal(message).withStyle(ChatFormatting.GREEN));
            return 1;
        };
    }

    public static ArgumentBuilder<FabricClientCommandSource, ?> literal(String s) {
        return LiteralArgumentBuilder.literal(s);
    }

    public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String s, ArgumentType<T> type) {
        return ClientCommandManager.argument(s, type);
    }

    @FunctionalInterface
    public interface CommandExecutor {
        Component execute(CommandContext<FabricClientCommandSource> ctx) throws SQLException;
    }
}
