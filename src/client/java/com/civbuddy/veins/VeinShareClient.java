package com.civbuddy.veins;

import com.civbuddy.utils.ChatHelper;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.civbuddy.veins.serializers.ShareMarkingSerializer;
import com.sun.jna.platform.unix.solaris.LibKstat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.joml.Vector3i;
import org.jspecify.annotations.NonNull;
import java.sql.SQLException;
import java.util.*;

public final class VeinShareClient {
    public static final String PREPEND = "[cb]:";
    private static String sharingGroup = "";
    private static long sharingVein = -1;
    private static final Set<VeinMarkingRow> known = new HashSet<>();
    private static final SortedSet<ShareMarking> commiting = new TreeSet<>();
    private static final Set<ShareMarking> stage = new HashSet<>();

    public static final class ShareMarking implements Comparable<ShareMarking> {
        public Vector3i pos;
        public Vector3i range;
        public boolean isRemove;
        public int age = 0;

        public ShareMarking(VeinMarkingRow row, boolean isRemove) {
            this.pos = row.pos();
            this.range = row.range();
            this.isRemove = isRemove;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ShareMarking that)) return false;
            return isRemove == that.isRemove && Objects.equals(pos, that.pos) && Objects.equals(range, that.range);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, range, isRemove);
        }

        @Override
        public int compareTo(@NonNull ShareMarking o) {
            int c;
            if ((c = Integer.compare(pos.x, o.pos.x)) != 0) return c;
            if ((c = Integer.compare(pos.y, o.pos.y)) != 0) return c;
            return Integer.compare(pos.z, o.pos.z);
        }
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(VeinShareClient::onTick);
    }

    private static long tick = 0;
    private static void onTick(Minecraft minecraft) {
        try {
            if (!isSharing()) return;

            drawSharingIndicator();

            // only run every 20 ticks
            if (tick++ % 20 == 0) {
                if (!isSameVein()) return;
                ageStagedChanges();
                findNewChanges();
                commitMarkings();
            }
        } catch (Exception ignored) {}
    }

    private static boolean isSameVein() throws SQLException {
        long currentVein = -2;
        try {
            currentVein = VeinClient.getActiveVeinId();
        } catch (Exception ignored) {
        }

        if (sharingVein != currentVein) {
            setGroup("");
            ChatHelper.say(Component.literal("§aVein sharing cancelled by swapping vein"));
            return false;
        }

        return true;
    }

    private static void ageStagedChanges() {
        for (ShareMarking s : stage) {
            s.age++;
        }
    }

    private static void findNewChanges() throws SQLException {
        // compare what is currently in db with what is known and add to stage
        Set<VeinMarkingRow> current = new HashSet<>(VeinMarkingDao.findAllForVein(sharingVein));

        Set<VeinMarkingRow> added = new HashSet<>(current); added.removeAll(known);
        Set<VeinMarkingRow> removed = new HashSet<>(known); removed.removeAll(current);

        for (VeinMarkingRow row : added)   stage.add(new ShareMarking(row, false));
        for (VeinMarkingRow row : removed) stage.add(new ShareMarking(row, true));

        // remove elements from stage if they are no longer in db
        stage.removeIf(s -> {
            VeinMarkingRow row = new VeinMarkingRow(sharingVein, s.pos, s.range);
            if (!s.isRemove && !added.contains(row)) return true;
            if (s.isRemove && !removed.contains(row)) return true;
            return false;
        });

        // commit staged diff's above threshold
        stage.removeIf(s -> {
            if (s.age <= 5) return false;

            addToKnown(s, true);

            return true; // remove from stage
        });
    }

    private static void commitMarkings() {
        if (commiting.isEmpty()) return;

        int maxLen = 220;

        List<ShareMarking> list = new ArrayList<>(commiting);
        int lo = 1;
        int hi = Math.min(64, list.size());
        int best = 0;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;

            String encoded = ShareMarkingSerializer.encode(list.subList(0, mid));

            if (encoded.length() <= maxLen) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        if (best == 0) return;

        List<ShareMarking> batch = list.subList(0, best);
        String encoded = ShareMarkingSerializer.encode(batch);

        // remove committed
        for (int i = 0; i < best; i++) {
            commiting.remove(list.get(i));
        }

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendCommand(
                    "g " + sharingGroup + " " + PREPEND + encoded
            );
        }
    }

    public static void addToKnown(ShareMarking marking, boolean share) {
        if (share) {
            commiting.add(marking);
        }

        VeinMarkingRow row = new VeinMarkingRow(sharingVein, marking.pos, marking.range);
        if (marking.isRemove) known.remove(row);
        else known.add(row);
    }

    public static void setGroup(String namelayer) throws SQLException {
        sharingGroup = namelayer;
        sharingVein = VeinClient.getActiveVeinId();
        known.clear();
        known.addAll(VeinMarkingDao.findAllForVein(sharingVein));
    }

    public static boolean resendAll() throws SQLException {
        if (!isSharing())
            return false;

        for (VeinMarkingRow marking : VeinMarkingDao.findAllForVein(sharingVein)) {
            commiting.add(new ShareMarking(marking, false));
        }

        return true;
    }

    public static boolean isSharing() {
        return sharingVein != -1 && sharingGroup != null && !sharingGroup.isBlank();
    }

    public static String getSharingGroup() {
        return sharingGroup;
    }

    public static long getSharingVein() {
        return sharingVein;
    }

    private static void drawSharingIndicator() {
        int count = commiting.size() + stage.size();

        String msg = "§6CivBuddy: §aSharing vein markings with: §6§o" + sharingGroup;
        msg += count != 0 ? " §r§a(sending " + count + ")" : "";
        Component text = Component.literal(msg);

        Minecraft mc = Minecraft.getInstance();
        mc.player.displayClientMessage(text, true);
    }
}
