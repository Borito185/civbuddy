package com.civbuddy.storage.sql;

public final class KeyValueMigrations {
    private KeyValueMigrations() {}

    public static Migration[] migrations() {
        return new Migration[] {
                new Migration() {
                    @Override public int version() { return 2025121401; } // pick a globally-unique increasing number
                    @Override public String name() { return "key_value_init"; }
                    @Override public String sql() { return """
                    CREATE TABLE IF NOT EXISTS key_value (
                        key   TEXT PRIMARY KEY,
                        value TEXT
                    );

                    CREATE INDEX IF NOT EXISTS idx_key_value_key
                    ON key_value(key);
                """; }
                }
        };
    }
}
