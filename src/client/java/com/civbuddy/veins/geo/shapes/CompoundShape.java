package com.civbuddy.veins.geo.shapes;

import com.civbuddy.veins.geo.primitives.UnitFace;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.*;

public class CompoundShape implements VoxelShape {
    private static final float EPS = 1e-4f;
    private final HashSet<VoxelShape> shapes = new HashSet<>();
    private final HashSet<UnitFace> faces = new HashSet<>();

    public void add(VoxelShape shape) {
        List<VoxelShape> overlapping = shapes.stream().filter(s -> s.overlaps(shape, EPS)).toList();
        shapes.add(shape);
        cullFaces(faces, shape);

        HashSet<UnitFace> newFaces = new HashSet<>(shape.getFaces());
        for (VoxelShape voxelShape : overlapping) {
            cullFaces(newFaces, voxelShape);
        }
        faces.addAll(newFaces);
    }

    public void add(Collection<VoxelShape> shapes) {
        int size = this.shapes.size();
        int addCount = shapes.size();

        if (addCount * 4 < size || addCount <= 1) {
            for (VoxelShape shape : shapes) {
                add(shape);
            }
            return;
        }

        this.shapes.addAll(shapes);
        regenerate();
    }

    public void set(Set<VoxelShape> newShapes) {
        List<VoxelShape> toRemove = shapes.stream().filter(s -> !newShapes.contains(s)).toList();
        if (toRemove.size() > shapes.size() / 3) {
            clear();
        } else {
            for (VoxelShape voxelShape : toRemove)
                remove(voxelShape);
        }

        List<VoxelShape> toAdd = newShapes.stream().filter(s -> !shapes.contains(s)).toList();
        add(toAdd);
    }

    public List<VoxelShape> getInnerShapes() {
        return shapes.stream().toList();
    }

    public boolean remove(VoxelShape shape) {
        if (!shapes.remove(shape)) return false;

        // remove his faces
        faces.removeIf(f -> shape.contains(f.center(), EPS));

        // find faces in his area belonging to neighbours
        List<VoxelShape> overlapping = shapes.stream().filter(s -> s.overlaps(shape, EPS)).toList();
        HashSet<UnitFace> temp = new HashSet<>();
        for (VoxelShape voxelShape : overlapping) {
            temp.addAll(voxelShape.getFaces());
        }
        temp.removeIf(f -> !shape.contains(f.center(), EPS));

        // let neighbours cull in that area too
        for (VoxelShape voxelShape : overlapping) {
            cullFaces(temp, voxelShape);
        }

        // add them to the faces
        faces.addAll(temp);

        return true;
    }

    public boolean removeAt(Vector3i pos) {
        boolean remove = shapes.removeIf(s -> new Vector3i(s.getCenter(), 2).equals(pos));
        regenerate();
        return remove;
    }

    public void clear() {
        shapes.clear();
        faces.clear();
    }

    public Collection<UnitFace> getFaces() {
        return faces;
    }

    @Override
    public boolean contains(Vector3fc p, float tolerance) {
        return shapes.stream().anyMatch(s -> s.contains(p, tolerance));
    }

    @Override
    public boolean contains(Vector3ic pos) {
        return shapes.stream().anyMatch(s -> s.contains(pos));
    }

    @Override
    public Vector3f getCenter() {
        return null;
    }

    @Override
    public boolean overlaps(VoxelShape shape, float tolerance) {
        throw new NotImplementedException();
    }

    private List<VoxelShape> getNeighbours(VoxelShape shape) {
        List<VoxelShape> result = new ArrayList<>(32);
        for (VoxelShape s : shapes) {
            if (s.overlaps(shape, EPS))
                result.add(s);
        }
        return result;
    }

    private void regenerate() {
        faces.clear();
        for (VoxelShape shape : shapes) {
            List<VoxelShape> neighbours = getNeighbours(shape);
            Collection<UnitFace> newFaces = shape.getFaces();

            for (UnitFace f : newFaces) {
                boolean keep = true;
                for (VoxelShape neighbour : neighbours) {
                    keep = !shouldCull(f, neighbour);
                    if (!keep) break;
                }
                if (keep) faces.add(f);
            }
        }
    }

    private final Vector3f tmp = new Vector3f();
    private boolean shouldCull(UnitFace f, VoxelShape shape) {
        Vector3fc p = f.center();
        if (!shape.contains(p, EPS)) return false;
        if (shape.contains(p, -EPS)) {
            return true;
        }

        Vector3fc n = f.normal();
        Vector3f toCenter = shape.getCenter().sub(p, tmp);
        return (n.dot(toCenter) < 0f);
    }

    private static void cullFaces(HashSet<UnitFace> set, VoxelShape shape) {
        ArrayList<UnitFace> toRemove = new ArrayList<>(set.size()/4);
        final Vector3fc rc = shape.getCenter();

        for (UnitFace f : set) {
            Vector3fc p = f.center();

            if (!shape.contains(p, EPS)) continue;

            if (shape.contains(p, -EPS)) {
                toRemove.add(f);
                continue;
            }

            Vector3fc n = f.normal();
            Vector3f toCenter = new Vector3f(rc.x() - p.x(), rc.y() - p.y(), rc.z() - p.z());
            if (n.dot(toCenter) < 0f)
                toRemove.add(f);
        }

        for (UnitFace face : toRemove) {
            set.remove(face);
        }
    }
}
