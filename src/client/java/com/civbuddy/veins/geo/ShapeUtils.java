package com.civbuddy.veins.geo;

import org.joml.Vector3f;

import java.util.Collection;
import java.util.Set;

public class ShapeUtils {
    public static void generateEdges(Set<Edge> set, Collection<Face> faces) {
        for (Face f : faces) {
            if (!f.hasEdges()) continue;

            Vector3f[] v = {
                    new Vector3f(f.a().x, f.a().y, f.a().z),
                    new Vector3f(f.b().x, f.b().y, f.b().z),
                    new Vector3f(f.c().x, f.c().y, f.c().z),
                    new Vector3f(f.d().x, f.d().y, f.d().z)
            };
            int[][] e = {{0,1},{1,2},{2,3},{3,0}};

            for (int[] pair : e) {
                Vector3f a = v[pair[0]], b = v[pair[1]];
                // enforce deterministic order
                Edge edge = compareVec(a, b) < 0 ? new Edge(a, b) : new Edge(b, a);
                set.add(edge);
            }
        }
    }

    private static int compareVec(Vector3f a, Vector3f b) {
        if (a.x != b.x) return Float.compare(a.x, b.x);
        if (a.y != b.y) return Float.compare(a.y, b.y);
        return Float.compare(a.z, b.z);
    }
}
