package com.civbuddy.common.geo.shapes;

import com.civbuddy.common.geo.util.RegionConsumer;
import com.civbuddy.common.geo.util.VoxelConsumer;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3ic;

public interface VoxelShape {
    public void addVoxels(VoxelConsumer consumer);

    public void addVoxels(RegionConsumer regionConsumer, VoxelConsumer voxelConsumer);

    public static VoxelShape of(Vector3ic center, Vector3ic radius, int type) {
        return switch (type) {
            case 0 -> new AABBShape(center, radius);
            case 1 -> new SphereShape(center, radius);
            default -> throw new NotImplementedException();
        };
    }
}
