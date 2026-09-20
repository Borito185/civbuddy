package com.civbuddy.common.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class CommandManager {
    private static final List<String> roots = new ArrayList<>();
    private static final List<Command> commands = new ArrayList<>();
    public static void register(Command cmd) {
        commands.add(cmd);
    }
    public static void addRoot(String root) {
        roots.add(root);
    }

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess) -> {
            List<LiteralArgumentBuilder<FabricClientCommandSource>> rootBuilders = new ArrayList<>();
            for (String root : roots) rootBuilders.add(literal(root));

            for (Command command : commands) {
                command.bind(nodes -> {
                    ArgumentBuilder<FabricClientCommandSource, ?> current = nodes.getLast();

                    for (int i = nodes.size()-2; i >= 0; i--) {
                        ArgumentBuilder<FabricClientCommandSource, ?> next = nodes.get(i);
                        next.then(current);
                        current = next;
                    }

                    for (var root : rootBuilders) {
                        root.then(current);
                    }
                    dispatcher.register((LiteralArgumentBuilder<FabricClientCommandSource>) current);
                });
            }

            rootBuilders.forEach(dispatcher::register);
        });
    }
}
