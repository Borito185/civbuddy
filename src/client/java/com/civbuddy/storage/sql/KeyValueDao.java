package com.civbuddy.storage.sql;

import java.sql.SQLException;
import java.util.*;

public final class KeyValueDao {
    private KeyValueDao() {}

    public static Optional<String> get(String key) throws SQLException {
        String sql = "SELECT value FROM key_value WHERE key = ?";
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setString(1, key);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public static void put(String key, String value) throws SQLException {
        String sql = """
            INSERT INTO key_value(key, value) VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """;
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}