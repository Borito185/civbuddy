package com.civbuddy;

import com.civbuddy.veins.VeinBuddyClient;
import com.civbuddy.veins.VeinBuddyCount;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class CivBuddyClient implements ClientModInitializer {
    private VeinBuddyClient veinBuddyClient;

    @Override
    public void onInitializeClient() {
        Save.initialize();
        veinBuddyClient = new VeinBuddyClient();
        veinBuddyClient.onInitializeClient();
    }
}
