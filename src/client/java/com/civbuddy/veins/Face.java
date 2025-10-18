package com.civbuddy.veins;

import org.joml.Vector3f;
import org.joml.Vector4f;

public record Face(Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector4f color, boolean hasEdges) {
    public static Face of(float x1, float y1, float z1,
                   float x2, float y2, float z2,
                   float x3, float y3, float z3,
                   float x4, float y4, float z4,
                   Vector4f color, boolean hasEdges) {
        return new Face(
                new Vector3f(x1, y1, z1),
                new Vector3f(x2, y2, z2),
                new Vector3f(x3, y3, z3),
                new Vector3f(x4, y4, z4),
                color, hasEdges
        );
    }

    Vector3f center() {
        return new Vector3f(
                (a.x + b.x + c.x + d.x) * 0.25f,
                (a.y + b.y + c.y + d.y) * 0.25f,
                (a.z + b.z + c.z + d.z) * 0.25f
        );
    }
    Vector3f normal() {
        Vector3f e1 = new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z);
        Vector3f e2 = new Vector3f(c.x - a.x, c.y - a.y, c.z - a.z);
        Vector3f n  = e1.cross(e2);
        return n.normalize();
    }
}