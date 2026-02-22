package com.civbuddy.migrations;

import com.civbuddy.commands.data.CommandCategory;
import com.civbuddy.commands.data.CommandDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;


// One-time migration: reads the old global commands.json and inserts into the per-world SQLite database.
// The JSON file is kept in place so other worlds can also import on first connect.
 
public class MigrateCommandsToSql {
    private static final String COMMANDS_FILE = "config/civbuddy/commands.json";

    public static void migrate() {
        try {
            // Only import if the DB has no categories yet
            if (CommandDao.getInstance().categoryCount() > 0) return;

            File jsonFile = new File(Minecraft.getInstance().gameDirectory, COMMANDS_FILE);
            if (!jsonFile.exists()) return;

            Gson gson = new GsonBuilder().create();
            try (FileReader reader = new FileReader(jsonFile)) {
                Type listType = new TypeToken<List<CommandCategory>>(){}.getType();
                List<CommandCategory> loaded = gson.fromJson(reader, listType);
                if (loaded != null && !loaded.isEmpty()) {
                    CommandDao.getInstance().insertCategoriesToDb(loaded);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
