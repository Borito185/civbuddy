package com.civbuddy.compat;

import com.civbuddy.CivBuddy;
import net.fabricmc.loader.api.FabricLoader;

public class CompatManager {
    public static void initialize() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("iris")) {
            try {
                IrisCompat.initialize();
            } catch (Exception e) {
                String irisVersion = loader
                        .getModContainer("iris")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown");

                CivBuddy.LOGGER.warn(
                        "Failed to load Iris compatibility (Iris version: {})",
                        irisVersion,
                        e
                );
            }
        }
    }
}
