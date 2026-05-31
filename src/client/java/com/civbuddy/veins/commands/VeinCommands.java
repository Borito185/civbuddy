package com.civbuddy.veins.commands;

import com.civbuddy.CivBuddy;
import com.civbuddy.CivBuddyClient;
import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.VeinShareClient;
import com.civbuddy.veins.config.VeinConfig;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public final class VeinCommands {
    public static Component clear(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        long activeVeinId = VeinClient.getActiveVeinId();

        int size = VeinMarkingDao.countForVein(activeVeinId);
        VeinMarkingDao.clearForVein(activeVeinId);
        VeinClient.notifyChange();

        // prevent clear from removing markings for others to prevent griefing
        VeinShareClient.setGroup(VeinShareClient.getSharingGroup());

        return Component.literal(String.format("§aCleared %d markings", size));
    }

    public static Component onChangeAllDigRange(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        int rad = IntegerArgumentType.getInteger(ctx, "radius");
        Vector3i radius = new Vector3i(rad);
        long activeVeinId = VeinClient.getActiveVeinId();

        VeinMarkingDao.setRangeForVein(activeVeinId, radius);
        long veinSize = VeinMarkingDao.countForVein(activeVeinId);

        VeinClient.notifyChange();
        return Component.literal(String.format("§aChanged dig range to: %d %d %d for %d markings", rad, rad, rad, veinSize));
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

    public static CompletableFuture<Suggestions> suggestShapeModes(
            CommandContext<FabricClientCommandSource> ctx,
            SuggestionsBuilder builder
    ) {
        for (VeinConfig.ShapeMode mode : VeinConfig.ShapeMode.values()) {
            builder.suggest(mode.name());
        }

        return builder.buildFuture();
    }
}
