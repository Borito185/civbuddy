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
        help.append(Text.literal("• /veins digRange <x> <y> <z> - Set mining area\n"));
        help.append(Text.literal("• /veins digRadius <r> - Set mining area\n"));
        help.append(Text.literal("• /veins toggleRenderer - Toggle rendering of vein markings\n"));
        help.append(Text.literal("• /veins changeAll digRadius <r> - Changes the radius of all markings\n"));
        help.append(Text.literal("• /veins clear - Clear all markers\n"));
        
        help.append(Text.literal("\nVein Diamond Counter:\n"));
        help.append(Text.literal("• /veins diaTracking set <veinName> - Set vein diamond tracking\n"));
        help.append(Text.literal("• /veins diaTracking shareLive <namelayer> - Set broadcast group\n"));
        help.append(Text.literal("• /veins diaTracking reset - Reset current count\n"));
        help.append(Text.literal("• /veins diaTracking listNames - Show all veins\n"));
        
        help.append(Text.literal("\nCalculator:\n"));
        help.append(Text.literal("• /calc <expression> - Math evaluator\n"));
        help.append(Text.literal("• Shortcuts: s/ci=64, cs=4096, k=1000\n"));
        
        help.append(Text.literal("\nCommand Bookmarks:\n"));
        help.append(Text.literal("• Press \\ (backslash) to open GUI\n"));
        help.append(Text.literal("• Organize commands into categories\n"));
        help.append(Text.literal("• Search, drag-drop, auto history\n"));
        
        help.append(Text.literal("\nAll commands can also be found under /civbuddy\n"));
        help.append(Text.literal("Alias: /cb = /civbuddy\n"));

        return help;
    }
    public static void initialize() {
        new HelpCommand();
    }
}
