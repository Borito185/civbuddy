package com.civbuddy.veins.data.markings;

import org.joml.Vector3i;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public record VeinMarkingRow(Long veinId, Vector3i pos, Vector3i range) {
    public static VeinMarkingRow of(ResultSet rs) throws SQLException {
        long v = rs.getLong("vein_id");
        Long veinId = rs.wasNull() ? null : v;

        return new VeinMarkingRow(
                veinId,
                new Vector3i(
                        rs.getInt("pos_x"),
                        rs.getInt("pos_y"),
                        rs.getInt("pos_z")
                ),
                new Vector3i(
                        rs.getInt("range_x"),
                        rs.getInt("range_y"),
                        rs.getInt("range_z")
                )
        );
    }

    /** Binds vein_id + pos + range (for INSERT / UPSERT) */
    public int bind(PreparedStatement ps, int i) throws SQLException {
        if (veinId == null) ps.setNull(i++, java.sql.Types.INTEGER);
        else ps.setLong(i++, veinId);

        return bindPosRange(ps, i);
    }

    /** Binds only pos + range (useful for UPDATE SET … WHERE …) */
    public int bindPosRange(PreparedStatement ps, int i) throws SQLException {
        ps.setInt(i++, pos.x);
        ps.setInt(i++, pos.y);
        ps.setInt(i++, pos.z);
        ps.setInt(i++, range.x);
        ps.setInt(i++, range.y);
        ps.setInt(i++, range.z);
        return i;
    }
}
