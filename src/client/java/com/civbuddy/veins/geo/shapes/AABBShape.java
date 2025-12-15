package com.civbuddy.veins.geo.shapes;

import com.civbuddy.veins.geo.primitives.UnitFace;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.*;
import org.joml.Math;

import java.util.*;

public record AABBShape(Vector3ic center, Vector3ic radius, Vector3fc realCenter) implements VoxelShape {
    public static AABBShape of(Vector3ic center, Vector3ic radius) {
        return new AABBShape(
                center,
                radius,
                new Vector3f(center).add(0.5f, 0.5f, 0.5f)
        );
    }

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
    public boolean contains(final Vector3ic pos) {
        int dx = Math.abs(pos.x() - center.x());
        int dy = Math.abs(pos.y() - center.y());
        int dz = Math.abs(pos.z() - center.z());

        return dx <= radius.x() && dy <= radius.y() && dz <= radius.z();
    }

    @Override
    public Vector3fc getCenter() {
        return realCenter;
    }

    @Override
    public boolean overlaps(VoxelShape shape, float tolerance) {
        if (shape instanceof AABBShape aabb) return overlaps(aabb, tolerance);
        throw new NotImplementedException();
    }

    private final static Vector3f tmp = new Vector3f();
    @Override
    public boolean contains(final Vector3fc p, final float tolerance) {
        final float t = 0.5f + tolerance;

        p.sub(realCenter, tmp);
        tmp.absolute();

        return tmp.x <= radius.x() + t &&
                tmp.y <= radius.y() + t &&
                tmp.z <= radius.z() + t;
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
        if (!(o instanceof AABBShape aabbShape)) return false;
        return Objects.equals(center, aabbShape.center) && Objects.equals(radius, aabbShape.radius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }

    @Override
    public Collection<UnitFace> getFaces() {
        int x = (radius.x()+1);
        int y = (radius.y()+1);
        int z = (radius.z()+1);
        int size = 2 * (x*x + y*y + z*z);
        ArrayList<UnitFace> faces = new ArrayList<>(size);
        generateFaces(faces);
        return faces;
    }

    private void generateFaces(List<UnitFace> set) {
        Vector3ic c = center, r = radius;

        int minX = c.x() - r.x(), maxX = c.x() + r.x();
        int minY = c.y() - r.y(), maxY = c.y() + r.y();
        int minZ = c.z() - r.z(), maxZ = c.z() + r.z();

        // Planes at the box boundary (note the +1 for the "positive" side planes)
        int xL = minX,      xR = maxX + 1;
        int yB = minY,      yT = maxY + 1;
        int zF = minZ,      zB = maxZ + 1;

        // -X / +X walls: sweep y,z
        for (int y = minY; y <= maxY; y++)
        for (int z = minZ; z <= maxZ; z++) {
            // -X (left)
            set.add(UnitFace.of(
                    new Vector3i(xL, y,   z),
                    new Vector3i(xL, y+1, z),
                    new Vector3i(xL, y+1, z+1),
                    new Vector3i(xL, y,   z+1)
            ));

            // +X (right)
            set.add(UnitFace.of(
                    new Vector3i(xR, y,   z+1),
                    new Vector3i(xR, y+1, z+1),
                    new Vector3i(xR, y+1, z),
                    new Vector3i(xR, y,   z)
            ));
        }

        // -Y / +Y walls: sweep x,z
        for (int x = minX; x <= maxX; x++)
        for (int z = minZ; z <= maxZ; z++) {
            // -Y (bottom)
            set.add(UnitFace.of(
                    new Vector3i(x,   yB, z+1),
                    new Vector3i(x+1, yB, z+1),
                    new Vector3i(x+1, yB, z),
                    new Vector3i(x,   yB, z)
            ));

            // +Y (top)
            set.add(UnitFace.of(
                    new Vector3i(x,   yT, z),
                    new Vector3i(x+1, yT, z),
                    new Vector3i(x+1, yT, z+1),
                    new Vector3i(x,   yT, z+1)
            ));
        }

        // -Z / +Z walls: sweep x,y
        for (int x = minX; x <= maxX; x++)
        for (int y = minY; y <= maxY; y++) {
            // -Z (front)
            set.add(UnitFace.of(
                    new Vector3i(x+1, y,   zF),
                    new Vector3i(x+1, y+1, zF),
                    new Vector3i(x,   y+1, zF),
                    new Vector3i(x,   y,   zF)
            ));

            // +Z (back)
            set.add(UnitFace.of(
                    new Vector3i(x,   y,   zB),
                    new Vector3i(x,   y+1, zB),
                    new Vector3i(x+1, y+1, zB),
                    new Vector3i(x+1, y,   zB)
            ));
        }
    }
}
