package com.civbuddy.veins.session;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.common.commands.CommandGroup;
import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.VeinShareClient;
import com.civbuddy.veins.config.VeinConfig;
import com.civbuddy.veins.session.listeners.DiaOreFoundListener;
import com.civbuddy.veins.session.listeners.MessageListener;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class VeinSessionClient {
    public static final String PREFIX = "[cbS]:";
    public static SessionData activeSession = null;
    public static String me;
    public static final Map<String, SessionConfig> seenConfigs = new HashMap<>();

    public static void initialize(CommandGroup group) {
        group.add(new SessionCommands());
        MessageListener.initialize();
        DiaOreFoundListener.initialize();
    }

    public static boolean isActive() {
        return activeSession != null;
    }

    public static void foundDiamonds(int number) {
        if (!isActive()) return;

        number += activeSession.playerDimmies.getOrDefault(me, 0);
        setPersonDiamonds(me, number);

        Messenger.sendCountUpdate(me, number);
    }

    public static void setPersonDiamonds(String name, int n) {
        if (!isActive()) return;

        activeSession.playerDimmies.put(name, n);
    }

    public static void startSession(String namelayer) {
        SessionConfig cfg = SessionConfig.create(namelayer);
        setSession(cfg);
        sendConfig();
    }

    public static void setSession(SessionConfig scfg) {
        try {
            if (scfg == null) {
                activeSession = null;
                VeinShareClient.setGroup("");

                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                Random random = new Random();

                me = ""
                        + chars.charAt(random.nextInt(chars.length()))
                        + chars.charAt(random.nextInt(chars.length()))
                        + " "
                        + Minecraft.getInstance().player.getDisplayName().getString();

                return;
            }

            // if no session or diff. Create new one
            if (!isActive() || !Objects.equals(activeSession.namelayer, scfg.namelayer)) {
                activeSession = new SessionData();
                activeSession.namelayer = scfg.namelayer;
            }

            VeinConfig config = VeinClient.config();

            VeinShareClient.setGroup(scfg.namelayer);
            config.doRender = true;
            config.markRange = scfg.range;
            config.borderThreshold = scfg.threshold;
            config.shapeMode = scfg.shape;

            CivBuddyClient.config.save();
            foundDiamonds(0);
            VeinClient.notifyChange();
        } catch (Exception e) {}
    }

    public static void sendConfig() {
        if (!isActive()) return;

        SessionConfig cfg = SessionConfig.create(activeSession.namelayer);
        Messenger.sendSessionConfig(cfg);
    }

    public static class SessionData {
        public String namelayer;
        public final Map<String, Integer> playerDimmies = new HashMap<>();
    }
}
