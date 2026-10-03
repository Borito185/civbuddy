package com.civbuddy.veins.commands;

import com.civbuddy.common.commands.Command;
import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinKVStore;
import com.civbuddy.veins.data.VeinRow;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.civbuddy.CivBuddyClient.config;
import static com.civbuddy.common.commands.CommandUtils.*;

public final class ConfigCommands implements Command {
    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        add.accept(List.of(
                literal("toggle").executes(andRespondWith(ConfigCommands::toggle))
        ));

        add.accept(List.of(
                literal("set"),
                argument("veinName", StringArgumentType.string())
                        .suggests(ConfigCommands::veinSuggestions)
                        .executes(andRespondWith(ConfigCommands::setVein))
        ));


    }

    public static Component toggle(CommandContext<FabricClientCommandSource> ctx) {
        config.updateAndSave(c -> c.veins.doRender = !c.veins.doRender);
        VeinClient.notifyChange();
        return Component.literal(String.format("§aVein rendering turned %s", config.get().veins.doRender ? "on" : "off"));
    }

    private static CompletableFuture<Suggestions> veinSuggestions(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder){
        try {
            VeinDao.top(100).stream().map(VeinRow::name).sorted().forEach(builder::suggest);
            return builder.buildFuture();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Command: Set vein key
     */
    public static Component setVein(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        String newKey = StringArgumentType.getString(ctx, "veinName").toLowerCase();

        // Validate key format
        if (!newKey.matches("^[\\p{L}\\p{N}_-]{2,16}$")) {
            return Component.literal("§cInvalid key format! §aUse 2–16 characters: letters, numbers, _ or -.");
        }

        VeinKVStore.setActiveVeinName(newKey);
        VeinRow vein = VeinDao.getOrCreate(newKey);

        VeinClient.notifyChange();

        return Component.literal(String.format("§aVein key set to: §e%s", vein.name()));
    }
}
