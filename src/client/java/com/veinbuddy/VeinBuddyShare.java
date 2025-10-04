package com.veinbuddy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3i;

/**
 * VeinBuddy Sharing System 
 * 
 * Commands:
 *   /veinbuddy group <name>   – Set Namelayer group
 *   /veinbuddy toggle         – Enable/disable sharing
 *   /veinbuddy status         – Show current status
 */
public class VeinBuddyShare {

    private static VeinBuddyShare instance = null;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Sharing state
    private boolean sharingEnabled = false;
    private String shareGroup = "";

    // Tracking data
    private final Map<Vec3i, Long> selectionTimestamps = new ConcurrentHashMap<>();
    private final Set<Vec3i> sharedSelections = new ConcurrentSkipListSet<>();

    // Callback to import selections
    private ImportCallback importCallback = null;

    // Diamond detection pattern (only diamond-related messages)
    private static final Pattern DIAMOND_FOUND_PATTERN = Pattern.compile(
        ".*?(?:found|discovered|located|uncovered|revealed).*?(\\d+).*?(?:diamond|deepslate.?diamond).*?(?:ore|vein).*?",
        Pattern.CASE_INSENSITIVE
    );

    @FunctionalInterface
    public interface ImportCallback {
        void addSelection(Vec3i selection, Vec3i range, boolean bulk);
    }

    private VeinBuddyShare() {}

    public static VeinBuddyShare getInstance() {
        if (instance == null) {
            instance = new VeinBuddyShare();
        }
        return instance;
    }

    public static void initialize() {
        VeinBuddyShare sharing = getInstance();
        ClientReceiveMessageEvents.GAME.register(sharing::onChatMessage);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Tick logic is handled externally by VeinBuddyClient
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("veinbuddy")
                    .then(ClientCommandManager.literal("group")
                        .then(ClientCommandManager.argument("groupName", StringArgumentType.string())
                            .executes(sharing::onSetShareGroup)))
                    .then(ClientCommandManager.literal("toggle").executes(sharing::onToggleSharing))
                    .then(ClientCommandManager.literal("status").executes(sharing::onShowStatus))
            );
        });
    }

    public static void setImportCallback(ImportCallback callback) {
        getInstance().importCallback = callback;
    }

    public static void recordNewSelection(Vec3i selection) {
        getInstance().selectionTimestamps.put(selection, System.currentTimeMillis());
    }

    public static void cleanupSelection(Vec3i selection) {
        VeinBuddyShare sharing = getInstance();
        sharing.selectionTimestamps.remove(selection);
        sharing.sharedSelections.remove(selection);
    }

    public static void checkForSharingOpportunities(Set<Vec3i> selections, Map<Vec3i, Vec3i> selectionRanges) {
        VeinBuddyShare sharing = getInstance();
        if (!sharing.sharingEnabled || sharing.shareGroup.isEmpty() ||
            sharing.mc.player == null || sharing.mc.getNetworkHandler() == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        for (Vec3i selection : selections) {
            if (!sharing.sharedSelections.contains(selection)) {
                Long timestamp = sharing.selectionTimestamps.get(selection);
                if (timestamp != null && (currentTime - timestamp) >= 10000) {
                    Vec3i range = selectionRanges.get(selection);
                    if (range != null) {
                        sharing.shareSelection(selection, range);
                        sharing.sharedSelections.add(selection);
                    }
                }
            }
        }
    }

    private void shareSelection(Vec3i selection, Vec3i range) {
        String message = String.format("[VeinBuddy] MARK: %d %d %d %d %d %d",
            selection.getX(), selection.getY(), selection.getZ(),
            range.getX(), range.getY(), range.getZ());
        mc.getNetworkHandler().sendChatCommand("g " + shareGroup + " " + message);
    }

    private void shareDiamondDiscovery(int count) {
        String message = String.format("[VeinBuddy] Found %d diamonds!", count);
        mc.getNetworkHandler().sendChatCommand("g " + shareGroup + " " + message);
    }

    private void onChatMessage(Text message, boolean overlay) {
        if (overlay) return;

        String msg = message.getString();

        // Handle imports
        if (msg.contains("[VeinBuddy]") && msg.contains("MARK:")) {
            if (importCallback != null) {
                handleMarkImport(msg);
            }
            return;
        }

        // Auto-share diamond discoveries
        if (sharingEnabled && !shareGroup.isEmpty()) {
            Matcher matcher = DIAMOND_FOUND_PATTERN.matcher(msg);
            if (matcher.matches()) {
                try {
                    int count = Integer.parseInt(matcher.group(1));
                    shareDiamondDiscovery(count);
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("§6§l Shared diamond discovery (" + count + " diamonds) with group!"), false);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void handleMarkImport(String msg) {
        try {
            String[] parts = msg.split("MARK:", 2)[1].trim().split("\\s+");
            if (parts.length >= 6) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                int rx = Integer.parseInt(parts[3]);
                int ry = Integer.parseInt(parts[4]);
                int rz = Integer.parseInt(parts[5]);
                Vec3i pos = new Vec3i(x, y, z);
                Vec3i range = new Vec3i(rx, ry, rz);
                importCallback.addSelection(pos, range, true);
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("§aImported VeinBuddy mark at " + x + " " + y + " " + z), false);
                }
            }
        } catch (Exception ignored) {}
    }

    // === Commands ===

    private int onSetShareGroup(CommandContext<FabricClientCommandSource> ctx) {
        shareGroup = StringArgumentType.getString(ctx, "groupName");
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§aVeinBuddy sharing group set to: " + shareGroup), false);
        }
        return 0;
    }

    private int onToggleSharing(CommandContext<FabricClientCommandSource> ctx) {
        if (shareGroup.isEmpty()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§cPlease set a group first: /veinbuddy group <name>"), false);
            }
            return 1;
        }
        sharingEnabled = !sharingEnabled;
        String status = sharingEnabled ? "§aENABLED" : "§cDISABLED";
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§bVeinBuddy sharing " + status + " for group: §a" + shareGroup), false);
        }
        return 0;
    }

    private int onShowStatus(CommandContext<FabricClientCommandSource> ctx) {
        String status = String.format("§bSharing: %s §7| §bGroup: %s §7| §bTracked: %d §7| §bShared: %d",
            sharingEnabled ? "§aON" : "§cOFF",
            shareGroup.isEmpty() ? "§cNONE" : "§a" + shareGroup,
            selectionTimestamps.size(),
            sharedSelections.size());
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(status), false);
        }
        return 0;
    }
}