package com.civbuddy.veins.session;

import com.civbuddy.veins.serializers.Base91;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;

import static com.civbuddy.veins.session.VeinSessionClient.PREFIX;

public class Messenger {
    public static void sendCountUpdate(String me, int number) {
        String namelayer = VeinSessionClient.activeSession.namelayer;

        String raw = me + ":" + namelayer + ":" + number;
        raw += ":" + raw.hashCode();

        String encoded = Base91.encode(raw.getBytes(StandardCharsets.UTF_8));
        sendToNL(namelayer, encoded);
    }

    public static void sendSessionConfig(SessionConfig cfg) {
        String encoded = SessionConfig.encode(cfg);
        sendToNL(cfg.namelayer, encoded);
    }

    private static void sendToNL(String nl, String msg) {
        Minecraft.getInstance().player.connection.sendCommand(
                "g " + nl + " " + PREFIX + msg
        );
    }
}
