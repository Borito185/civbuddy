package com.civbuddy.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class CommandsHelper {
    @FunctionalInterface
    public interface CommandExecutor {
        public Text execute(CommandContext<FabricClientCommandSource> ctx);
    }

    public interface CommandProvider {
        public LiteralArgumentBuilder<FabricClientCommandSource> commands();
        public boolean commandsAlias();
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
            MutableText prefix = Text.literal("[CivBuddy]").styled(s -> s.withColor(Formatting.GOLD)).append(": ");
            Text result = exe.execute(ctx);
            // write to chat
            if (result == null)
                result = Text.literal("Success!").styled(s -> s.withColor(Formatting.GREEN));
            MinecraftClient.getInstance().player.sendMessage(prefix.append(result), false);
            return 1;
        };
    }
}
