package com.civbuddy.veins.geo.shapes;

import com.civbuddy.veins.geo.primitives.UnitFace;
import com.civbuddy.veins.geo.util.VoxelConsumer;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Collection;
import java.util.Iterator;

public interface VoxelShape {
    public void AddVoxels(VoxelConsumer consumer);

    public static VoxelShape of(Vector3ic center, Vector3ic radius, int type) {
        return switch (type) {
            case 0 -> new AABBShape(center, radius);
            case 1 -> new SphereShape(center, radius);
            default -> throw new NotImplementedException();
        };
    }
}
