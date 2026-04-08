package com.civbuddy.veins.geo.util;

import com.civbuddy.veins.geo.primitives.Edge;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.List;

public final class GridAlignedEdgeOptimizer {
    private GridAlignedEdgeOptimizer() {}
    record Key(int axis, int c1, int c2) {}
    public static List<Edge> optimize(Collection<Edge> in) {
        // Normalize edges and bucket by axis + plane
        var buckets = new java.util.HashMap<Key, java.util.List<int[]>>();

        for (Edge e : in) {
            var a = e.a();
            var b = e.b();

            int dx = Integer.compare(b.x(), a.x());
            int dy = Integer.compare(b.y(), a.y());

            int axis;
            int c1, c2;
            int s, t;

            if (dx != 0) { // X edge
                axis = 0; c1 = a.y(); c2 = a.z();
                s = Math.min(a.x(), b.x());
                t = Math.max(a.x(), b.x());
            } else if (dy != 0) { // Y edge
                axis = 1; c1 = a.x(); c2 = a.z();
                s = Math.min(a.y(), b.y());
                t = Math.max(a.y(), b.y());
            } else { // Z edge
                axis = 2; c1 = a.x(); c2 = a.y();
                s = Math.min(a.z(), b.z());
                t = Math.max(a.z(), b.z());
            }

            buckets.computeIfAbsent(new Key(axis, c1, c2), ignore -> new java.util.ArrayList<>())
                    .add(new int[]{s, t});
        }

        var out = new java.util.ArrayList<Edge>();

        for (var entry : buckets.entrySet()) {
            var k = entry.getKey();
            var segs = entry.getValue();

            segs.sort(java.util.Comparator.comparingInt(a -> a[0]));

            int s = segs.get(0)[0];
            int t = segs.get(0)[1];

            for (int i = 1; i < segs.size(); i++) {
                var cur = segs.get(i);
                if (cur[0] <= t) {
                    t = Math.max(t, cur[1]);
                } else {
                    out.add(buildEdge(k, s, t));
                    s = cur[0];
                    t = cur[1];
                }
            }
            out.add(buildEdge(k, s, t));
        }

        return out;
    }

    private static Edge buildEdge(Key k, int s, int t) {
        return switch (k.axis()) {
            case 0 -> new Edge(
                    new Vector3i(s, k.c1(), k.c2()),
                    new Vector3i(t, k.c1(), k.c2())
            );
            case 1 -> new Edge(
                    new Vector3i(k.c1(), s, k.c2()),
                    new Vector3i(k.c1(), t, k.c2())
            );
            default -> new Edge(
                    new Vector3i(k.c1(), k.c2(), s),
                    new Vector3i(k.c1(), k.c2(), t)
            );
        };
    }
}
