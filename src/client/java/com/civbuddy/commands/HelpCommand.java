package com.civbuddy.commands;

import com.civbuddy.utils.CommandsHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class HelpCommand implements CommandsHelper.CommandProvider {

    public HelpCommand() {
        // Auto-register when instantiated
        CommandsHelper.register(this);
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return ClientCommandManager.literal("help").executes(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(getHelpText(), false);
            }
            return 1;
        });
    }

    @Override
    public boolean commandsAlias() {
        return false; 
    }

    private static Text getHelpText() {
        MutableText help = Text.literal("\n=== CivBuddy Help ===\n");
        
        help.append(Text.literal("\nVein Marking:\n"));
        help.append(Text.literal("• Hold right-click with pickaxe to place marker\n"));
        help.append(Text.literal("• Quick right-click to remove marker\n"));
        help.append(Text.literal("• /civbuddy digrange <x> <y> <z> - Set mining area\n"));
        help.append(Text.literal("• /civbuddy render - Toggle rendering\n"));
        help.append(Text.literal("• /civbuddy clear - Clear all markers\n"));
        
        help.append(Text.literal("\nVein Counter:\n"));
        help.append(Text.literal("• /civbuddy name <key> - Set vein identifier\n"));
        help.append(Text.literal("• /civbuddy group <name> - Set broadcast group\n"));
        help.append(Text.literal("• /civbuddy reset - Reset current count\n"));
        help.append(Text.literal("• /civbuddy listnames - Show all veins\n"));
        
        help.append(Text.literal("\nCalculator:\n"));
        help.append(Text.literal("• /calc <expression> - Math evaluator\n"));
        help.append(Text.literal("• Shortcuts: s=64, cs=4096, k=1000\n"));
        
        help.append(Text.literal("\nCommand Bookmarks:\n"));
        help.append(Text.literal("• Press \\ (backslash) to open GUI\n"));
        help.append(Text.literal("• Organize commands into categories\n"));
        help.append(Text.literal("• Search, drag-drop, auto history\n"));
        
        help.append(Text.literal("\nAlias: /cb = /civbuddy\n"));
        
        return help;
    }
    public static void initialize() {
        new HelpCommand();
    }
}
