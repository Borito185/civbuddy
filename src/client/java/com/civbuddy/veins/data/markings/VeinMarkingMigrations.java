package com.civbuddy.veins.data.markings;

import com.civbuddy.storage.sql.Migration;

public final class VeinMarkingMigrations {
    private VeinMarkingMigrations() {}

    public static Migration[] migrations() {
        return new Migration[] {
                new Migration() {
                    @Override public int version() { return 2025121503; }
                    @Override public String name() { return "vein_marking_init"; }
                    @Override public String sql() { return """
                    CREATE TABLE IF NOT EXISTS vein_marking (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,

                        vein_id INTEGER NOT NULL,

                        pos_x   INTEGER NOT NULL,
                        pos_y   INTEGER NOT NULL,
                        pos_z   INTEGER NOT NULL,

                        range_x INTEGER NOT NULL,
                        range_y INTEGER NOT NULL,
                        range_z INTEGER NOT NULL,

                        FOREIGN KEY (vein_id) REFERENCES vein(id) ON DELETE CASCADE,
                        UNIQUE(vein_id, pos_x, pos_y, pos_z)
                    );

                    CREATE INDEX IF NOT EXISTS idx_vein_marking_vein
                    ON vein_marking(vein_id);

                    CREATE INDEX IF NOT EXISTS idx_vein_marking_pos_xz
                    ON vein_marking(pos_x, pos_z);
                """; }
                }
        };
    }
}
