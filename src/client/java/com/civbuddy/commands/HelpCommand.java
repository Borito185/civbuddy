package com.civbuddy.commands;

import com.civbuddy.utils.CommandsHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

public class HelpCommand implements CommandsHelper.CommandProvider {
    public HelpCommand() {
        // Auto-register when instantiated
        CommandsHelper.register(this);
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return ClientCommandManager.literal("help").executes(ctx -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.displayClientMessage(getHelpText(), false);
            }
            return 1;
        });
    }

    @Override
    public boolean commandsAlias() {
        return false; 
    }

    private static Component getHelpText() {
        MutableComponent help = Component.literal("\n=== CivBuddy Help ===\n");
        
        help.append(Component.literal("\nVein Marking:\n"));
        help.append(Component.literal("• Hold right-click with pickaxe to place marker\n"));
        help.append(Component.literal("• Quick right-click to remove marker\n"));
        help.append(Component.literal("• /veins digRange <x> <y> <z> - Set mining area\n"));
        help.append(Component.literal("• /veins digRadius <r> - Set mining area\n"));
        help.append(Component.literal("• /veins toggleRenderer - Toggle rendering of vein markings\n"));
        help.append(Component.literal("• /veins changeAll digRadius <r> - Changes the radius of all markings\n"));
        help.append(Component.literal("• /veins clear - Clear all markers of current vein\n"));
        help.append(Component.literal("• /veins set - Switch to either a new or existing vein\n"));
        help.append(Component.literal("• /veins info - View information about the current vein\n"));

        help.append(Component.literal("\nCalculator:\n"));
        help.append(Component.literal("• /calc <expression> - Math evaluator\n"));
        help.append(Component.literal("• Shortcuts: b=9, s/ci=64, cs=4096, k=1000\n"));
        help.append(Component.literal("• Example: '/calc 2ci + 1b' -> 137\n"));

        help.append(Component.literal("\nCommand Bookmarks:\n"));
        help.append(Component.literal("• Press \\ (backslash) to open GUI\n"));
        help.append(Component.literal("• Organize commands into categories\n"));
        help.append(Component.literal("• Search, drag-drop, auto history\n"));
        
        help.append(Component.literal("\nAll commands can also be found under /civbuddy\n"));
        help.append(Component.literal("Alias: /cb = /civbuddy\n"));

        return help;
    }
    public static void initialize() {
        new HelpCommand();
    }
}
