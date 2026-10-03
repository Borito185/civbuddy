package com.civbuddy.common.utils.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Vector3IArgument implements ArgumentType<Vector3i> {

    private static final SimpleCommandExceptionType INVALID_POSITION =
            new SimpleCommandExceptionType(Component.literal("Invalid position"));

    public static Vector3IArgument vector3i() {
        return new Vector3IArgument();
    }

    public static <S> Vector3i getVector3i(CommandContext<S> context, String name) {
        return context.getArgument(name, Vector3i.class);
    }

    @Override
    public Vector3i parse(StringReader reader) throws CommandSyntaxException {
        var player = Minecraft.getInstance().player;

        if (player == null)
            throw INVALID_POSITION.createWithContext(reader);

        Vector3i origin = new Vector3i(
                player.getBlockX(),
                player.getBlockY(),
                player.getBlockZ()
        );

        int x = readCoordinate(reader, origin.x);

        expectSpace(reader);

        int y = readCoordinate(reader, origin.y);

        expectSpace(reader);

        int z = readCoordinate(reader, origin.z);

        return new Vector3i(x, y, z);
    }

    private int readCoordinate(StringReader reader, int origin)
            throws CommandSyntaxException {

        if (!reader.canRead())
            throw INVALID_POSITION.createWithContext(reader);

        if (reader.peek() == '~') {
            reader.skip();

            if (!reader.canRead() || reader.peek() == ' ')
                return origin;

            return origin + reader.readInt();
        }

        return reader.readInt();
    }

    private void expectSpace(StringReader reader)
            throws CommandSyntaxException {

        if (!reader.canRead() || reader.peek() != ' ')
            throw INVALID_POSITION.createWithContext(reader);

        reader.skip();

        while (reader.canRead() && reader.peek() == ' ')
            reader.skip();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            CommandContext<S> context,
            SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemaining();

        if (remaining.isEmpty()) {
            builder.suggest("~ ~ ~");

            var player = Minecraft.getInstance().player;
            if (player != null) {
                builder.suggest(
                        player.getBlockX() + " "
                                + player.getBlockY() + " "
                                + player.getBlockZ()
                );
            }
        }

        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "0 64 0",
                "~ ~ ~",
                "~10 ~-5 ~20",
                "-100 70 250"
        );
    }
}