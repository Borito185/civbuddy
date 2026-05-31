package com.civbuddy.veins.geo.shapes;

import com.civbuddy.veins.geo.primitives.UnitFace;
import com.civbuddy.veins.geo.util.ChunkedVoxelField;
import org.joml.Vector3fc;
import org.joml.Vector3ic;

import java.util.*;

public class AlternativeCompoundShape implements VoxelShape {
    private final ChunkedVoxelField field = new ChunkedVoxelField();
    private final Set<VoxelShape> shapes = new HashSet<>();

    private List<UnitFace> faces = List.of();

    public void set(Collection<VoxelShape> newShapes) {
        HashSet<VoxelShape> ref = new HashSet<>(newShapes);

        HashSet<VoxelShape> toRemove = new HashSet<>(shapes);
        toRemove.removeAll(ref);
        for (VoxelShape voxelShape : toRemove)
            shiftShape(voxelShape, false);

        HashSet<VoxelShape> toAdd = new HashSet<>(ref);
        toAdd.removeAll(shapes);
        for (VoxelShape voxelShape : toAdd)
            shiftShape(voxelShape, true);

        faces = field.extractSurface();
    }

    private void shiftShape(VoxelShape shape, boolean isAdd) {
        if (isAdd) {
            boolean add = shapes.add(shape);
            if (!add) return;
        } else {
            boolean removed = shapes.remove(shape);
            if (!removed) return;
        }
        short shiftAmount = (short)(isAdd ? 1 : -1);

        if (shape instanceof AABBShape aabb) {
            Vector3ic center = aabb.center();
            Vector3ic radius = aabb.radius();

            for (int x = center.x() - radius.x(); x <= center.x() + radius.x(); x++)
            for (int y = center.y() - radius.y(); y <= center.y() + radius.y(); y++)
            for (int z = center.z() - radius.z(); z <= center.z() + radius.z(); z++)
                field.shift(x, y, z, shiftAmount);
        }
    }

    @Override
    public Collection<UnitFace> getFaces() {
        return faces;
    }

    @Override
    public boolean contains(Vector3fc pos, float tolerance) {
        return false;
    }

    @Override
    public boolean contains(Vector3ic pos) {
        return false;
    }

    @Override
    public Vector3fc getCenter() {
        return null;
    }

    @Override
    public boolean overlaps(VoxelShape shape, float tolerance) {
        return false;
    }

    public List<VoxelShape> getInnerShapes() {
        return shapes.stream().toList();
    }
}
