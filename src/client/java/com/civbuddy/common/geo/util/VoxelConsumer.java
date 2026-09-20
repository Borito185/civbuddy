package com.civbuddy.common.geo.util;

@FunctionalInterface
public interface VoxelConsumer {
    void accept(int x, int y, int z);
}

