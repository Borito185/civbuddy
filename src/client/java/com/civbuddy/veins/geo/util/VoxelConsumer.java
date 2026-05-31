package com.civbuddy.veins.geo.util;

@FunctionalInterface
public interface VoxelConsumer {
    void accept(int x, int y, int z);
}