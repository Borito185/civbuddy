package com.civbuddy.veins.geo.shapes;

import com.civbuddy.veins.geo.util.VoxelConsumer;
import org.joml.Vector3ic;

public record SphereShape(Vector3ic center, Vector3ic radius) implements VoxelShape {
    @Override
    public void AddVoxels(VoxelConsumer consumer) {
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
