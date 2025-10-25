package com.civbuddy.commands;

import com.civbuddy.CivBuddyClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BookmarkManager {
    private static BookmarkManager instance;
    private final List<BookmarkCategory> categories = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String BOOKMARK_FILE = "config/civbuddy/commands.json";
    private static final String PREBUILT_FILE = "/assets/civbuddy/config/prebuilt_commands.json";

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
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            loadPrebuiltCommands();
        }
    }

    private void ensureHistoryExists() {
        // Always ensure History category exists
        boolean hasHistory = categories.stream().anyMatch(cat -> cat.getName().equals("History"));
        if (!hasHistory) {
            categories.add(new BookmarkCategory("History", 0xAAAAAA));
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

    private void loadPrebuiltCommands() {
        InputStreamReader inputStreamReader;
        try (InputStream resourceAsStream = CivBuddyClient.class.getResourceAsStream(PREBUILT_FILE)) {
            assert resourceAsStream != null;
            inputStreamReader = new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<BookmarkCategory>>(){}.getType();
            List<BookmarkCategory> loaded = gson.fromJson(inputStreamReader, listType);
            if (loaded != null) {
                categories.clear();
                categories.addAll(loaded);
                ensureHistoryExists();
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
