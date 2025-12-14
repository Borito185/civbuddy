package com.civbuddy.veins.commands;

import com.civbuddy.utils.CommandsHelper;
import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinRow;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import static com.civbuddy.utils.CommandsHelper.andRespondWith;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class CommandHandler implements CommandsHelper.CommandProvider {
    public static void initialize() {
        CommandsHelper.register(new CommandHandler());
    }
    private CommandHandler() { }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return literal("veins")
                .then(literal("digRange").then(argument("x", integer(1, 11)).then(argument("y", integer(1, 11)).then(argument("z", integer(1, 11))
                        .executes(andRespondWith(ConfigCommands::setDigRange))))))
                .then(literal("digRadius").then(argument("radius", integer(1, 11))
                        .executes(andRespondWith(ConfigCommands::setDigRadius))))
                .then(literal("clearAll")
                        .executes(andRespondWith(VeinCommands::clear)))
                .then(literal("toggleRenderer")
                        .executes(andRespondWith(ConfigCommands::toggleRenderer)))
                .then(literal("changeAll")
                        .then(literal("digRadius").then(argument("radius", integer(1, 11))
                                .executes(andRespondWith(VeinCommands::onChangeAllDigRange)))))
                .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("veinName", StringArgumentType.string())
                                .suggests(CommandHandler::getSuggestions)
                                .executes(andRespondWith(ConfigCommands::cmdSetKey))))
                .then(ClientCommandManager.literal("list").executes(andRespondWith(InfoCommands::cmdList)));
    }

    @Override
    public boolean commandsAlias() { return true; }

    private static CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder){
        try {
            VeinDao.top(100).stream().map(VeinRow::name).sorted().forEach(builder::suggest);
            return builder.buildFuture();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
