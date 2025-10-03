package com.veinbuddy;

import org.joml.Math;
import org.joml.Vector3i;

public record Bounds(Vector3i center, Vector3i range) {
    public boolean overlaps(Bounds o) {
        // calc distance
        int dx = Math.abs(center.x - o.center.x);
        int dy = Math.abs(center.y - o.center.y);
        int dz = Math.abs(center.z - o.center.z);

        // add ranges
        int rx = range.x + o.range.x;
        int ry = range.y + o.range.y;
        int rz = range.z + o.range.z;

        // compare
        return dx <= rx && dy <= ry && dz <= rz;
    }

    public boolean contains(Vector3i pos) {
        int dx = Math.abs(pos.x - center.x);
        int dy = Math.abs(pos.y - center.y);
        int dz = Math.abs(pos.z - center.z);

        return dx <= range.x && dy <= range.y && dz <= range.z;
    }
}
