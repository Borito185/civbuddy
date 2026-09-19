package com.civbuddy.snitch.listeners;

import com.civbuddy.snitch.SnitchClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class WorldDisconnectListener {
    private WorldDisconnectListener() {}

    public static void initialize() {
        ClientPlayConnectionEvents.DISCONNECT.register((a, b) -> {
            SnitchClient.clear();
        });
    }
}
