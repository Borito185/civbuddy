package com.civbuddy;

import com.civbuddy.calc.CalculatorClient;
import com.civbuddy.veins.VeinBuddyClient;
import net.fabricmc.api.ClientModInitializer;

public class CivBuddyClient implements ClientModInitializer {
    private VeinBuddyClient veinBuddyClient;
    private CalculatorClient calculatorClient;

    @Override
    public void onInitializeClient() {
        Save.initialize();
        veinBuddyClient = new VeinBuddyClient();
        veinBuddyClient.onInitializeClient();
        calculatorClient = new CalculatorClient();
        calculatorClient.onInitializeClient();
    }
}
