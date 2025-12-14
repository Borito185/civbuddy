package com.civbuddy.veins.data;

import com.civbuddy.storage.sql.Migration;

public final class VeinMigrations {
    private VeinMigrations() {}

    public static Migration[] migrations() {
        return new Migration[] {
                new Migration() {
                    @Override public int version() { return 2025121501; }
                    @Override public String name() { return "vein_init"; }
                    @Override public String sql() { return """
                    CREATE TABLE IF NOT EXISTS vein (
                        id    INTEGER PRIMARY KEY AUTOINCREMENT,
                        name  TEXT NOT NULL UNIQUE,
                        count INTEGER NOT NULL
                    );

                    CREATE INDEX IF NOT EXISTS idx_vein_count
                    ON vein(count DESC);
                """; }
                }
        };
    }
}
