package com.civbuddy.snitch.commands;

import com.civbuddy.common.commands.Command;
import com.civbuddy.common.commands.CommandManager;
import com.civbuddy.snitch.SnitchClient;
import com.civbuddy.common.utils.arguments.Vector3IArgument;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;

import java.util.List;
import java.util.function.Consumer;

import static com.civbuddy.common.commands.CommandUtils.*;


public class CommandHandler implements Command {
    public static void initialize() {
        CommandManager.register(new CommandHandler());
    }

    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        add.accept(List.of(
                literal("snitch"),
                literal("clear_markings").executes(andRespondWith(CommandHandler::clear))
        ));

        add.accept(List.of(
                literal("snitch"),
                literal("toggle_filter").executes(andRespondWith(CommandHandler::toggleFilter))
        ));

        add.accept(List.of(
                literal("snitch"),
                literal("add_marking"),
                argument("pos", Vector3IArgument.vector3i())
                        .executes(andRespondWith(CommandHandler::search)))
        );
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
