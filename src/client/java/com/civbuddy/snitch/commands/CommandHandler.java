package com.civbuddy.snitch.commands;

import com.civbuddy.snitch.SnitchClient;
import com.civbuddy.utils.CommandsHelper;
import com.civbuddy.utils.arguments.Vector3IArgument;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;

import static com.civbuddy.utils.CommandsHelper.andRespondWith;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandHandler implements CommandsHelper.CommandProvider{
    public static void initialize() {
        CommandsHelper.register(new CommandHandler());
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return literal("snitch")
                .then(literal("clear_markings")
                        .executes(andRespondWith(CommandHandler::clear)))
                .then(literal("toggle_filter")
                        .executes(andRespondWith(CommandHandler::toggleFilter)))
                .then(literal("add_marking")
                        .then(argument("pos", Vector3IArgument.vector3i())
                                .executes(andRespondWith(CommandHandler::search))));
    }

    @Override
    public boolean commandsAlias() {
        return true;
    }

    public static Component clear(CommandContext<FabricClientCommandSource> ctx) {
        SnitchClient.positions.clear();
        SnitchClient.notifyChange();

        return Component.literal("§aRemoved JA markings");
    }

    public static Component toggleFilter(CommandContext<FabricClientCommandSource> ctx) {
        SnitchClient.filterByPositions = !SnitchClient.filterByPositions;

        if (SnitchClient.filterByPositions) {
            return Component.literal("§aStarted filtering JA by highlighted positions.");
        } else {
            return Component.literal("§aStopped filtering JA by highlighted positions.");
        }
    }

    public static Component search(CommandContext<FabricClientCommandSource> ctx) {
        Vector3i pos = Vector3IArgument.getVector3i(ctx, "pos");

        SnitchClient.positions.add(pos);
        SnitchClient.notifyChange();

        return Component.literal(String.format("Added a position to markings: %d %d %d", pos.x, pos.y, pos.z));
    }
}
