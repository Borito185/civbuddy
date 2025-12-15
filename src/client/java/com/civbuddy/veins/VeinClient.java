package com.civbuddy.veins;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.storage.sql.DatabaseManager;
import com.civbuddy.veins.commands.CommandHandler;
import com.civbuddy.veins.config.VeinConfig;
import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinKVStore;
import com.civbuddy.veins.data.VeinMigrations;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingMigrations;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.civbuddy.veins.listeners.RightClickListener;
import com.civbuddy.veins.listeners.MessageListener;
import com.civbuddy.veins.listeners.WorldEventListener;
import com.civbuddy.veins.render.ShapeRenderer;
import org.joml.Vector3i;
import com.civbuddy.veins.geo.AABBShape;
import com.civbuddy.veins.geo.CompoundShape;
import com.civbuddy.veins.geo.VoxelShape;

public class VeinClient {
    private static VeinClient instance;
    private final SimpleRenderer staticRenderer = new SimpleRenderer();
    private final CompoundShape borders = new CompoundShape();
    private final CompoundShape markings = new CompoundShape();
    private final ShapeRenderer borderRenderer = new ShapeRenderer();

    private VeinClient() {}

    /* ===================== PUBLIC API ===================== */

    public static VeinClient getInstance() {
        return instance;
    }

    /**
     * Shortcut to the veins section of the global config
     * @return Config for veins
     */
    public static VeinConfig config() {
        return CivBuddyClient.config.get().veins;
    }

    public static long getActiveVeinId() throws SQLException {
        String activeVeinName = VeinKVStore.getActiveVeinName();
        return VeinDao.getOrCreateId(activeVeinName);
    }

    public static void onInitializeClient() {
        instance = new VeinClient();

        // --- Register Commands ---
        CommandHandler.initialize();

        // --- Init SQL database ---
        DatabaseManager.register(VeinMigrations.migrations());
        DatabaseManager.register(VeinMarkingMigrations.migrations());

        // --- Init Listeners ---
        RightClickListener.initialize();
        MessageListener.initialize();
        WorldEventListener.initialize();
    }

    public static void notifyChange() {
        try {
            getInstance().redraw();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Just didn't feel like it was worth going through DAO for this
     */
    public List<VoxelShape> getCurrentMarkings() {
        return markings.getInnerShapes();
    }

    /* ===================== INTERNAL ===================== */
    private void redraw() throws SQLException {
        VeinConfig config = config();

        if (!config.doRender) {
            borders.clear();
            markings.clear();
            draw();
            return;
        }

        List<VeinMarkingRow> rows = VeinMarkingDao.findAllForVein(getActiveVeinId());
        Set<VoxelShape> bordersShapes = rows
                .stream()
                .map(r -> new AABBShape(r.pos(), r.range()))
                .collect(Collectors.toSet());
        Set<VoxelShape> markingsShapes = rows
                .stream()
                .map(r -> new AABBShape(r.pos(), new Vector3i(0)))
                .collect(Collectors.toSet());

        borderRenderer.getShape().set(bordersShapes);
//        borders.set(bordersShapes);
//        markings.set(markingsShapes);
//
//        draw();
    }

    private void draw() {
        List<VoxelShape> shapes = List.of(borders, markings);

        if (!config().doRender)
            shapes = List.of();
        staticRenderer.draw(shapes);
    }
}
