package com.civbuddy.veins;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.civbuddy.Save;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * VeinBuddy Count - Lightweight vein tracking for miners
 * 
 * Tracks ore discoveries per vein using simple keys.
 * Miners set a key for their vein, and discoveries auto-update the count.
 * 
 * Commands:
 *   /civbuddy group <name>   - Set group to send count updates to
 *   /civbuddy name <key>     - Set key for current vein (e.g., "f2da")
 *   /civbuddy reset          - Reset current vein count to 0
 *   /civbuddy listnames      - List all tracked veins
 */
public class VeinBuddyCount {

    private static VeinBuddyCount instance = null;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    
    // Ore detection pattern - detects "You sense a diamond nearby 2 DEEPSLATE_DIAMOND_ORE nearby"
    private static final Pattern ORE_SENSE_PATTERN = Pattern.compile(
        "You sense a diamond nearby\\s+(\\d+)\\s+.*",
        Pattern.CASE_INSENSITIVE
    );

    private VeinBuddyCount() {}

    public static VeinBuddyCount getInstance() {
        if (instance == null) {
            instance = new VeinBuddyCount();
        }
        return instance;
    }

    public static void initialize() {
        VeinBuddyCount counter = getInstance();
        
        // Register chat listener
        ClientReceiveMessageEvents.GAME.register(counter::onChatMessage);
        
        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("civbuddy")
                    .then(ClientCommandManager.literal("group")
                        .then(ClientCommandManager.argument("groupName", StringArgumentType.string())
                            .executes(counter::cmdSetGroup)))
                    .then(ClientCommandManager.literal("name")
                        .then(ClientCommandManager.argument("key", StringArgumentType.string())
                            .executes(counter::cmdSetKey)))
                    .then(ClientCommandManager.literal("reset").executes(counter::cmdReset))
                    .then(ClientCommandManager.literal("listnames").executes(counter::cmdList))
            );
        });
    }


    /**
     * Get or create vein counter
     */
    private Save.VeinCounterData getOrCreateVein(String key) {
        Map<String, Save.VeinCounterData> veins = Save.data.veins;
        return veins.computeIfAbsent(key.toLowerCase(), Save.VeinCounterData::new);
    }

    /**
     * Share count update to group
     */
    private void shareCountUpdate(String key, int count) {
        if (!hasCountGroup() || mc.getNetworkHandler() == null) {
            return;
        }
        
        String message = String.format("key: %s count: %d", key, count);
        mc.getNetworkHandler().sendChatCommand("g " + Save.data.countGroup + " " + message);
    }

    /**
     * Handle incoming chat messages
     */
    private void onChatMessage(Text message, boolean overlay) {
        if (overlay) return;
        
        String msg = message.getString();
        
        // Filter out player chat messages (they contain player names with <> or brackets)
        // Only process system messages (ore detection from the server)
        if (msg.contains("<") || msg.contains(">") || (msg.contains("[") && msg.contains("]"))) {
            // This is likely a player chat message, ignore it
            return;
        }

        String key = Save.data.currentVeinKey;

        // Auto-detect ore discoveries if we have an active vein key
        if (!hasKey()) return;

        Matcher matcher = ORE_SENSE_PATTERN.matcher(msg);
        if (matcher.matches()) {
            // Parse the count from the message
            // "You sense a diamond nearby 1 DEEPSLATE_DIAMOND_ORE nearby" = 1
            // "You sense a diamond nearby 3 DEEPSLATE_DIAMOND_ORE nearby" = 3
            String countStr = matcher.group(1);
            int amount = Integer.parseInt(countStr);
            addToCurrentVein(amount);
        }
    }

    private boolean hasKey() {
        String key = Save.data.currentVeinKey;
        return key != null && !key.isBlank();
    }

    private boolean hasCountGroup() {
        String group = Save.data.countGroup;
        return group != null && !group.isBlank();
    }

    /**
     * Add amount to current vein
     */
    private void addToCurrentVein(int amount) {
        if (!hasKey()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§cNo vein key set! Use /civbuddy name <key>"), false);
            }
            return;
        }

        Save.VeinCounterData vein = getOrCreateVein(Save.data.currentVeinKey);
        vein.count += amount;
        Save.save();
        
        // Share update to group
        shareCountUpdate(vein.key, vein.count);
        
        // Notify player
        if (mc.player != null) {
            String prefix = "§a✓ Auto-detected";
            mc.player.sendMessage(Text.literal(String.format("%s §7+%d → §ekey: %s count: %d", 
                prefix, amount, vein.key, vein.count)), false);
        }
    }

    // ===== COMMANDS =====

    /**
     * Command: Set group
     */
    private int cmdSetGroup(CommandContext<FabricClientCommandSource> ctx) {
        Save.data.countGroup = StringArgumentType.getString(ctx, "groupName");
        Save.save();

        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§aCivBuddy count group set to: " + Save.data.countGroup), false);
        }
        return 0;
    }

    /**
     * Command: Set vein key
     */
    private int cmdSetKey(CommandContext<FabricClientCommandSource> ctx) {
        String newKey = StringArgumentType.getString(ctx, "key").toLowerCase();
        
        // Validate key format (alphanumeric, 2-8 chars)
        if (!newKey.matches("^[a-z0-9]{2,8}$")) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§cInvalid key format! Use 2-8 alphanumeric characters (e.g., f2da)"), false);
            }
            return 1;
        }

        Save.data.currentVeinKey = newKey;
        Save.save();

        Save.VeinCounterData vein = getOrCreateVein(Save.data.currentVeinKey);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(String.format("§aVein key set to: §e%s §7(count: %d)", 
                vein.key, vein.count)), false);
        }
        return 0;
    }

    /**
     * Command: Reset current vein
     */
    private int cmdReset(CommandContext<FabricClientCommandSource> ctx) {
        if (!hasKey()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§cNo vein key set!"), false);
            }
            return 1;
        }

        Save.VeinCounterData vein = getOrCreateVein(Save.data.currentVeinKey);
        vein.count = 0;
        Save.save();
        
        // Share reset
        shareCountUpdate(vein.key, vein.count);
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(String.format("§aReset §ekey: %s count: 0", vein.key)), false);
        }
        return 0;
    }

    /**
     * Command: List all veins
     */
    private int cmdList(CommandContext<FabricClientCommandSource> ctx) {
        if (mc.player == null) {
            return 0;
        }
        Map<String, Save.VeinCounterData> veins = Save.data.veins;

        if (veins.isEmpty()) {
            mc.player.sendMessage(Text.literal("§7No veins tracked yet"), false);
            return 0;
        }
        mc.player.sendMessage(Text.literal("§b━━━ Tracked Veins ━━━"), false);
        veins.values().stream()
            .sorted((a, b) -> Long.compare(b.count, a.count))
            .forEach(vein -> {
                String active = vein.key.equals(Save.data.currentVeinKey) ? " §a✓" : "";
                mc.player.sendMessage(Text.literal(String.format("§7Key: §e%s §7Count: §a%d%s",
                    vein.key, vein.count, active)), false);
            });
        mc.player.sendMessage(Text.literal("§b━━━━━━━━━━━━━━━━━━"), false);
        return 0;
    }
}