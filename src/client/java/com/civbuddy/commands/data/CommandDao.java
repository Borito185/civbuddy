package com.civbuddy.commands.data;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.storage.sql.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CommandDao {
    private static CommandDao instance;
    private static final String PREBUILT_FILE = "/assets/civbuddy/config/prebuilt_commands.json";
    private static final int HISTORY_MAX = 20;

    private CommandDao() {}

    public static CommandDao getInstance() {
        if (instance == null) { instance = new CommandDao(); }
        return instance;
    }

    // ======================== Init ========================

    /** Seed DB if empty and ensure History category exists. Called on world join. */
    public void initialize() {
        try {
            if (queryInt("SELECT COUNT(*) FROM command_category") == 0) {
                seedFromPrebuilt();
            }
            if (getHistoryCategoryId() < 0) {
                int order = queryInt("SELECT COALESCE(MAX(sort_order), -1) FROM command_category") + 1;
                dbInsertCategory("History", 0xAAAAAA, order, true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void seedFromPrebuilt() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (InputStream in = CivBuddyClient.class.getResourceAsStream(PREBUILT_FILE)) {
            if (in == null) return;
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<CommandCategory>>(){}.getType();
            List<CommandCategory> loaded = gson.fromJson(reader, listType);
            if (loaded != null) insertCategoriesToDb(loaded);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    /** Inserts a list of categories + entries into the database. Used by migration and seeding. */
    public void insertCategoriesToDb(List<CommandCategory> cats) throws SQLException {
        int catOrder = queryInt("SELECT COALESCE(MAX(sort_order), -1) FROM command_category") + 1;
        for (CommandCategory cat : cats) {
            long catId = dbInsertCategory(cat.getName(), cat.getColor(), catOrder++, "History".equals(cat.getName()));
            int entOrder = 0;
            for (CommandEntry entry : cat.getEntries()) {
                dbInsertEntry(catId, entry.getCommand(), entOrder++);
            }
        }
    }

    // ======================== Reads ========================

    /** Fetch all categories with their entries from DB. */
    public List<CommandCategory> getCategories() {
        try {
            List<CommandCategory> cats = dbGetAllCategories();
            for (CommandCategory cat : cats) {
                cat.getEntries().addAll(dbGetEntriesForCategory(cat.getId()));
            }
            return cats;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** Fetch entries for a single category from DB. */
    public List<CommandEntry> getEntries(long categoryId) {
        try { return dbGetEntriesForCategory(categoryId); }
        catch (SQLException e) { e.printStackTrace(); return new ArrayList<>(); }
    }

    /** Check if DB has any categories. */
    public int categoryCount() throws SQLException {
        return queryInt("SELECT COUNT(*) FROM command_category");
    }

    // ======================== Category CRUD (create, read, update, delete) ========================

    public long addCategory(String name, int color) {
        try {
            int order = queryInt("SELECT COALESCE(MAX(sort_order), -1) FROM command_category") + 1;
            return dbInsertCategory(name, color, order, false);
        } catch (SQLException e) { e.printStackTrace(); return -1; }
    }

    public void removeCategory(long id) {
        exec("DELETE FROM command_category WHERE id = ?", id);
    }

    public void updateCategory(long id, String name, int color) {
        exec("UPDATE command_category SET name = ?, color = ? WHERE id = ?", name, color, id);
    }

    public void reorderCategories(List<Long> ids) {
        reorder("command_category", ids);
    }

    // ======================== Entry CRUD (create, read, update, delete) ========================

    public long addEntry(long categoryId, String command) {
        try {
            int order = queryInt("SELECT COALESCE(MAX(sort_order), -1) FROM command_entry WHERE category_id = ?", categoryId) + 1;
            return dbInsertEntry(categoryId, command, order);
        } catch (SQLException e) { e.printStackTrace(); return -1; }
    }

    public void removeEntry(long id) {
        exec("DELETE FROM command_entry WHERE id = ?", id);
    }

    public void updateEntry(long id, String command) {
        exec("UPDATE command_entry SET command = ? WHERE id = ?", command, id);
    }

    public void reorderEntries(List<Long> ids) {
        reorder("command_entry", ids);
    }

    public void copyEntryToCategory(long entryId, long targetCategoryId) {
        exec("""
            INSERT INTO command_entry(category_id, command, sort_order)
            SELECT ?, command, (SELECT COALESCE(MAX(sort_order), -1) + 1 FROM command_entry WHERE category_id = ?)
            FROM command_entry WHERE id = ?
        """, targetCategoryId, targetCategoryId, entryId);
    }

    // ======================== History ========================

    public void addToHistory(String command) {
        try {
            long histId = getHistoryCategoryId();
            if (histId < 0) return;
            exec("DELETE FROM command_entry WHERE category_id = ? AND command = ?", histId, command);
            exec("UPDATE command_entry SET sort_order = sort_order + 1 WHERE category_id = ?", histId);
            dbInsertEntry(histId, command, 0);
            exec("""
                DELETE FROM command_entry WHERE id IN (
                    SELECT id FROM command_entry WHERE category_id = ?
                    ORDER BY sort_order DESC
                    LIMIT MAX(0, (SELECT COUNT(*) FROM command_entry WHERE category_id = ?) - ?)
                )
            """, histId, histId, HISTORY_MAX);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Check if a category is the protected History category. */
    public boolean isProtected(long categoryId) {
        try { return getHistoryCategoryId() == categoryId; }
        catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ======================== Internal helpers ========================

    /** Execute an update/delete/insert with positional params. Swallows exceptions. */
    private void exec(String sql, Object... params) {
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Long v)    ps.setLong(i + 1, v);
                else if (params[i] instanceof Integer v) ps.setInt(i + 1, v);
                else if (params[i] instanceof String v)  ps.setString(i + 1, v);
                else ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Run a query that returns a single int. */
    private int queryInt(String sql, Object... params) throws SQLException {
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Long v)    ps.setLong(i + 1, v);
                else if (params[i] instanceof Integer v) ps.setInt(i + 1, v);
                else ps.setObject(i + 1, params[i]);
            }
            try (var rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Update sort_order for a list of IDs in the given table. */
    private void reorder(String table, List<Long> ids) {
        try {
            String sql = "UPDATE " + table + " SET sort_order = ? WHERE id = ?";
            for (int i = 0; i < ids.size(); i++) {
                try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
                    ps.setInt(1, i);
                    ps.setLong(2, ids.get(i));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private long getHistoryCategoryId() throws SQLException {
        String sql = "SELECT id FROM command_category WHERE is_protected = 1 AND name = 'History' LIMIT 1";
        try (var ps = DatabaseManager.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    private List<CommandCategory> dbGetAllCategories() throws SQLException {
        String sql = "SELECT id, name, color, sort_order FROM command_category ORDER BY sort_order";
        try (var ps = DatabaseManager.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            List<CommandCategory> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new CommandCategory(rs.getLong("id"), rs.getString("name"),
                        rs.getInt("color"), rs.getInt("sort_order")));
            }
            return out;
        }
    }

    private List<CommandEntry> dbGetEntriesForCategory(long categoryId) throws SQLException {
        String sql = "SELECT id, command, sort_order FROM command_entry WHERE category_id = ? ORDER BY sort_order";
        try (var ps = DatabaseManager.connection().prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (var rs = ps.executeQuery()) {
                List<CommandEntry> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new CommandEntry(rs.getLong("id"), rs.getString("command"), rs.getInt("sort_order")));
                }
                return out;
            }
        }
    }

    private long dbInsertCategory(String name, int color, int sortOrder, boolean isProtected) throws SQLException {
        String sql = "INSERT INTO command_category(name, color, sort_order, is_protected) VALUES (?, ?, ?, ?)";
        try (var ps = DatabaseManager.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setInt(2, color); ps.setInt(3, sortOrder); ps.setInt(4, isProtected ? 1 : 0);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    private long dbInsertEntry(long categoryId, String command, int sortOrder) throws SQLException {
        String sql = "INSERT INTO command_entry(category_id, command, sort_order) VALUES (?, ?, ?)";
        try (var ps = DatabaseManager.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, categoryId); ps.setString(2, command); ps.setInt(3, sortOrder);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }
}
