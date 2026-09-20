package com.civbuddy.snitch;

import com.civbuddy.CivBuddyClient;
import com.civbuddy.common.geo.shapes.AABBShape;
import com.civbuddy.common.geo.shapes.VoxelShape;
import com.civbuddy.common.render.ShapeRenderer;
import com.civbuddy.snitch.commands.CommandHandler;
import com.civbuddy.snitch.config.SnitchConfig;
import com.civbuddy.snitch.listeners.WorldDisconnectListener;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SnitchClient {
    private static final ShapeRenderer renderer = new ShapeRenderer();
    public static final Set<Vector3i> positions = new HashSet<>();
    public static boolean filterByPositions = false;

    public static final Map<String, Integer> eventColorMap = new HashMap<>();

    public static void onInitializeClient() {
        CommandHandler.initialize();
        WorldDisconnectListener.initialize();

        notifyChange();
    }

    public static void notifyChange() {
        SnitchConfig config = CivBuddyClient.config.get().snitch;

        eventColorMap.clear();
        eventColorMap.putAll(config.eventColors());

        Set<VoxelShape> shapes = new HashSet<>(positions.size());
        Vector3i zero = new Vector3i();


        for (Vector3i p : positions) {
            shapes.add(new AABBShape(p, zero));
        }

        renderer.setStyle(config.highlight);
        renderer.setInnerShapes(shapes);
    }
}
