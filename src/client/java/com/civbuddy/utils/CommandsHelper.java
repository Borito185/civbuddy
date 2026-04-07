package com.civbuddy.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class CommandsHelper {
    @FunctionalInterface
    public interface CommandExecutor {
        Component execute(CommandContext<FabricClientCommandSource> ctx) throws SQLException;
    }

    public interface CommandProvider {
        LiteralArgumentBuilder<FabricClientCommandSource> commands();
        boolean commandsAlias();
    }
    private static final Set<CommandProvider> providers = new HashSet<>();

    public static void register(CommandProvider provider) {
        providers.add(provider);
    }

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, commandRegistryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = literal("civbuddy");
            LiteralArgumentBuilder<FabricClientCommandSource> root2 = literal("cb");

            for (CommandProvider provider : providers) {
                LiteralArgumentBuilder<FabricClientCommandSource> commands = provider.commands();

                if (provider.commandsAlias())
                    dispatcher.register(commands);

                root = root.then(commands);
                root2 = root2.then(commands);
            }

            dispatcher.register(root);
            dispatcher.register(root2);
        }));
    }

    public static Command<FabricClientCommandSource> andRespondWith(CommandExecutor exe) {
        return ctx -> {
            try {
                Component result = exe.execute(ctx);
                // write to chat
                if (result == null)
                    result = Component.literal("Success!").withStyle(s -> s.withColor(ChatFormatting.GREEN));

                ChatHelper.say(result);
                return 1;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
