package com.civbuddy.veins.geo.shapes;

import com.civbuddy.veins.geo.primitives.UnitFace;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Collection;

public interface VoxelShape {
    public Collection<UnitFace> getFaces();
    public boolean contains(Vector3fc pos, float tolerance);
    public boolean contains(Vector3ic pos);
    public Vector3fc getCenter();
    public boolean overlaps(VoxelShape shape, float tolerance);
}
