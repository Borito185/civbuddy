package com.civbuddy.veins.commands;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.common.commands.Command;
import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.VeinShareClient;
import com.civbuddy.veins.config.VeinConfig;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.civbuddy.common.commands.CommandUtils.*;

public class ModifyCommands implements Command {
    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        add.accept(List.of(
                literal("modify"),
                literal("clear").executes(andRespondWith(ModifyCommands::clear))
        ));

        add.accept(List.of(
                literal("modify"),
                literal("applyDigRange").executes(andRespondWith(ModifyCommands::applyDigRange))
        ));

        add.accept(List.of(
                literal("modify"),
                literal("shape"),
                argument("type", StringArgumentType.word())
                        .suggests(ModifyCommands::suggestShapeModes)
                        .executes(andRespondWith(ModifyCommands::setShapeType))
        ));
    }

    private static Component clear(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        long activeVeinId = VeinClient.getActiveVeinId();

        int size = VeinMarkingDao.countForVein(activeVeinId);
        VeinMarkingDao.clearForVein(activeVeinId);
        VeinClient.notifyChange();

        // prevent clear from removing markings for others to prevent griefing
        VeinShareClient.setGroup(VeinShareClient.getSharingGroup());

        return Component.literal(String.format("§aCleared %d markings", size));
    }

    private static Component applyDigRange(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        Vector3i range = VeinClient.config().markRange;
        long activeVeinId = VeinClient.getActiveVeinId();

        VeinMarkingDao.setRangeForVein(activeVeinId, range);
        long veinSize = VeinMarkingDao.countForVein(activeVeinId);

        VeinClient.notifyChange();
        return Component.literal(String.format("§aChanged dig range to: %d %d %d for %d markings", range.x, range.y, range.z, veinSize));
    }

    public static CompletableFuture<Suggestions> suggestShapeModes(
            CommandContext<FabricClientCommandSource> ctx,
            SuggestionsBuilder builder
    ) {
        for (VeinConfig.ShapeMode mode : VeinConfig.ShapeMode.values()) {
            builder.suggest(mode.name());
        }

        return builder.buildFuture();
    }

    public static Component setShapeType(CommandContext<FabricClientCommandSource> ctx) {
        String type = StringArgumentType.getString(ctx, "type");

        try {
            VeinConfig.ShapeMode shapeMode = VeinConfig.ShapeMode.valueOf(type);

            CivBuddyClient.config.updateAndSave(c -> c.veins.shapeMode = shapeMode);

            VeinClient.notifyChange();

            return Component.literal("§aChanged shapemode to " + type);
        } catch (IllegalArgumentException e) {
            return Component.literal("§cUnknown shape mode: " + type);
        }
    }
}
