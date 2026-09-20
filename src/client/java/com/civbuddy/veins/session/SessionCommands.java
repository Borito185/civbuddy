package com.civbuddy.veins.session;

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
import net.minecraft.network.chat.MutableComponent;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import static com.civbuddy.common.commands.CommandUtils.*;

public class SessionCommands implements Command {
    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        add.accept(List.of(
                literal("session"),
                literal("start"),
                argument("nl", StringArgumentType.string())
                        .suggests(SessionCommands::namelayerSuggestions)
                        .executes(andRespondWith(SessionCommands::start))
        ));

        add.accept(List.of(
                literal("session"),
                literal("invite").executes(andRespondWith(SessionCommands::invite))
        ));

        add.accept(List.of(
                literal("session"),
                literal("join"),
                argument("nl", StringArgumentType.string())
                        .suggests(SessionCommands::sessionSuggestions)
                        .executes(andRespondWith(SessionCommands::join))
        ));

        add.accept(List.of(
                literal("session"),
                literal("stats").executes(andRespondWith(SessionCommands::stats))
        ));

        add.accept(List.of(
                literal("session"),
                literal("shareAll").executes(andRespondWith(SessionCommands::shareAll))
        ));

        add.accept(List.of(
                literal("session"),
                literal("leave").executes(andRespondWith(SessionCommands::leave))
        ));
    }

    private static CompletableFuture<Suggestions> namelayerSuggestions(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
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

    private static CompletableFuture<Suggestions> sessionSuggestions(CommandContext<FabricClientCommandSource> fabricClientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        Set<String> strings = VeinSessionClient.seenConfigs.keySet();

        strings.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private static Component start(CommandContext<FabricClientCommandSource> ctx) {
        String namelayer = StringArgumentType.getString(ctx, "nl");
        namelayer = namelayer.strip();
        if (namelayer == "!") {
            return Component.literal("Hell nah, you're not sharing anything with global...");
        }

        VeinSessionClient.startSession(namelayer);
        return Component.literal(String.format("§aStarted a new session on §e%s!", namelayer));
    }

    private static Component invite(CommandContext<FabricClientCommandSource> ctx) {
        VeinSessionClient.sendConfig();
        return Component.literal("§aResend the invitation to the namelayer!");
    }

    private static Component join(CommandContext<FabricClientCommandSource> ctx) {
        String namelayer = StringArgumentType.getString(ctx, "nl");
        namelayer = namelayer.strip();
        if (namelayer == "!") {
            return Component.literal("§aHell nah, you're not sharing anything with global...");
        }

        SessionConfig session = VeinSessionClient.seenConfigs.getOrDefault(namelayer, null);
        if (session == null) {
            return Component.literal("§aHaven't received an invite yet...");
        }

        VeinSessionClient.setSession(session);

        return Component.literal(String.format("§aJoined session on §e%s!", session.namelayer));
    }

    public static Component shareAll(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        VeinShareClient.resendAll();
        return Component.literal("§aSharing all markings!");
    }

    private static Component stats(CommandContext<FabricClientCommandSource> ctx) {
        if (!VeinSessionClient.isActive()) {
            return Component.literal("§cNo active session");
        }

        Map<String, Integer> playerDimmies =
                VeinSessionClient.activeSession.playerDimmies;

        MutableComponent message = Component.literal("§aSession Info");

        if (playerDimmies.isEmpty()) {
            return message.append("\n§7No diamonds found yet.");
        }

        playerDimmies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> message.append(String.format(
                        "\n§f%s§7: §b%d",
                        entry.getKey(),
                        entry.getValue()
                )));

        return message;
    }

    private static Component leave(CommandContext<FabricClientCommandSource> ctx) {
        VeinSessionClient.setSession(null);
        return Component.literal("§aLeft the mining session");
    }
}
