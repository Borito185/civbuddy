package com.civbuddy.veins.geo.util;

import com.civbuddy.veins.geo.primitives.Edge;
import com.civbuddy.veins.geo.primitives.Face;
import com.civbuddy.veins.geo.primitives.UnitFace;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class Face2Edge {
    public static Collection<Edge> generateEdges(Collection<Face> faces) {
        Set<Edge> edges = new HashSet<>();

        for (Face face : faces) {

            Vector3ic a = face.a();
            Vector3ic b = face.b();
            Vector3ic d = face.d();

            int abx = Integer.signum(b.x() - a.x());
            int aby = Integer.signum(b.y() - a.y());
            int abz = Integer.signum(b.z() - a.z());

            int adx = Integer.signum(d.x() - a.x());
            int ady = Integer.signum(d.y() - a.y());
            int adz = Integer.signum(d.z() - a.z());

            int width =
                    Math.abs(b.x() - a.x()) +
                            Math.abs(b.y() - a.y()) +
                            Math.abs(b.z() - a.z());

            int height =
                    Math.abs(d.x() - a.x()) +
                            Math.abs(d.y() - a.y()) +
                            Math.abs(d.z() - a.z());

            // Lines parallel to AB
            for (int i = 0; i <= height; i++) {

                Vector3i start = new Vector3i(
                        a.x() + adx * i,
                        a.y() + ady * i,
                        a.z() + adz * i
                );

                Vector3i end = new Vector3i(
                        start.x + abx * width,
                        start.y + aby * width,
                        start.z + abz * width
                );

                edges.add(normalize(new Edge(start, end)));
            }

            // Lines parallel to AD
            for (int i = 0; i <= width; i++) {

                Vector3i start = new Vector3i(
                        a.x() + abx * i,
                        a.y() + aby * i,
                        a.z() + abz * i
                );

                Vector3i end = new Vector3i(
                        start.x + adx * height,
                        start.y + ady * height,
                        start.z + adz * height
                );

                edges.add(normalize(new Edge(start, end)));
            }
        }

        return edges;
    }

    private static Edge normalize(Edge edge) {
        Vector3ic a = edge.a();
        Vector3ic b = edge.b();

        if (
                a.x() < b.x() ||
                        (a.x() == b.x() && a.y() < b.y()) ||
                        (a.x() == b.x() && a.y() == b.y() && a.z() <= b.z())
        ) {
            return edge;
        }

        return new Edge(b, a);
    }
}
