package com.civbuddy.veins.commands;

import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinKVStore;
import com.civbuddy.veins.data.VeinRow;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.joml.Vector3i;

import java.sql.SQLException;

import static com.civbuddy.CivBuddyClient.config;

public final class ConfigCommands {
    public static Text setDigRange(CommandContext<FabricClientCommandSource> ctx) {
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        int z = IntegerArgumentType.getInteger(ctx, "z");

        config.updateAndSave(c -> c.veins.markRange = new Vector3i(x,y,z));
        return Text.literal(String.format("§aChanged dig range to: %d %d %d", x, y, z));
    }

    public static Text setDigRadius(CommandContext<FabricClientCommandSource> ctx) {
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        config.updateAndSave(c -> c.veins.markRange = new Vector3i(radius,radius,radius));

        return Text.literal(String.format("§aChanged dig range to: %d %d %d", radius, radius, radius));
    }

    public static Text toggleRenderer(CommandContext<FabricClientCommandSource> ctx) {
        config.updateAndSave(c -> c.veins.doRender = !c.veins.doRender);
        VeinClient.notifyChange();
        return Text.literal(String.format("§aVein rendering turned %s", config.get().veins.doRender ? "on" : "off"));
    }

    /**
     * Command: Set vein key
     */
    public static Text setVein(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        String newKey = StringArgumentType.getString(ctx, "veinName").toLowerCase();

        // Validate key format
        if (!newKey.matches("^[\\p{L}\\p{N}_-]{2,16}$")) {
            return Text.literal("§cInvalid key format! §aUse 2–16 characters: letters, numbers, _ or -.");
        }

        VeinKVStore.setActiveVeinName(newKey);
        VeinRow vein = VeinDao.getOrCreate(newKey);

        VeinClient.notifyChange();

        return Text.literal(String.format("§aVein key set to: §e%s", vein.name()));
    }
}
