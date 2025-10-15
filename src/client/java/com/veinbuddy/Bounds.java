package com.veinbuddy;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Math;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import java.util.Objects;

public record Bounds(Vector3i center, Vector3i range) {
    public boolean overlaps(Bounds o) {
        // calc distance
        int dx = Math.abs(center.x() - o.center.x());
        int dy = Math.abs(center.y() - o.center.y());
        int dz = Math.abs(center.z() - o.center.z());

        // add ranges
        int rx = range.x() + o.range.x();
        int ry = range.y() + o.range.y();
        int rz = range.z() + o.range.z();

        // compare
        return dx <= rx && dy <= ry && dz <= rz;
    }

    public boolean contains(Vector3ic pos) {
        int dx = Math.abs(pos.x() - center.x());
        int dy = Math.abs(pos.y() - center.y());
        int dz = Math.abs(pos.z() - center.z());

        return dx <= range.x() && dy <= range.y() && dz <= range.z();
    }

    public boolean intersects(Vec3d a, Vec3d b) {
        BlockHitResult raycast = VoxelShapes
                .cuboid(center.x(), center.y(), center.z(), center.x() + 1, center.y() + 1, center.z() + 1)
                .raycast(a, b, BlockPos.ORIGIN);
        return raycast != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bounds bounds)) return false;
        return Objects.equals(center, bounds.center) && Objects.equals(range, bounds.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, range);
    }
}
