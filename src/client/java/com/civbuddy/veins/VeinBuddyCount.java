package com.civbuddy.veins;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 *   /veinbuddy group <name>   - Set group to send count updates to
 *   /veinbuddy key <key>      - Set key for current vein (e.g., "f2da")
 *   /veinbuddy reset          - Reset current vein count to 0
 *   /veinbuddy list           - List all tracked veins
 */
public class VeinBuddyCount {

    private static VeinBuddyCount instance = null;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Configuration
    private String countGroup = "";
    private String currentVeinKey = "";
    
    // Vein tracking
    private final Map<String, VeinCounter> veins = new ConcurrentHashMap<>();
    
    // Callback to mark save as dirty
    private Runnable markDirtyCallback = null;
    
    // Ore detection pattern - detects "You sense a diamond nearby 2 DEEPSLATE_DIAMOND_ORE nearby"
    private static final Pattern ORE_SENSE_PATTERN = Pattern.compile(
        "You sense (?:a |an )?(?:diamond|deepslate.?diamond|iron|gold|copper|redstone|lapis|emerald|coal)s? nearby\\s+(\\d+)\\s+.*",
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
                ClientCommandManager.literal("veinbuddy")
                    .then(ClientCommandManager.literal("group")
                        .then(ClientCommandManager.argument("groupName", StringArgumentType.string())
                            .executes(counter::cmdSetGroup)))
                    .then(ClientCommandManager.literal("key")
                        .then(ClientCommandManager.argument("key", StringArgumentType.string())
                            .executes(counter::cmdSetKey)))
                    .then(ClientCommandManager.literal("reset").executes(counter::cmdReset))
                    .then(ClientCommandManager.literal("list").executes(counter::cmdList))
            );
        });
    }
    
    /**
     * Set callback to mark save as dirty when data changes
     */
    public static void setMarkDirtyCallback(Runnable callback) {
        getInstance().markDirtyCallback = callback;
    }
    
    /**
     * Mark save as dirty
     */
    private void markDirty() {
        if (markDirtyCallback != null) {
            markDirtyCallback.run();
        }
    }
    
    /**
     * Save current state to the provided Save object
     */
    public static void saveToFile(Save.Data save) {
        VeinBuddyCount counter = getInstance();
        save.countGroup = counter.countGroup;
        save.currentVeinKey = counter.currentVeinKey;
        save.veins.clear();
        
        // Convert VeinCounter objects to serializable VeinCounterData
        for (Map.Entry<String, VeinCounter> entry : counter.veins.entrySet()) {
            VeinCounter vc = entry.getValue();
            save.veins.put(entry.getKey(), new Save.VeinCounterData(
                vc.key, vc.count, vc.createdTime, vc.lastUpdateTime
            ));
        }
    }
    
    /**
     * Load state from the provided Save object
     */
    public static void loadFromFile(Save.Data save) {
        VeinBuddyCount counter = getInstance();
        counter.countGroup = save.countGroup != null ? save.countGroup : "";
        counter.currentVeinKey = save.currentVeinKey != null ? save.currentVeinKey : "";
        counter.veins.clear();
        
        // Convert VeinCounterData back to VeinCounter objects
        if (save.veins != null) {
            for (Map.Entry<String, Save.VeinCounterData> entry : save.veins.entrySet()) {
                Save.VeinCounterData data = entry.getValue();
                VeinCounter vc = new VeinCounter(data.key);
                vc.count = data.count;
                vc.createdTime = data.createdTime;
                vc.lastUpdateTime = data.lastUpdateTime;
                counter.veins.put(entry.getKey(), vc);
            }
        }
    }

    /**
     * Vein counter data class
     */
    private static class VeinCounter {
        String key;
        int count;
        long createdTime;
        long lastUpdateTime;
        
        VeinCounter(String key) {
            this.key = key;
            this.count = 0;
            this.createdTime = System.currentTimeMillis();
            this.lastUpdateTime = this.createdTime;
        }
    }

    /**
     * Get or create vein counter
     */
    private VeinCounter getOrCreateVein(String key) {
        return veins.computeIfAbsent(key.toLowerCase(), VeinCounter::new);
    }

    /**
     * Share count update to group
     */
    private void shareCountUpdate(String key, int count) {
        if (countGroup.isEmpty() || mc.getNetworkHandler() == null) {
            return;
        }
        
        String message = String.format("key: %s count: %d", key, count);
        mc.getNetworkHandler().sendChatCommand("g " + countGroup + " " + message);
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
        
        // Auto-detect ore discoveries if we have an active vein key
        if (!currentVeinKey.isEmpty()) {
            Matcher matcher = ORE_SENSE_PATTERN.matcher(msg);
            if (matcher.matches()) {
                // Parse the count from the message
                // "You sense a diamond nearby 1 DEEPSLATE_DIAMOND_ORE nearby" = 1
                // "You sense a diamond nearby 3 DEEPSLATE_DIAMOND_ORE nearby" = 3
                String countStr = matcher.group(1);
                int amount = Integer.parseInt(countStr);
                addToCurrentVein(amount, true); // true = auto-detected
            }
        }
    }

    /**
     * Add amount to current vein
     */
    private void addToCurrentVein(int amount, boolean autoDetected) {
        if (currentVeinKey.isEmpty()) {
            if (!autoDetected && mc.player != null) {
                mc.player.sendMessage(Text.literal("§cNo vein key set! Use /veinbuddy key <key>"), false);
            }
            return;
        }
        
        VeinCounter vein = getOrCreateVein(currentVeinKey);
        vein.count += amount;
        vein.lastUpdateTime = System.currentTimeMillis();
        markDirty(); // Mark save as dirty
        
        // Share update to group
        shareCountUpdate(vein.key, vein.count);
        
        // Notify player
        if (mc.player != null) {
            String prefix = autoDetected ? "§a✓ Auto-detected" : "§b+ Added";
            mc.player.sendMessage(Text.literal(String.format("%s §7+%d → §ekey: %s count: %d", 
                prefix, amount, vein.key, vein.count)), false);
        }
    }

    // ===== COMMANDS =====

    /**
     * Command: Set group
     */
    private int cmdSetGroup(CommandContext<FabricClientCommandSource> ctx) {
        countGroup = StringArgumentType.getString(ctx, "groupName");
        markDirty(); // Mark save as dirty
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§aVeinBuddy count group set to: " + countGroup), false);
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
        
        currentVeinKey = newKey;
        VeinCounter vein = getOrCreateVein(currentVeinKey);
        markDirty(); // Mark save as dirty
        
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
        if (currentVeinKey.isEmpty()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§cNo vein key set!"), false);
            }
            return 1;
        }
        
        VeinCounter vein = getOrCreateVein(currentVeinKey);
        vein.count = 0;
        vein.lastUpdateTime = System.currentTimeMillis();
        markDirty(); // Mark save as dirty
        
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
        if (mc.player != null) {
            if (veins.isEmpty()) {
                mc.player.sendMessage(Text.literal("§7No veins tracked yet"), false);
            } else {
                mc.player.sendMessage(Text.literal("§b━━━ Tracked Veins ━━━"), false);
                veins.values().stream()
                    .sorted((a, b) -> Long.compare(b.lastUpdateTime, a.lastUpdateTime))
                    .forEach(vein -> {
                        String active = vein.key.equals(currentVeinKey) ? " §a✓" : "";
                        mc.player.sendMessage(Text.literal(String.format("§7Key: §e%s §7Count: §a%d%s", 
                            vein.key, vein.count, active)), false);
                    });
                mc.player.sendMessage(Text.literal("§b━━━━━━━━━━━━━━━━━━"), false);
            }
        }
        return 0;
    }
}