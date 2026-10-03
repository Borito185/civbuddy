package com.civbuddy.common.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandGroup implements Command {
    private final List<Command> commands = new ArrayList<>();
    private final String help;
    private final List<String> names;

    public CommandGroup(String name) { this(List.of(name), ""); }
    public CommandGroup(String name, String help) { this(List.of(name), help); }
    public CommandGroup(List<String> names) { this(names, ""); }
    public CommandGroup(List<String> names, String help) {
        this.names = names;
        this.help = help;
    }

    public void add(Command cmd) {
        commands.add(cmd);
    }

    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        List<LiteralArgumentBuilder<FabricClientCommandSource>> roots = new ArrayList<>();

        for (String name : names) {
            LiteralArgumentBuilder<FabricClientCommandSource> lit = literal(name);

            if (help != null && !help.isBlank()) {
                lit.executes(CommandUtils.help(help));
            }

            roots.add(lit);
        }

        for (Command command : commands) {
            command.bind(list -> {
                for (var root : roots) {
                    List<ArgumentBuilder<FabricClientCommandSource, ?>> path = new ArrayList<>();
                    path.add(root);
                    path.addAll(list);

                    add.accept(path);
                }
            });
        }
    }
}
