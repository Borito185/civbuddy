package com.civbuddy.veins.geo;

import org.joml.Vector3f;
import org.joml.Vector3i;

public record Face(Vector3i a, Vector3i b, Vector3i c, Vector3i d) {
    public Vector3f center() {
        return new Vector3f(
                (a.x + b.x + c.x + d.x) * 0.25f,
                (a.y + b.y + c.y + d.y) * 0.25f,
                (a.z + b.z + c.z + d.z) * 0.25f
        );
    }
    public Vector3f normal() {
        Vector3f e1 = new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z);
        Vector3f e2 = new Vector3f(c.x - a.x, c.y - a.y, c.z - a.z);
        Vector3f n  = e1.cross(e2);
        return n.normalize();
    }
}