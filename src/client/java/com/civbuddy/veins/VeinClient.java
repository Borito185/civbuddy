package com.civbuddy.veins;

import java.sql.SQLException;
import java.util.Collection;
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
import com.civbuddy.veins.geo.shapes.AABBShape;
import com.civbuddy.veins.geo.shapes.CompoundShape;
import com.civbuddy.veins.geo.shapes.VoxelShape;

public class VeinClient {
    private static VeinClient instance;
    private final ShapeRenderer borderRenderer = new ShapeRenderer();
    private final ShapeRenderer markingRenderer = new ShapeRenderer();

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
    public Collection<VoxelShape> getCurrentMarkings() {
        return markingRenderer.getInnerShapes();
    }

    /* ===================== INTERNAL ===================== */
    private void redraw() throws SQLException {
        VeinConfig config = config();
        borderRenderer.setStyle(config.borderWallColor, config.borderHasGrid);
        markingRenderer.setStyle(config.markingWallColor, config.markingHasGrid);

        if (!config.doRender) {
            borderRenderer.setInnerShapes(Set.of());
            markingRenderer.setInnerShapes(Set.of());
            return;
        }

        List<VeinMarkingRow> rows = VeinMarkingDao.findAllForVein(getActiveVeinId());
        Set<VoxelShape> bordersShapes = rows
                .stream()
                .map(r -> AABBShape.of(r.pos(), r.range()))
                .collect(Collectors.toSet());
        Set<VoxelShape> markingsShapes = rows
                .stream()
                .map(r -> AABBShape.of(r.pos(), new Vector3i(0)))
                .collect(Collectors.toSet());

        borderRenderer.setInnerShapes(bordersShapes);
        markingRenderer.setInnerShapes(markingsShapes);
    }
}
