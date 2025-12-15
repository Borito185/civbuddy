package com.civbuddy.storage.sql;

public interface Migration {
    /** Globally increasing version (or use sortable ids like 2025121401). */
    int version();
    String name();
    String sql();
}
