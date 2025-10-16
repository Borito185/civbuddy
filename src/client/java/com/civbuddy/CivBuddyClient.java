package com.civbuddy;

import com.civbuddy.veins.VeinBuddyClient;
import net.fabricmc.api.ClientModInitializer;

public class CivBuddyClient implements ClientModInitializer {
    private VeinBuddyClient veinBuddyClient;

    @Override
    public void onInitializeClient() {
        veinBuddyClient = new VeinBuddyClient();
        veinBuddyClient.onInitializeClient();
    }
}
