package com.civbuddy;

import com.civbuddy.calc.CalculatorClient;
import com.civbuddy.commands.CommandClient;
import com.civbuddy.utils.CommandsHelper;
import com.civbuddy.veins.VeinBuddyClient;
import net.fabricmc.api.ClientModInitializer;

public class CivBuddyClient implements ClientModInitializer {
    public static final String MODID = "civbuddy";
    public static final String COMMAND_ROOT = "civbuddy";

    private VeinBuddyClient veinBuddyClient;
    private CalculatorClient calculatorClient;

    @Override
    public void onInitializeClient() {
        Save.initialize();
        veinBuddyClient = new VeinBuddyClient();
        veinBuddyClient.onInitializeClient();
        calculatorClient = new CalculatorClient();
        calculatorClient.onInitializeClient();
        com.civbuddy.commands.HelpCommand.initialize();

        CommandsHelper.initialize();

        // Initialize bookmark GUI
        CommandClient.initialize();
    }
}
