package com.civbuddy.commands.data;

import com.civbuddy.CivBuddyClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private static CommandManager instance;
    private final List<CommandCategory> categories = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String COMMANDS_FILE = "config/civbuddy/commands.json";
    private static final String PREBUILT_FILE = "/assets/civbuddy/config/prebuilt_commands.json";

    private CommandManager() {}

    public static CommandManager getInstance() {
        if (instance == null) { instance = new CommandManager(); }
        return instance;
    }

    public void loadCommands() {
        File file = new File(Minecraft.getInstance().gameDirectory, COMMANDS_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<CommandCategory>>(){}.getType();
                List<CommandCategory> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    categories.clear();
                    categories.addAll(loaded);
                    ensureHistoryExists();
                }
            } catch (Exception e) { e.printStackTrace(); }
        } else {
            loadPrebuiltCommands();
        }
    }

    private void ensureHistoryExists() {
        boolean hasHistory = categories.stream().anyMatch(cat -> cat.getName().equals("History"));
        if (!hasHistory) {
            categories.add(new CommandCategory("History", 0xAAAAAA));
            saveCommands();
        }
    }

    public void saveCommands() {
        File file = new File(Minecraft.getInstance().gameDirectory, COMMANDS_FILE);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(categories, writer);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadPrebuiltCommands() {
        try (InputStream resourceAsStream = CivBuddyClient.class.getResourceAsStream(PREBUILT_FILE)) {
            assert resourceAsStream != null;
            InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<CommandCategory>>(){}.getType();
            List<CommandCategory> loaded = gson.fromJson(inputStreamReader, listType);
            if (loaded != null) {
                categories.clear();
                categories.addAll(loaded);
                ensureHistoryExists();
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void addCategory(CommandCategory category) { categories.add(category); }
    public void removeCategory(CommandCategory category) { categories.remove(category); }
    public List<CommandCategory> getCategories() { return categories; }

    public CommandCategory getHistoryCategory() {
        return categories.stream().filter(cat -> cat.getName().equals("History")).findFirst().orElse(null);
    }

    public void addToHistory(CommandEntry entry) {
        CommandCategory history = getHistoryCategory();
        if (history != null) {
            history.getEntries().removeIf(e -> e.getCommand().equals(entry.getCommand()));
            history.getEntries().add(0, new CommandEntry(entry.getName(), entry.getCommand()));
            while (history.getEntries().size() > 20) {
                history.getEntries().remove(history.getEntries().size() - 1);
            }
            saveCommands();
        }
    }
}
