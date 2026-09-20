package com.civbuddy;

import com.civbuddy.calc.CalculatorClient;
import com.civbuddy.commands.CommandClient;
import com.civbuddy.commands.data.CommandDao;
import com.civbuddy.commands.data.CommandMigrations;
import com.civbuddy.common.commands.CommandManager;
import com.civbuddy.common.compat.CompatManager;
import com.civbuddy.common.compat.migrations.LoadOldSave;
import com.civbuddy.common.compat.migrations.MigrateCommandsToSql;
import com.civbuddy.snitch.SnitchClient;
import com.civbuddy.common.storage.config.GlobalConfig;
import com.civbuddy.common.storage.config.JsonConfig;
import com.civbuddy.common.storage.sql.DatabaseManager;
import com.civbuddy.common.storage.sql.KeyValueMigrations;
import com.civbuddy.common.ui.MenuListener;
import com.civbuddy.veins.VeinClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CivBuddyClient implements ClientModInitializer {
    public static final String MODID = "civbuddy";
    public static final KeyMapping.Category CIVBUDDY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("civbuddy", "civbuddy"));

    public static JsonConfig<GlobalConfig> config;
    public static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    @Override
    public void onInitializeClient() {
        // --- Init Config ---
        config = JsonConfig.of(
            MODID,
            GlobalConfig.class,
            GlobalConfig::new
        );

        // --- Init UI ---
        MenuListener.initialize();

        // --- Init Compat ---
        CompatManager.initialize();

        // --- Init Database ---
        DatabaseManager.initialize();
        DatabaseManager.register(KeyValueMigrations.migrations());
        DatabaseManager.register(CommandMigrations.migrations());


        // --- Init Features ---
        VeinClient.onInitializeClient();
        CalculatorClient.onInitializeClient();
        SnitchClient.onInitializeClient();
        CommandClient.initialize();

        // --- Init (/)Commands ---
        CommandManager.addRoot("civbuddy");
        CommandManager.addRoot("cb");
        CommandManager.addRoot("");
        CommandManager.initialize();

        // --- Init Migrations ---
        ClientPlayConnectionEvents.JOIN.register((a, b, c) -> {
            LoadOldSave.migrate();
            MigrateCommandsToSql.migrate();
            CommandDao.getInstance().initialize();
        });
    }
}
