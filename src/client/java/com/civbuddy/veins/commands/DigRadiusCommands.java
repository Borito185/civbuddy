package com.civbuddy.veins.commands;

import com.civbuddy.common.commands.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import static com.civbuddy.CivBuddyClient.config;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.civbuddy.common.commands.CommandUtils.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public final class DigRadiusCommands implements Command {

    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        add.accept(List.of(
                literal("digRange"),
                argument("radius", integer(1, 256)).executes(andRespondWith(this::digRadius))
        ));

        add.accept(List.of(
                literal("digRange"),
                argument("x", integer(1, 256)),
                argument("y", integer(1, 256)),
                argument("z", integer(1, 256)).executes(andRespondWith(this::digRange))
        ));
    }

    private Component digRange(CommandContext<FabricClientCommandSource> ctx) {
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        int z = IntegerArgumentType.getInteger(ctx, "z");

        config.updateAndSave(c -> c.veins.markRange = new Vector3i(x,y,z));
        return Component.literal(String.format("§aChanged dig range to: %d %d %d", x, y, z));
    }

    private Component digRadius(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        config.updateAndSave(c -> c.veins.markRange = new Vector3i(radius, radius, radius));

        return Component.literal(String.format("§aChanged dig range to: %d %d %d", radius, radius, radius));
    }
}
