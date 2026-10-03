package com.civbuddy.veins.session;

import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.config.VeinConfig;
import com.civbuddy.veins.data.VeinKVStore;
import org.joml.Vector3i;

import java.sql.SQLException;

public class SessionConfig {
    public String namelayer;
    public Vector3i range = new Vector3i(5);
    public VeinConfig.ShapeMode shape = VeinConfig.ShapeMode.Cuboid;
    public int threshold = 1;
    public boolean invite;
    public String key;

    public static SessionConfig create(boolean isInvite) throws SQLException {
        String namelayer = VeinSessionClient.isActive() ? VeinSessionClient.activeSession.namelayer : "";
        return create(namelayer, isInvite);
    }

    public static SessionConfig create(String namelayer, boolean isInvite) throws SQLException {
        VeinConfig config = VeinClient.config();

        SessionConfig scfg = new SessionConfig();
        scfg.namelayer = namelayer;
        scfg.range = config.markRange;
        scfg.threshold = config.borderThreshold;
        scfg.shape = config.shapeMode;
        scfg.invite = isInvite;
        scfg.key = VeinKVStore.getActiveVeinName();

        return scfg;
    }

    private static final int VERSION = 1;

    public static String encode(SessionConfig cfg) {
        return "v=%d;r=%d,%d,%d;s=%d;t=%d;i=%b;k=%s".formatted(
                VERSION,
                cfg.range.x,
                cfg.range.y,
                cfg.range.z,
                cfg.shape.ordinal(),
                cfg.threshold,
                cfg.invite,
                cfg.key
        );
    }

    public static SessionConfig decode(String namelayer, String message) {
        try {
            String[] parts = message.split(";");

            int version = Integer.parseInt(parts[0].substring(2));

            String[] range = parts[1].substring(2).split(",");
            if (range.length < 3) return null;

            SessionConfig cfg = new SessionConfig();
            cfg.namelayer = namelayer;
            cfg.range = new Vector3i(
                    Integer.parseInt(range[0]),
                    Integer.parseInt(range[1]),
                    Integer.parseInt(range[2])
            );
            cfg.shape = VeinConfig.ShapeMode.values()[Integer.parseInt(parts[2].substring(2))];
            cfg.threshold = Integer.parseInt(parts[3].substring(2));
            cfg.invite = Boolean.parseBoolean(parts[4].substring(2));
            cfg.key = parts[5].substring(2);

            return cfg;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }
}