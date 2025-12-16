package com.civbuddy.compat;

import net.fabricmc.loader.api.FabricLoader;

public class CompatManager {
    public static void initialize() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("iris")) {
            IrisCompat.initialize();
        }
    }
}
