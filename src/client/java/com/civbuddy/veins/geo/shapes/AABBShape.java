package com.civbuddy.veins.geo.shapes;

import com.civbuddy.veins.geo.util.VoxelConsumer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.*;

public record AABBShape(Vector3ic center, Vector3ic radius) implements VoxelShape {
    @Override
    public void AddVoxels(VoxelConsumer consumer) {
        for (int x = center.x() - radius.x(); x <= center.x() + radius.x(); x++)
        for (int y = center.y() - radius.y(); y <= center.y() + radius.y(); y++)
        for (int z = center.z() - radius.z(); z <= center.z() + radius.z(); z++)
            consumer.accept(x, y, z);
    }

    public boolean intersectsCenter(Vec3 a, Vec3 b) {
        BlockHitResult raycast = Shapes
                .box(center.x(), center.y(), center.z(), center.x() + 1, center.y() + 1, center.z() + 1)
                .clip(a, b, BlockPos.ZERO);
        return raycast != null;
    }
}
