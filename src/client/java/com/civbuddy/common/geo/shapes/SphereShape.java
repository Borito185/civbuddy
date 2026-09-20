package com.civbuddy.common.geo.shapes;

import com.civbuddy.common.geo.util.RegionConsumer;
import com.civbuddy.common.geo.util.VoxelConsumer;
import org.joml.Vector3ic;

public record SphereShape(Vector3ic center, Vector3ic radius) implements VoxelShape {
    private static final int VOXEL_THRESHOLD = 4;
    @Override
    public void addVoxels(
            RegionConsumer regionConsumer,
            VoxelConsumer voxelConsumer
    ) {
        subdivide(
                -radius.x(), -radius.y(), -radius.z(),
                radius.x(),  radius.y(),  radius.z(),
                regionConsumer,
                voxelConsumer
        );
    }

    private void subdivide(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            RegionConsumer regions,
            VoxelConsumer voxels
    ) {
        if (outside(minX, minY, minZ, maxX, maxY, maxZ))
            return;

        if (inside(minX, minY, minZ, maxX, maxY, maxZ)) {
            regions.accept(
                    center.x() + minX,
                    center.y() + minY,
                    center.z() + minZ,
                    center.x() + maxX,
                    center.y() + maxY,
                    center.z() + maxZ
            );
            return;
        }

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (sizeX <= VOXEL_THRESHOLD &&
                sizeY <= VOXEL_THRESHOLD &&
                sizeZ <= VOXEL_THRESHOLD) {

            emitVoxels(
                    minX, minY, minZ,
                    maxX, maxY, maxZ,
                    voxels
            );
            return;
        }

        // Split along the largest dimension.
        if (sizeX >= sizeY && sizeX >= sizeZ) {
            int mid = (minX + maxX) >> 1;

            subdivide(minX, minY, minZ, mid, maxY, maxZ, regions, voxels);
            subdivide(mid + 1, minY, minZ, maxX, maxY, maxZ, regions, voxels);

        } else if (sizeY >= sizeZ) {
            int mid = (minY + maxY) >> 1;

            subdivide(minX, minY, minZ, maxX, mid, maxZ, regions, voxels);
            subdivide(minX, mid + 1, minZ, maxX, maxY, maxZ, regions, voxels);

        } else {
            int mid = (minZ + maxZ) >> 1;

            subdivide(minX, minY, minZ, maxX, maxY, mid, regions, voxels);
            subdivide(minX, minY, mid + 1, maxX, maxY, maxZ, regions, voxels);
        }
    }

    private void emitVoxels(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            VoxelConsumer consumer
    ) {
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    if (contains(x, y, z)) {
                        consumer.accept(
                                center.x() + x,
                                center.y() + y,
                                center.z() + z
                        );
                    }
                }
    }

    private boolean contains(int x, int y, int z) {
        return normalizedDistanceSquared(x, y, z) <= 1.0;
    }

    /**
     * Entire AABB is inside if its furthest corner is inside.
     */
    private boolean inside(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    ) {
        int x = Math.max(Math.abs(minX), Math.abs(maxX));
        int y = Math.max(Math.abs(minY), Math.abs(maxY));
        int z = Math.max(Math.abs(minZ), Math.abs(maxZ));

        return contains(x, y, z);
    }

    /**
     * Entire AABB is outside if its closest point to the origin is outside.
     */
    private boolean outside(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    ) {
        int x = closestToZero(minX, maxX);
        int y = closestToZero(minY, maxY);
        int z = closestToZero(minZ, maxZ);

        return !contains(x, y, z);
    }

    private static int closestToZero(int min, int max) {
        if (min > 0) return min;
        if (max < 0) return max;
        return 0;
    }

    private double normalizedDistanceSquared(int x, int y, int z) {
        double nx = (double) x / radius.x();
        double ny = (double) y / radius.y();
        double nz = (double) z / radius.z();

        return nx * nx + ny * ny + nz * nz;
    }

    @Override
    public void addVoxels(VoxelConsumer consumer) {
        int cx = center.x();
        int cy = center.y();
        int cz = center.z();

        int rx = radius.x();
        int ry = radius.y();
        int rz = radius.z();

        double invRx2 = 1.0 / (rx * (double) rx);
        double invRy2 = 1.0 / (ry * (double) ry);
        double invRz2 = 1.0 / (rz * (double) rz);

        for (int x = -rx; x <= rx; x++) {
            double dx2 = x * (double) x * invRx2;

            for (int y = -ry; y <= ry; y++) {
                double dy2 = y * (double) y * invRy2;

                for (int z = -rz; z <= rz; z++) {
                    double dz2 = z * (double) z * invRz2;

                    if (dx2 + dy2 + dz2 <= 1.0) {
                        consumer.accept(
                                cx + x,
                                cy + y,
                                cz + z
                        );
                    }
                }
            }
        }
    }
}
