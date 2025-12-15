package com.civbuddy.veins.data;

import com.civbuddy.storage.sql.DatabaseManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VeinDao {
    private VeinDao() {}

    public static void upsert(VeinRow row) throws SQLException {
        String sql = """
            INSERT INTO vein(name, count) VALUES (?, ?)
            ON CONFLICT(name) DO UPDATE SET count = excluded.count
        """;
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            row.bind(ps, 1);
            ps.executeUpdate();
        }
    }

    /** Adds delta to the existing count, or creates the row if missing. */
    public static void increment(String name, int delta) throws SQLException {
        String sql = """
            INSERT INTO vein(name, count) VALUES (?, ?)
            ON CONFLICT(name) DO UPDATE SET count = count + excluded.count
        """;
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, delta);
            ps.executeUpdate();
        }
    }

    public static Optional<VeinRow> get(String name) throws SQLException {
        String sql = "SELECT name, count FROM vein WHERE name = ?";
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(VeinRow.of(rs)) : Optional.empty();
            }
        }
    }

    public static VeinRow getOrCreate(String name) throws SQLException {
        Optional<VeinRow> veinRow = get(name);
        if (veinRow.isPresent()) {
            return veinRow.get();
        }

        VeinRow row = new VeinRow(name, 0);
        upsert(row);
        return row;
    }

    public static List<VeinRow> top(int limit) throws SQLException {
        String sql = "SELECT name, count FROM vein ORDER BY count DESC LIMIT ?";
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (var rs = ps.executeQuery()) {
                List<VeinRow> out = new ArrayList<>();
                while (rs.next()) out.add(VeinRow.of(rs));
                return out;
            }
        }
    }

    public static int countRows() throws SQLException {
        try (var ps = DatabaseManager.connection().prepareStatement("SELECT COUNT(*) FROM vein");
             var rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public static void clear() throws SQLException {
        try (var ps = DatabaseManager.connection().prepareStatement("DELETE FROM vein")) {
            ps.executeUpdate();
        }
    }

    public static boolean delete(String name) throws SQLException {
        String sql = "DELETE FROM vein WHERE name = ?";
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        }
    }

    public static long getOrCreateId(String name) throws SQLException {
        String select = "SELECT id FROM vein WHERE name = ?";
        try (var ps = DatabaseManager.connection().prepareStatement(select)) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        String insert = "INSERT INTO vein(name, count) VALUES (?, 0)";
        try (var ps = DatabaseManager.connection().prepareStatement(insert)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }

        try (var ps = DatabaseManager.connection().prepareStatement(select)) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
