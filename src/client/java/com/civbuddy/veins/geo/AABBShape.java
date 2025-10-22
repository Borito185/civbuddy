package com.civbuddy.veins.geo;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.*;
import org.joml.Math;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public record AABBShape(Vector3i center, Vector3i radius, Vector4f color, boolean hasGrid) implements VoxelShape {
    public boolean overlaps(AABBShape o, float tolerance) {
        tolerance += 1;

        // calc distance
        float dx = Math.abs(center.x() - o.center.x());
        float dy = Math.abs(center.y() - o.center.y());
        float dz = Math.abs(center.z() - o.center.z());

        // add ranges
        float rx = radius.x() + o.radius.x() + tolerance;
        float ry = radius.y() + o.radius.y() + tolerance;
        float rz = radius.z() + o.radius.z() + tolerance;

        // compare
        return dx <= rx && dy <= ry && dz <= rz;
    }

    @Override
    public boolean contains(final Vector3i pos) {
        int dx = Math.abs(pos.x() - center.x());
        int dy = Math.abs(pos.y() - center.y());
        int dz = Math.abs(pos.z() - center.z());

        return dx <= radius.x() && dy <= radius.y() && dz <= radius.z();
    }

    @Override
    public Vector3f getCenter() {
        return new Vector3f(center.x+.5f, center.y+.5f, center.z+.5f);
    }

    @Override
    public boolean overlaps(VoxelShape shape, float tolerance) {
        if (shape instanceof AABBShape aabb) return overlaps(aabb, tolerance);
        throw new NotImplementedException();
    }

    @Override
    public boolean contains(final Vector3f p, final float tolerance) {
        Vector3f center = getCenter();

        return Math.abs(p.x - center.x) <= radius.x + .5f + tolerance &&
                Math.abs(p.y - center.y) <= radius.y + .5f + tolerance &&
                Math.abs(p.z - center.z) <= radius.z + .5f + tolerance;
    }

    public boolean intersectsCenter(Vec3d a, Vec3d b) {
        BlockHitResult raycast = VoxelShapes
                .cuboid(center.x(), center.y(), center.z(), center.x() + 1, center.y() + 1, center.z() + 1)
                .raycast(a, b, BlockPos.ORIGIN);
        return raycast != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AABBShape shape)) return false;
        return Objects.equals(center, shape.center) && Objects.equals(radius, shape.radius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }

    @Override
    public Collection<Face> getFaces() {
        HashSet<Face> faces = new HashSet<>();
        generateFaces(faces);
        return faces;
    }

    private void generateFaces(HashSet<Face> set) {
        Vector3i c = center, r = radius;
        Vector4f col = color;

        int minX = c.x - r.x, maxX = c.x + r.x;
        int minY = c.y - r.y, maxY = c.y + r.y;
        int minZ = c.z - r.z, maxZ = c.z + r.z;

        // Planes at the box boundary (note the +1 for the "positive" side planes)
        int xL = minX,      xR = maxX + 1;
        int yB = minY,      yT = maxY + 1;
        int zF = minZ,      zB = maxZ + 1;

        // -X / +X walls: sweep y,z
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++) {
                // -X (left)
                set.add(Face.of(xL, y, z,   xL, y+1, z,   xL, y+1, z+1,   xL, y, z+1,   col, hasGrid));
                // +X (right)
                set.add(Face.of(xR, y, z+1, xR, y+1, z+1, xR, y+1, z,     xR, y, z,     col, hasGrid));
            }

        // -Y / +Y walls: sweep x,z
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                // -Y (bottom)
                set.add(Face.of(x,   yB, z+1,  x+1, yB, z+1,  x+1, yB, z,    x,   yB, z,    col, hasGrid));
                // +Y (top)
                set.add(Face.of(x,   yT, z,    x+1, yT, z,    x+1, yT, z+1,  x,   yT, z+1,  col, hasGrid));
            }

        // -Z / +Z walls: sweep x,y
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++) {
                // -Z (front)
                set.add(Face.of(x+1, y, zF,   x+1, y+1, zF,   x, y+1, zF,    x, y, zF,     col, hasGrid));
                // +Z (back)
                set.add(Face.of(x,   y, zB,   x,   y+1, zB,   x+1, y+1, zB,  x+1, y, zB,   col, hasGrid));
            }
    }
}
