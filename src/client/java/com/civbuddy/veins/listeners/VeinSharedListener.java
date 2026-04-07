package com.civbuddy.veins.listeners;

import com.civbuddy.CivBuddy;
import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.VeinShareClient;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.civbuddy.veins.serializers.ShareMarkingSerializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.civbuddy.CivBuddyClient.WORKER;

public final class VeinSharedListener {

    public static void initialize() {
        ClientReceiveMessageEvents.GAME.register(VeinSharedListener::onChatMessage);
    }

    private static void onChatMessage(Component component, boolean b) {
        if (!VeinShareClient.isSharing()) return;

        String msg = component.getString();
        if (!msg.contains(VeinShareClient.PREPEND)) return;

        // Offload everything heavy
        WORKER.submit(() -> handleMessage(msg));
    }

    private static void handleMessage(String msg) {
        try {
            List<VeinShareClient.ShareMarking> markings = extractMarkings(msg);
            if (markings == null || markings.isEmpty()) return;

            processIncoming(markings);

            VeinClient.notifyChange();

        } catch (Exception e) {
            CivBuddy.LOGGER.error("Async error", e);
        }
    }

    private static List<VeinShareClient.ShareMarking> extractMarkings(String msg) {
        List<VeinShareClient.ShareMarking> result = null;

        String regex = "(?i)^\\[" + Pattern.quote(VeinShareClient.getSharingGroup()) + "\\].*?"
                + Pattern.quote(VeinShareClient.PREPEND)
                + "(\\S+)";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(msg);
        if (m.find()) {
            String encoded = m.group(1);
            result = ShareMarkingSerializer.decode(encoded);
        }

        return result != null ? result : new ArrayList<>();
    }

    private static void processIncoming(List<VeinShareClient.ShareMarking> markings) throws SQLException {
        for (VeinShareClient.ShareMarking shareMarking : markings) {
            VeinShareClient.addToKnown(shareMarking, false);
            if (shareMarking.isRemove) {
                VeinMarkingDao.delete(VeinShareClient.getSharingVein(), shareMarking.pos);
            } else {
                VeinMarkingRow row = new VeinMarkingRow(VeinShareClient.getSharingVein(), shareMarking.pos, shareMarking.range);
                VeinMarkingDao.upsert(row);
            }
        }
        VeinClient.notifyChange();
    }
}
