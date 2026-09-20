package com.civbuddy.veins.session;

import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.config.VeinConfig;
import org.joml.Vector3i;

public class SessionConfig {
    public String namelayer;
    public Vector3i range = new Vector3i(5);
    public VeinConfig.ShapeMode shape = VeinConfig.ShapeMode.Cuboid;
    public int threshold = 1;

    public static SessionConfig create(String namelayer) {
        VeinConfig config = VeinClient.config();

        SessionConfig scfg = new SessionConfig();
        scfg.namelayer = namelayer;
        scfg.range = config.markRange;
        scfg.threshold = config.borderThreshold;
        scfg.shape = config.shapeMode;

        return scfg;
    }
}