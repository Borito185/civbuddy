package com.civbuddy.veins.data.markings;

import com.civbuddy.storage.sql.DatabaseManager;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VeinMarkingDao {
    private VeinMarkingDao() {}

    /** Insert or update by (vein_id, pos). */
    public static void upsert(VeinMarkingRow row) throws SQLException {
        String sql = """
            INSERT INTO vein_marking(vein_id, pos_x, pos_y, pos_z, range_x, range_y, range_z)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(vein_id, pos_x, pos_y, pos_z)
            DO UPDATE SET
                range_x = excluded.range_x,
                range_y = excluded.range_y,
                range_z = excluded.range_z
        """;

        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            row.bind(ps, 1);
            ps.executeUpdate();
        }
    }

    public static Optional<VeinMarkingRow> get(long veinId, Vector3i pos) throws SQLException {
        String sql = """
            SELECT vein_id, pos_x, pos_y, pos_z, range_x, range_y, range_z
            FROM vein_marking
            WHERE vein_id = ? AND pos_x = ? AND pos_y = ? AND pos_z = ?
        """;

        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, veinId);
            ps.setInt(i++, pos.x);
            ps.setInt(i++, pos.y);
            ps.setInt(i++, pos.z);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(VeinMarkingRow.of(rs)) : Optional.empty();
            }
        }
    }

    /** All markings for a vein (good for GUI lists). */
    public static List<VeinMarkingRow> findAllForVein(long veinId) throws SQLException {
        String sql = """
            SELECT vein_id, pos_x, pos_y, pos_z, range_x, range_y, range_z
            FROM vein_marking
            WHERE vein_id = ?
            ORDER BY pos_y, pos_x, pos_z
        """;

        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setLong(1, veinId);

            try (var rs = ps.executeQuery()) {
                List<VeinMarkingRow> out = new ArrayList<>();
                while (rs.next()) out.add(VeinMarkingRow.of(rs));
                return out;
            }
        }
    }

    /** Delete one marking for a vein at a position. */
    public static boolean delete(long veinId, Vector3ic pos) throws SQLException {
        String sql = """
            DELETE FROM vein_marking
            WHERE vein_id = ? AND pos_x = ? AND pos_y = ? AND pos_z = ?
        """;

        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, veinId);
            ps.setInt(i++, pos.x());
            ps.setInt(i++, pos.y());
            ps.setInt(i++, pos.z());
            return ps.executeUpdate() > 0;
        }
    }

    /** Delete everything for a single vein. */
    public static int clearForVein(long veinId) throws SQLException {
        String sql = "DELETE FROM vein_marking WHERE vein_id = ?";
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setLong(1, veinId);
            return ps.executeUpdate();
        }
    }

    /** Delete everything (all veins). */
    public static int clearAll() throws SQLException {
        try (var ps = DatabaseManager.connection().prepareStatement("DELETE FROM vein_marking")) {
            return ps.executeUpdate();
        }
    }

    public static int countForVein(long veinId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vein_marking WHERE vein_id = ?";
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setLong(1, veinId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static int countAll() throws SQLException {
        try (var ps = DatabaseManager.connection().prepareStatement("SELECT COUNT(*) FROM vein_marking");
             var rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public static void setRangeForVein(long veinId, Vector3i range) throws SQLException {
        String sql = """
            UPDATE vein_marking
            SET range_x = ?, range_y = ?, range_z = ?
            WHERE vein_id = ?
        """;

        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, range.x);
            ps.setInt(i++, range.y);
            ps.setInt(i++, range.z);
            ps.setLong(i++, veinId);
            ps.executeUpdate();
        }
    }
}