package com.civbuddy.veins.data;

import com.civbuddy.storage.sql.KeyValueDao;

import java.sql.SQLException;
import java.util.Optional;

public final class VeinKVStore {
    private VeinKVStore() {}

    public final static String VEIN_NAME_KEY = "veins.active_vein_name";

    public static String getActiveVeinName() throws SQLException {
        return KeyValueDao.get(VEIN_NAME_KEY).orElse("default");
    }

    public static void setActiveVeinName(String veinId) throws SQLException {
        KeyValueDao.put(VEIN_NAME_KEY, veinId);
    }

    private static boolean parseBool(String v) {
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }
}
