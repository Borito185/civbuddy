package com.civbuddy;

import com.civbuddy.calc.CalculatorClient;
import com.civbuddy.commands.CommandClient;
import com.civbuddy.commands.HelpCommand;
import com.civbuddy.compat.CompatManager;
import com.civbuddy.migrations.LoadOldSave;
import com.civbuddy.storage.config.GlobalConfig;
import com.civbuddy.storage.config.JsonConfig;
import com.civbuddy.storage.sql.DatabaseManager;
import com.civbuddy.storage.sql.KeyValueMigrations;
import com.civbuddy.utils.CommandsHelper;
import com.civbuddy.veins.VeinClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class CivBuddyClient implements ClientModInitializer {
    public static final String MODID = "civbuddy";
    public static JsonConfig<GlobalConfig> config;

    @Override
    public void onInitializeClient() {
        // --- Init Config ---
        config = JsonConfig.of(
            MODID,
            GlobalConfig.class,
            GlobalConfig::new
        );

        // --- Init Compat ---
        CompatManager.initialize();

        // --- Init Database ---
        DatabaseManager.initialize();
        DatabaseManager.register(KeyValueMigrations.migrations());


        // --- Init Features ---
        VeinClient.onInitializeClient();
        CalculatorClient.onInitializeClient();
        HelpCommand.initialize();
        CommandClient.initialize();


        // --- Init (/)Commands ---
        CommandsHelper.initialize();

        // --- Init Migrations ---
        ClientPlayConnectionEvents.JOIN.register((a, b, c) -> LoadOldSave.migrate());
    }
}
