package com.civbuddy.commands.data;

import com.civbuddy.storage.sql.Migration;

public final class CommandMigrations {
    private CommandMigrations() {}

    public static Migration[] migrations() {
        return new Migration[] { categoryTable(), entryTable(), entryIndexCategoryId() };
    }

    private static Migration categoryTable() {
        return new Migration() {
            @Override public int version() { return 2025121601; }
            @Override public String name() { return "command_category"; }
            @Override public String sql() { return """
                CREATE TABLE IF NOT EXISTS command_category (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    name       TEXT    NOT NULL,
                    color      INTEGER NOT NULL DEFAULT 16777215,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    is_protected INTEGER NOT NULL DEFAULT 0
                );
            """; }
        };
    }

    private static Migration entryTable() {
        return new Migration() {
            @Override public int version() { return 2025121602; }
            @Override public String name() { return "command_entry"; }
            @Override public String sql() { return """
                CREATE TABLE IF NOT EXISTS command_entry (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_id INTEGER NOT NULL,
                    command     TEXT    NOT NULL,
                    sort_order  INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (category_id) REFERENCES command_category(id) ON DELETE CASCADE
                );
            """; }
        };
    }

    private static Migration entryIndexCategoryId() {
        return new Migration() {
            @Override public int version() { return 2026022201; }
            @Override public String name() { return "idx_entry_category_id"; }
            @Override public String sql() { return """
                CREATE INDEX IF NOT EXISTS idx_entry_category_id ON command_entry(category_id);
            """; }
        };
    }
}
