package com.civbuddy.veins.commands;

import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;
import java.sql.SQLException;

public final class VeinCommands {
    public static Component clear(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        long activeVeinId = VeinClient.getActiveVeinId();

        int size = VeinMarkingDao.countForVein(activeVeinId);
        VeinMarkingDao.clearForVein(activeVeinId);
        VeinClient.notifyChange();

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
}
