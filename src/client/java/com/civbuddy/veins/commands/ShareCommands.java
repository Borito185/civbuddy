package com.civbuddy.veins.commands;

import com.civbuddy.veins.VeinShareClient;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import java.sql.SQLException;

public final class ShareCommands {
    public static Component shareWithClear(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        VeinShareClient.setGroup("");

        String text = "§aStopped sharing vein.";

        return Component.literal(text);
    }

    public static Component shareWith(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        String namelayer = StringArgumentType.getString(ctx, "nl");

        namelayer = namelayer.strip();

        if (namelayer == "!") {
            return Component.literal("Hell nah, you're not sharing anything with global...");
        }

        VeinShareClient.setGroup(namelayer);

        String text = "§aNow sharing vein with group '§o" + namelayer + "§r§a'!";

        return Component.literal(text);
    }

    public static Component shareAll(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        VeinShareClient.resendAll();
        String text = "§aSharing all markings!";

        return Component.literal(text);
    }
}
