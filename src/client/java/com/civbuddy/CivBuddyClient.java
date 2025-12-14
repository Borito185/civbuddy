package com.civbuddy;

import com.civbuddy.calc.CalculatorClient;
import com.civbuddy.commands.CommandClient;
import com.civbuddy.commands.HelpCommand;
import com.civbuddy.storage.config.GlobalConfig;
import com.civbuddy.storage.config.JsonConfig;
import com.civbuddy.storage.sql.DatabaseManager;
import com.civbuddy.storage.sql.KeyValueDao;
import com.civbuddy.storage.sql.KeyValueMigrations;
import com.civbuddy.utils.CommandsHelper;
import com.civbuddy.veins.VeinClient;
import net.fabricmc.api.ClientModInitializer;

public class CivBuddyClient implements ClientModInitializer {
    public static final String MODID = "civbuddy";
    public static JsonConfig<GlobalConfig> config;

    @Override
    public void onInitializeClient() {
        config = JsonConfig.of(
            MODID,
            GlobalConfig.class,
            GlobalConfig::new
        );

        DatabaseManager.register(KeyValueMigrations.migrations());

        VeinClient.onInitializeClient();
        CalculatorClient.onInitializeClient();
        HelpCommand.initialize();

        CommandsHelper.initialize();

        // Initialize bookmark GUI
        CommandClient.initialize();
    }
}
