package com.civbuddy.storage.sql;

import java.sql.*;
import java.util.*;

public final class Migrator {
    private Migrator() {}

    public static void migrate(Connection c, List<Migration> migrations) throws SQLException {
        migrations = new ArrayList<>(migrations);
        migrations.sort(Comparator.comparingInt(Migration::version));

        int current = getUserVersion(c);

        c.setAutoCommit(false);
        try {
            for (Migration m : migrations) {
                if (m.version() > current) {
                    try (var st = c.createStatement()) {
                        st.execute(m.sql());
                    }
                    setUserVersion(c, m.version());
                }
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    private static int getUserVersion(Connection c) throws SQLException {
        try (var st = c.createStatement(); var rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void setUserVersion(Connection c, int v) throws SQLException {
        try (var st = c.createStatement()) {
            st.execute("PRAGMA user_version=" + v);
        }
    }
}
