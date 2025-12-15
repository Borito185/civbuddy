package com.civbuddy.veins.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public record VeinRow(String name, int count) {
    public static VeinRow of(ResultSet rs) throws SQLException {
        return new VeinRow(
                rs.getString("name"),
                rs.getInt("count")
        );
    }

    public int bind(PreparedStatement ps, int i) throws SQLException {
        ps.setString(i++, name);
        ps.setInt(i++, count);
        return i;
    }
}
