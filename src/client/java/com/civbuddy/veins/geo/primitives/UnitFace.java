package com.civbuddy.veins.geo.primitives;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Objects;

public record UnitFace(Vector3ic a, Vector3ic b, Vector3ic c, Vector3ic d, Vector3fc center, Vector3fc normal) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnitFace unitFace)) return false;
        return Objects.equals(center, unitFace.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center);
    }

    public static UnitFace of(Vector3i a, Vector3i b, Vector3i c, Vector3i d) {
        return new UnitFace(
                a, b, c, d,
                compute_center(a, b, c, d),
                compute_normal(a, b, c, d)
        );
    }
    private static Vector3f compute_center(Vector3i a, Vector3i b, Vector3i c, Vector3i d) {
        return new Vector3f(
                (a.x + b.x + c.x + d.x) * 0.25f,
                (a.y + b.y + c.y + d.y) * 0.25f,
                (a.z + b.z + c.z + d.z) * 0.25f
        );
    }
    private static Vector3f compute_normal(Vector3i a, Vector3i b, Vector3i c, Vector3i d) {
        float e1x = b.x - a.x, e1y = b.y - a.y, e1z = b.z - a.z;
        float e2x = c.x - a.x, e2y = c.y - a.y, e2z = c.z - a.z;

        float nx = e1y * e2z - e1z * e2y;
        float ny = e1z * e2x - e1x * e2z;
        float nz = e1x * e2y - e1y * e2x;

        float invLen = (float) (1.0 / Math.sqrt(nx*nx + ny*ny + nz*nz));
        return new Vector3f(nx * invLen, ny * invLen, nz * invLen);
    }
}