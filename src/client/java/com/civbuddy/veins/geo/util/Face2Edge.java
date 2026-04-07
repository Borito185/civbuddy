package com.civbuddy.veins.geo.util;

import com.civbuddy.veins.geo.primitives.Edge;
import com.civbuddy.veins.geo.primitives.UnitFace;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class Face2Edge {
    public static Collection<Edge> generateEdges(Collection<UnitFace> faces) {
        Set<Edge> result = new HashSet<>(faces.size() * 2);
        generateEdges(result, faces);
        return result;
    }

    public static void generateEdges(Set<Edge> set, Collection<UnitFace> faces) {
        if (faces == null || faces.isEmpty()) return;

        for (UnitFace f : faces) {
            Vector3ic a = f.a(), b = f.b(), c = f.c(), d = f.d();

            // quad perimeter: a-b-c-d-a
            addUndirectedEdge(set, a, b);
            addUndirectedEdge(set, b, c);
            addUndirectedEdge(set, c, d);
            addUndirectedEdge(set, d, a);
        }
    }

    private static void addUndirectedEdge(Set<Edge> set, Vector3ic p, Vector3ic q) {
        if (p == null || q == null) return;
        if (p.equals(q)) return; // ignore degenerate

        // Canonical ordering so (a,b) == (b,a)
        if (compareVec3i(p, q) <= 0) set.add(new Edge(p, q));
        else                         set.add(new Edge(q, p));
    }

    private static int compareVec3i(Vector3ic u, Vector3ic v) {
        int cx = Integer.compare(u.x(), v.x());
        if (cx != 0) return cx;
        int cy = Integer.compare(u.y(), v.y());
        if (cy != 0) return cy;
        return Integer.compare(u.z(), v.z());
    }
}
