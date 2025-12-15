package com.civbuddy.veins.listeners;

import com.civbuddy.veins.VeinClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class WorldEventListener {
    private WorldEventListener() {}

    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((a, b, c) -> VeinClient.notifyChange());
    }
}
