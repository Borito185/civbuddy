package com.civbuddy.storage.sql;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

public final class DatabaseManager {
    private static Connection conn;
    private static String currentKey;
    private static final List<Migration> MIGRATIONS = new ArrayList<>();

    private DatabaseManager() {}

    /* ===================== PUBLIC API ===================== */

    public static void register(Migration... migrations) {
        MIGRATIONS.addAll(Arrays.asList(migrations));
    }

    public static synchronized Connection connection() throws SQLException {
        String key = getWorldKey();
        if (key == null)
            throw new IllegalStateException("No world/server active yet");

        if (conn == null || !key.equals(currentKey)) {
            reopen(key);
        }
        return conn;
    }

    public static synchronized void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {}
        conn = null;
        currentKey = null;
    }

    /* ===================== INTERNAL ===================== */

    private static void reopen(String key) throws SQLException {
        close();

        String url = buildUrl(key);
        conn = DriverManager.getConnection(
                url + "?foreign_keys=on&journal_mode=WAL&busy_timeout=5000"
        );

        Migrator.migrate(conn, MIGRATIONS);
        currentKey = key;
    }

    private static String getWorldKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverInfo = client.getCurrentServerEntry();
        IntegratedServer server = client.getServer();

        if (server != null) {
            return sanitize(server.getSaveProperties().getLevelName());
        }

        if (serverInfo != null) {
            return sanitize(serverInfo.address);
        }

        return null;
    }

    private static String buildUrl(String key) {
        Path dataDir = FabricLoader.getInstance()
                .getGameDir()
                .resolve("data")
                .resolve("civbuddy");

        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DB directory", e);
        }

        Path dbPath = dataDir.resolve(key + ".db");
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    private static String sanitize(String key) {
        return key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}