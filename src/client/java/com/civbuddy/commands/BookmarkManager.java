package com.civbuddy.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BookmarkManager {
    private static BookmarkManager instance;
    private final List<BookmarkCategory> categories = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String BOOKMARK_FILE = "config/civbuddy_bookmarks.json";
    private static final String PREBUILT_FILE = "config/civbuddy_prebuilt_commands.json";

    private BookmarkManager() {}

    public static BookmarkManager getInstance() {
        if (instance == null) {
            instance = new BookmarkManager();
        }
        return instance;
    }

    public void loadBookmarks() {
        File file = new File(MinecraftClient.getInstance().runDirectory, BOOKMARK_FILE);

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<BookmarkCategory>>(){}.getType();
                List<BookmarkCategory> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    categories.clear();
                    categories.addAll(loaded);
                    ensureHistoryExists();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        createDefaultCategories();
    }

    private void ensureHistoryExists() {
        // Always ensure History category exists
        boolean hasHistory = categories.stream().anyMatch(cat -> cat.getName().equals("History"));
        if (!hasHistory) {
            categories.add(new BookmarkCategory("History", 0xAAAAAA));
            saveBookmarks();
        }

        // Always ensure Destinations category exists
        boolean hasDestinations = categories.stream().anyMatch(cat -> cat.getName().equals("Destinations"));
        if (!hasDestinations) {
            categories.add(new BookmarkCategory("Destinations", 0x55FF55));
            saveBookmarks();
        }
    }

    public void saveBookmarks() {
        File file = new File(MinecraftClient.getInstance().runDirectory, BOOKMARK_FILE);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(categories, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDefaultCategories() {
        categories.clear();

        // Load prebuilt commands from config
        PrebuiltCommands prebuilt = loadPrebuiltCommands();

        // PVP category
        BookmarkCategory pvp = new BookmarkCategory("PVP", 0xFF5555);
        if (prebuilt != null && prebuilt.pvp != null) {
            for (CommandEntry entry : prebuilt.pvp) {
                pvp.addEntry(new BookmarkEntry(entry.command, entry.command));
            }
        }
        categories.add(pvp);

        // Destinations category
        BookmarkCategory destinations = new BookmarkCategory("Destinations", 0x55FF55);
        if (prebuilt != null && prebuilt.destinations != null) {
            for (CommandEntry entry : prebuilt.destinations) {
                destinations.addEntry(new BookmarkEntry(entry.command, entry.command));
            }
        }
        categories.add(destinations);

        // History category (always empty at start)
        categories.add(new BookmarkCategory("History", 0xAAAAAA));

        saveBookmarks();
    }

    private PrebuiltCommands loadPrebuiltCommands() {
        File file = new File(MinecraftClient.getInstance().runDirectory, PREBUILT_FILE);

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, PrebuiltCommands.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Inner class for deserializing prebuilt commands JSON
    private static class PrebuiltCommands {
        List<CommandEntry> destinations;
        List<CommandEntry> pvp;
    }

    private static class CommandEntry {
        String command;
    }

    public void addCategory(BookmarkCategory category) {
        categories.add(category);
    }

    public void removeCategory(BookmarkCategory category) {
        categories.remove(category);
    }

    public List<BookmarkCategory> getCategories() {
        return categories;
    }

    public BookmarkCategory getHistoryCategory() {
        return categories.stream()
                .filter(cat -> cat.getName().equals("History"))
                .findFirst()
                .orElse(null);
    }

    public void addToHistory(BookmarkEntry entry) {
        BookmarkCategory history = getHistoryCategory();
        if (history != null) {
            history.getEntries().removeIf(e -> e.getCommand().equals(entry.getCommand()));
            history.getEntries().add(0, new BookmarkEntry(entry.getName(), entry.getCommand()));
            while (history.getEntries().size() > 20) {
                history.getEntries().remove(history.getEntries().size() - 1);
            }
            saveBookmarks();
        }
    }
}
