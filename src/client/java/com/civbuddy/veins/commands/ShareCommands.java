package com.civbuddy.veins.commands;

import com.civbuddy.common.commands.Command;
import com.civbuddy.veins.VeinShareClient;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.civbuddy.common.commands.CommandUtils.*;

public final class ShareCommands implements Command {
    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        add.accept(List.of(
                literal("share"),
                literal("with").executes(andRespondWith(ShareCommands::shareWithClear)),
                argument("nl", StringArgumentType.string())
                        .suggests(ShareCommands::getNamelayerSuggestions)
                        .executes(andRespondWith(ShareCommands::shareWith))
        ));

        add.accept(List.of(
                literal("share"),
                literal("all").executes(andRespondWith(ShareCommands::shareAll))
        ));
    }

    public static Component shareWithClear(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        VeinShareClient.setGroup("");

        String text = "§aStopped sharing vein.";

        return Component.literal(text);
    }

    private static CompletableFuture<Suggestions> getNamelayerSuggestions(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        ClientPacketListener connection = ctx.getSource().getClient().getConnection();
        if (connection == null) return builder.buildFuture();

        String command = "g " + builder.getRemaining();
        ParseResults<ClientSuggestionProvider> parse = connection.getCommands().parse(command, connection.getSuggestionsProvider());
        SuggestionsBuilder localBuilder = builder.createOffset(builder.getStart());
        return connection.getCommands().getCompletionSuggestions(parse, command.length()).thenApply(suggestions -> {
            suggestions.getList().forEach(suggestion -> localBuilder.suggest(suggestion.getText(), suggestion.getTooltip()));
            return localBuilder.build();
        });
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
