package com.civbuddy.veins;

import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

public class CompoundShape implements VoxelShape {
    private final HashSet<VoxelShape> shapes = new HashSet<>();
    private final HashSet<Face> faces = new HashSet<>();

    public void add(VoxelShape shape) {
        shapes.add(shape);
        regenerate();
    }

    public void add(Collection<VoxelShape> shapes) {
        this.shapes.addAll(shapes);
        regenerate();
    }

    public void set(Collection<VoxelShape> shapes) {
        clear();
        this.shapes.addAll(shapes);
        regenerate();
    }

    public boolean remove(VoxelShape shape) {
        boolean remove = shapes.remove(shape);
        regenerate();
        return remove;
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

    public void regenerate() {
        faces.clear();
        for (VoxelShape shape : shapes) {
            faces.addAll(shape.getFaces());
        }

        for (VoxelShape shape : shapes) {
            cullFaces(faces, shape);
        }
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

    private static void cullFaces(HashSet<Face> set, VoxelShape shape) {
        final float EPS = 1e-4f;
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

        if (shape instanceof AABBShape aabb) { cullFaces(set, aabb); return; }

        throw new NotImplementedException();
    }

    private static void cullFaces(HashSet<Face> set, AABBShape shape) {
        final float EPS = 1e-4f;
        final Vector3f rc = shape.getCenter();
        final Vector3f rad = new Vector3f(shape.radius());

        for (Iterator<Face> it = set.iterator(); it.hasNext();) {
            Face f = it.next();
            Vector3f p = f.center();

            float dx = Math.abs(p.x - rc.x) - rad.x;
            float dy = Math.abs(p.y - rc.y) - rad.y;
            float dz = Math.abs(p.z - rc.z) - rad.z;
            float maxd = Math.max(dx, Math.max(dy, dz));

            if (maxd < -EPS) { // strictly inside
                it.remove();
                continue;
            }
            if (Math.abs(maxd) <= EPS) { // on border: check facing
                Vector3f n = f.normal();
                Vector3f toCenter = new Vector3f(rc.x - p.x, rc.y - p.y, rc.z - p.z);
                if (n.dot(toCenter) > 0f)
                    it.remove();
            }
        }
    }
}
