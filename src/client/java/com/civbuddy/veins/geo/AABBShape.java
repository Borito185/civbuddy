package com.civbuddy.veins.geo;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.*;
import org.joml.Math;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public record AABBShape(Vector3i center, Vector3i radius, Vector4f color, boolean hasGrid) implements VoxelShape {
    public boolean overlaps(AABBShape o) {
        // calc distance
        int dx = Math.abs(center.x() - o.center.x());
        int dy = Math.abs(center.y() - o.center.y());
        int dz = Math.abs(center.z() - o.center.z());

        // add ranges
        int rx = radius.x() + o.radius.x();
        int ry = radius.y() + o.radius.y();
        int rz = radius.z() + o.radius.z();

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
        Vector3i c = center;
        Vector3i r = radius;
        Vector4f col = color;

        int minX = c.x - r.x, maxX = c.x + r.x;
        int minY = c.y - r.y, maxY = c.y + r.y;
        int minZ = c.z - r.z, maxZ = c.z + r.z;

        for (int x = minX; x <= maxX; x++)
        for (int y = minY; y <= maxY; y++)
        for (int z = minZ; z <= maxZ; z++) {

            if (x - 1 < minX) set.add(Face.of(x, y, z, x, y+1, z, x, y+1, z+1, x, y, z+1, col, hasGrid));
            if (x + 1 > maxX) set.add(Face.of(x+1, y, z+1, x+1, y+1, z+1, x+1, y+1, z, x+1, y, z, col, hasGrid));
            if (y - 1 < minY) set.add(Face.of(x, y, z+1, x+1, y, z+1, x+1, y, z, x, y, z, col, hasGrid));
            if (y + 1 > maxY) set.add(Face.of(x, y+1, z, x+1, y+1, z, x+1, y+1, z+1, x, y+1, z+1, col, hasGrid));
            if (z - 1 < minZ) set.add(Face.of(x+1, y, z,  x+1, y+1, z,  x, y+1, z,  x, y, z, col, hasGrid));
            if (z + 1 > maxZ) set.add(Face.of(x, y, z+1,  x, y+1, z+1,  x+1, y+1, z+1,  x+1, y, z+1, col, hasGrid));
        }
    }
}
