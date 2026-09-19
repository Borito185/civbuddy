package com.civbuddy.snitch.commands;

import com.civbuddy.snitch.SnitchClient;
import com.civbuddy.utils.CommandsHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import static com.civbuddy.utils.CommandsHelper.andRespondWith;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandHandler implements CommandsHelper.CommandProvider{
    public static void initialize() {
        CommandsHelper.register(new CommandHandler());
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return literal("snitch")
                .then(literal("clear")
                        .executes(andRespondWith(CommandHandler::clear)));
    }

    @Override
    public boolean commandsAlias() {
        return false;
    }

    public static Component clear(CommandContext<FabricClientCommandSource> ctx) {

        SnitchClient.clear();

        return Component.literal("§aRemoved JA markings");
    }
}
