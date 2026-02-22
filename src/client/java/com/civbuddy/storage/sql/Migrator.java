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
                    for (String stmt : splitStatements(m.sql())) {
                        try (var st = c.createStatement()) {
                            st.execute(stmt);
                        }
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

    /** Split a multi-statement SQL string on ';' boundaries, skipping blanks. */
    private static List<String> splitStatements(String sql) {
        List<String> stmts = new ArrayList<>();
        for (String part : sql.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                stmts.add(trimmed);
            }
        }
        return stmts;
    }
}
