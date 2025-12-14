package com.civbuddy.veins.data;

import com.civbuddy.storage.sql.KeyValueDao;

import java.sql.SQLException;
import java.util.Optional;

public final class VeinKVStore {
    private VeinKVStore() {}

    public final static String VEIN_NAME_KEY = "veins.active_vein_name";
    public final static String DO_COUNT_DIA_KEY = "veins.do_count_dia";

    public static String getActiveVeinName() throws SQLException {
        return KeyValueDao.get(VEIN_NAME_KEY).orElse("default");
    }

    public static void setActiveVeinName(String veinId) throws SQLException {
        KeyValueDao.put(VEIN_NAME_KEY, veinId);
    }

    public static boolean getDoCountDia() throws SQLException {
        return KeyValueDao
                .get(DO_COUNT_DIA_KEY)
                .map(VeinKVStore::parseBool)
                .orElse(false); // default
    }

    public static void setDoCountDia(boolean value) throws SQLException {
        // store as "1"/"0" (SQLite-friendly, unambiguous)
        KeyValueDao.put(DO_COUNT_DIA_KEY, value ? "1" : "0");
    }

    private static boolean parseBool(String v) {
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }
}
