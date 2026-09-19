package com.civbuddy.snitch;

import com.civbuddy.common.geo.shapes.AABBShape;
import com.civbuddy.common.geo.shapes.VoxelShape;
import com.civbuddy.common.render.ShapeRenderer;
import com.civbuddy.snitch.commands.CommandHandler;
import com.civbuddy.snitch.listeners.WorldDisconnectListener;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.util.HashSet;
import java.util.Set;

public class SnitchClient {
    private static final ShapeRenderer renderer = new ShapeRenderer();
    public static final Set<Vector3i> positions = new HashSet<>();
    public static boolean filterByPositions = false;

    public static void onInitializeClient() {
        CommandHandler.initialize();
        WorldDisconnectListener.initialize();
    }

    public static void redraw() {
        Set<VoxelShape> shapes = new HashSet<>(positions.size());
        Vector3i zero = new Vector3i();


        for (Vector3i p : positions) {
            shapes.add(new AABBShape(p, zero));
        }

        renderer.setStyle(new Vector4f(0.15f, 0.45f, 0.55f, 0.5f),false, true);
        renderer.setInnerShapes(shapes);
    }
}
