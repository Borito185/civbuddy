package com.civbuddy.snitch.utils;

import com.civbuddy.snitch.SnitchClient;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JAItemHelper {
    private static final Pattern POSITION_PATTERN =
            Pattern.compile("^\\[(-?\\d+) (-?\\d+) (-?\\d+)]$");

    private static final Pattern LOCATION_PATTERN =
            Pattern.compile("^Location:\\s+\\S+\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)$");


    public static Optional<Integer> getHighlight(ItemStack stack) {
        if (!isJAItem(stack)) {
            return Optional.empty();
        }

        return toHighlightColor(stack);
    }

    public static boolean isJAItem(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        int count = 0;

        for (Component line : lore.lines()) {
            String text = line.getString();

            if (text.startsWith("Player: ") || text.startsWith("Time: ")) {
                count++;
            }
        }

        return count > 0;
    }

    private static Optional<Integer> toHighlightColor(ItemStack stack) {
        // if filtering and this aint it. skip
        if (SnitchClient.filterByPositions && !SnitchClient.positions.isEmpty()) {
            Optional<Vector3i> position = getPosition(stack);

            if (position.isEmpty()) return Optional.empty();
            if (!SnitchClient.positions.contains(position.get())) return Optional.empty();
        }

        String name = stack.getHoverName().getString() + " ";

        for (Map.Entry<String, Integer> entry : SnitchClient.eventColorMap.entrySet()) {
            if (name.startsWith(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }

        System.out.println(name);
        return Optional.empty();
    }

    public static Optional<Vector3i> getPosition(ItemStack stack) {
        if (!isJAItem(stack)) return Optional.empty();

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return Optional.empty();

        for (Component line : lore.lines()) {
            String text = line.getString().trim();

            Matcher matcher = POSITION_PATTERN.matcher(text);

            if (!matcher.matches()) {
                matcher = LOCATION_PATTERN.matcher(text);
            }

            if (matcher.matches()) {
                return Optional.of(new Vector3i(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                ));
            }
        }

        return Optional.empty();
    }
}
