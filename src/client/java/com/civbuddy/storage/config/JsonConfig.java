package com.civbuddy.storage.config;

import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static com.civbuddy.serializers.GSONSerializer.GSON;

/**
 * Simple JSON config wrapper for Fabric + Gson.
 *
 * Usage:
 *   public static final JsonConfig<Config> CONFIG =
 *       JsonConfig.of("civbuddy", Config.class, Config::new);
 *
 *   // during init:
 *   Config cfg = CONFIG.loadOrCreateAndSave();
 *
 *   // later:
 *   CONFIG.save(cfg);
 */
public final class JsonConfig<T> {
    @FunctionalInterface
    public interface Factory<T> { T create(); }
    @FunctionalInterface
    public interface ConfigUpdate<T> { void update(T c); }

    private final String fileName;
    private final Class<T> type;
    private final Factory<T> defaultsFactory;

    private T loaded;

    private JsonConfig(String fileName, Class<T> type, Factory<T> defaultsFactory) {
        this.fileName = fileName.endsWith(".json") ? fileName : fileName + ".json";
        this.type = type;
        this.defaultsFactory = defaultsFactory;
        reload();
    }

    public static <T> JsonConfig<T> of(String fileName, Class<T> type, Factory<T> defaultsFactory) {
        return new JsonConfig<>(fileName, type, defaultsFactory);
    }

    /** config/<fileName>.json */
    public Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(fileName);
    }

    public T get() {
        if (loaded == null) {
            return reload();
        }

        return loaded;
    }

    public void updateAndSave(ConfigUpdate<T> func) {
        func.update(get());
        save();
    }

    public T reload() {
        loaded = load();
        save();
        return loaded;
    }

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(
                    path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE
            )) {
                GSON.toJson(loaded, writer);
            }
        } catch (IOException e) {
            // Optional: log this via your mod logger
        }
    }

    private T load() {
        Path path = path();

        if (!Files.exists(path)) {
            return defaultsFactory.create();
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T loaded = GSON.fromJson(reader, type);
            return loaded != null ? loaded : defaultsFactory.create();
        } catch (IOException | JsonSyntaxException e) {
            // If the JSON is corrupted/unreadable, fall back to defaults.
            return defaultsFactory.create();
        }
    }
}
