package com.civbuddy.veins.listeners;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinKVStore;
import com.civbuddy.veins.data.VeinRow;
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
public class MessageListener {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    
    // Ore detection pattern - detects "You sense a diamond nearby 2 DEEPSLATE_DIAMOND_ORE nearby"
    private static final Pattern ORE_SENSE_PATTERN = Pattern.compile(
        "You sense a diamond nearby\\s+(\\d+)\\s+.*",
        Pattern.CASE_INSENSITIVE
    );
    private static final String[] IGNORE_MSG_CHARACTERS = new String[] {"<", ">", "[", "]"};

    private MessageListener() {}

    public static void initialize() {
        // Register chat listener
        ClientReceiveMessageEvents.GAME.register(MessageListener::onChatMessage);
    }

    /**
     * Handle incoming chat messages
     */
    private static void onChatMessage(Text message, boolean overlay) {
        if (overlay) return;

        String msg = message.getString();

        try {
            checkMessage(msg);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkMessage(String message) throws SQLException {
        if (!VeinKVStore.getDoCountDia()) return;

        // Filter out player chat messages (they contain player names with <> or brackets)
        // Only process system messages (ore detection from the server)
        if (Arrays.stream(IGNORE_MSG_CHARACTERS).anyMatch(message::contains)) {
            // This is likely a player chat message, ignore it
            return;
        }

        String key = VeinKVStore.getActiveVeinName();

        // Auto-detect ore discoveries if we have an active vein key
        if (!key.equals("default")) return;

        Matcher matcher = ORE_SENSE_PATTERN.matcher(message);
        if (matcher.matches()) {
            // Parse the count from the message
            // "You sense a diamond nearby 1 DEEPSLATE_DIAMOND_ORE nearby" = 1
            // "You sense a diamond nearby 3 DEEPSLATE_DIAMOND_ORE nearby" = 3
            String countStr = matcher.group(1);
            int amount = Integer.parseInt(countStr);
            addToCurrentVein(amount);
        }
    }

    /**
     * Add amount to current vein
     */
    private static void addToCurrentVein(int amount) throws SQLException {
        String veinName = VeinKVStore.getActiveVeinName();
        VeinDao.increment(veinName, amount);
        VeinRow vein = VeinDao.getOrCreate(veinName);

        // Notify player
        if (mc.player != null) {
            String prefix = "§a✓ Auto-detected";
            mc.player.sendMessage(Text.literal(String.format("%s §7+%d → §ekey: %s count: %d",
                    prefix, amount, vein.name(), vein.count())), false);
        }
    }
}