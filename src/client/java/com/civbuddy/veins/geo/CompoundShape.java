package com.civbuddy.veins.geo;

import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

public class CompoundShape implements VoxelShape {
    private static final float EPS = 1e-4f;
    private final HashSet<VoxelShape> shapes = new HashSet<>();
    private final HashSet<Face> faces = new HashSet<>();

    public void add(VoxelShape shape) {
        List<VoxelShape> overlapping = shapes.stream().filter(s -> s.overlaps(shape, EPS)).toList();
        shapes.add(shape);
        cullFaces(faces, shape);

        HashSet<Face> newFaces = new HashSet<>(shape.getFaces());
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

    public boolean remove(VoxelShape shape) {
        if (!shapes.remove(shape)) return false;

        // remove his faces
        faces.removeIf(f -> shape.contains(f.center(), EPS));

        // find faces in his area belonging to neighbours
        List<VoxelShape> overlapping = shapes.stream().filter(s -> s.overlaps(shape, EPS)).toList();
        HashSet<Face> temp = new HashSet<>();
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

    public Collection<Face> getFaces() {
        return faces;
    }

    @Override
    public boolean contains(Vector3f p, float tolerance) {
        return shapes.stream().anyMatch(s -> s.contains(p, tolerance));
    }

    @Override
    public boolean contains(Vector3i pos) {
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
        return shapes.stream().filter(s -> s.overlaps(shape, EPS)).toList();
    }

    private void regenerate() {
        faces.clear();
        for (VoxelShape shape : shapes) {
            HashSet<Face> nFaces = new HashSet<>(shape.getFaces());
            List<VoxelShape> neighbours = getNeighbours(shape);

            for (VoxelShape n : neighbours) {
                cullFaces(nFaces, n);
            }

            faces.addAll(nFaces);
        }
    }

    private static void cullFaces(HashSet<Face> set, VoxelShape shape) {
        final Vector3f rc = shape.getCenter();

        for (Iterator<Face> it = set.iterator(); it.hasNext();) {
            Face f = it.next();
            Vector3f p = f.center();

            if (!shape.contains(p, EPS)) continue;

            if (shape.contains(p, -EPS)) {
                it.remove();
                continue;
            }

            Vector3f n = f.normal();
            Vector3f toCenter = new Vector3f(rc.x - p.x, rc.y - p.y, rc.z - p.z);
            if (n.dot(toCenter) < 0f)
                it.remove();
        }
    }
}
