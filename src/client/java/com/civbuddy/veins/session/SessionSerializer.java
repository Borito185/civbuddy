package com.civbuddy.veins.session;

import com.civbuddy.veins.config.VeinConfig;
import org.joml.Vector3i;

public class SessionSerializer {
    private static final int VERSION = 1;

    public static String encode(SessionConfig cfg) {
        return "v=%d;r=%d,%d,%d;s=%s;t=%d".formatted(
                VERSION,
                cfg.range.x,
                cfg.range.y,
                cfg.range.z,
                cfg.shape.name(),
                cfg.threshold
        );
    }

    public static SessionConfig decode(String namelayer, String message) {
        try {
            String[] parts = message.split(";");

            if (parts.length != 4) return null;

            int version = Integer.parseInt(parts[0].substring(2));
            if (version != 1) return null;

            String[] range = parts[1].substring(2).split(",");
            if (range.length != 3) return null;

            SessionConfig cfg = new SessionConfig();
            cfg.namelayer = namelayer;
            cfg.range = new Vector3i(
                    Integer.parseInt(range[0]),
                    Integer.parseInt(range[1]),
                    Integer.parseInt(range[2])
            );
            cfg.shape = VeinConfig.ShapeMode.valueOf(parts[2].substring(2));
            cfg.threshold = Integer.parseInt(parts[3].substring(2));

            return cfg;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }
}