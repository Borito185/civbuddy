package com.civbuddy.veins;

import com.civbuddy.CivBuddy;
import com.civbuddy.utils.ChatHelper;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.civbuddy.veins.serializers.ShareMarkingSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;
import org.jspecify.annotations.NonNull;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VeinShareClient {
    private static final String PREPEND = "[cb]:";
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
        ClientTickEvents.END_CLIENT_TICK.register(VeinShareClient::heartbeat);
        ClientReceiveMessageEvents.GAME.register(VeinShareClient::onChatMessage);
    }

    private static void onChatMessage(Component component, boolean b) {
        if (!isSharing()) return;
        String msg = component.getString();
        if (!msg.contains(PREPEND)) return;

        String regex = "(?i)^\\[" + Pattern.quote(sharingGroup) + "\\].*?"
                + Pattern.quote(PREPEND)
                + "(\\S+)";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(msg);
        try {
            if (m.find()) {
                String encoded = m.group(1);
                List<ShareMarking> decoded = ShareMarkingSerializer.decode(encoded);
                for (ShareMarking shareMarking : decoded) {
                    if (shareMarking.isRemove) {
                        VeinMarkingDao.delete(sharingVein, shareMarking.pos);
                        known.removeIf(marking -> marking.pos().equals(shareMarking.pos));
                    } else {
                        VeinMarkingRow row = new VeinMarkingRow(sharingVein, shareMarking.pos, shareMarking.range);
                        VeinMarkingDao.upsert(row);
                        known.add(row);
                    }
                }
                VeinClient.notifyChange();
            }
        } catch (Exception ignored) {}
    }

    private static long tick = 0;
    private static void heartbeat(Minecraft minecraft) {
        try {
            // only run every 20 ticks
            if (tick++ % 20 != 0) return;

            // check if sharing
            if (!isSharing()) return;

            // compare vein key
            //   diff: clear and quit
            //   same: continue
            long currentVein = -2;
            try {
                currentVein = VeinClient.getActiveVeinId();
            } catch (Exception ignored) {
            }

            if (sharingVein != currentVein) {
                sharingVein = -1;
                sharingGroup = "";
                ChatHelper.say(Component.literal("§aVein sharing cancelled by swapping vein"));
            }

            // increment time on staged ones
            for (ShareMarking s : stage) {
                s.age++;
            }

            // find & stage diff
            try {
                stageDiff();
            } catch (SQLException ignored) {
            }

            // commit staged diff's above threshold

            stage.removeIf(s -> {
                if (s.age <= 5) return false;
                commiting.add(s);

                VeinMarkingRow row = new VeinMarkingRow(sharingVein, s.pos, s.range);
                if (s.isRemove) {
                    known.remove(row);
                } else {
                    known.add(row);
                }

                return true; // remove from stage
            });

            commitMarkings();
        } catch (Exception ignored) {}
    }

    private static void stageDiff() throws SQLException {
        Set<VeinMarkingRow> current = new HashSet<>(VeinMarkingDao.findAllForVein(sharingVein));

        Set<VeinMarkingRow> added = new HashSet<>(current);
        added.removeAll(known);

        Set<VeinMarkingRow> removed = new HashSet<>(known);
        removed.removeAll(current);

        for (VeinMarkingRow row : added) {
            stage.add(new ShareMarking(row, false));
        }

        for (VeinMarkingRow row : removed) {
            stage.add(new ShareMarking(row, true));
        }

        stage.removeIf(s -> {
            VeinMarkingRow row = new VeinMarkingRow(sharingVein, s.pos, s.range);
            if (!s.isRemove && !added.contains(row)) return true;
            if (s.isRemove && !removed.contains(row)) return true;
            return false;
        });
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

    private static void commitMarkings() {
        if (commiting.isEmpty()) return;

        List<ShareMarking> batch = new ArrayList<>();
        int maxLen = 220;

        for (ShareMarking s : commiting) {
            batch.add(s);

            String encoded = ShareMarkingSerializer.encode(batch);
            if (encoded.length() > maxLen) {
                batch.remove(batch.size() - 1);
                break;
            }
        }

        batch.forEach(commiting::remove);

        Minecraft.getInstance().player.connection.sendCommand("g " + sharingGroup + " " + PREPEND + ShareMarkingSerializer.encode(batch));
    }

    private static boolean isSharing() {
        return sharingVein != -1 && sharingGroup != null && !sharingGroup.isBlank();
    }
}
