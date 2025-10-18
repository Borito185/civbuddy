package com.civbuddy.veins;

import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Collection;

public interface VoxelShape {
    public Collection<Face> getFaces();
    public boolean contains(Vector3f pos, float tolerance);
    public boolean contains(Vector3i pos);
    public Vector3f getCenter();
}
