package com.civbuddy.snitch.utils;

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
    private final static Map<String, Integer> EventColorMap = Map.ofEntries(
            Map.entry("Break",  0x80FF3B30), // red
            Map.entry("Place",  0x80FFD60A), // yellow
            Map.entry("Killed",   0x808B0000), // dark red
            Map.entry("Opened", 0x80FF6B35), // red-orange
            Map.entry("Enter",  0x8034C759), // green
            Map.entry("Exit",   0x800A84FF)  // blue
    );
    private static final Pattern POSITION_PATTERN =
            Pattern.compile("^\\[(-?\\d+) (-?\\d+) (-?\\d+)]$");


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
        String name = stack.getHoverName().getString();

        for (Map.Entry<String, Integer> entry : EventColorMap.entrySet()) {
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
            Matcher matcher = POSITION_PATTERN.matcher(line.getString().trim());

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
