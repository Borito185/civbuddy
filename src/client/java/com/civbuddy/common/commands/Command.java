package com.civbuddy.common.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.List;
import java.util.function.Consumer;

public interface Command {
    void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add);
}
