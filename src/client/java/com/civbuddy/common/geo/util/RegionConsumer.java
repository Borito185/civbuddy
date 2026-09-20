package com.civbuddy.common.geo.util;

@FunctionalInterface
public interface RegionConsumer {
    void accept(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    );
}
