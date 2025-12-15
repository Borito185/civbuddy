package com.civbuddy.veins.geo.util;

import com.civbuddy.veins.geo.primitives.Face;
import com.civbuddy.veins.geo.primitives.UnitFace;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.*;

public final class GridAlignedFaceOptimizer {
    private GridAlignedFaceOptimizer() {}

    public static List<Face> optimize(Collection<UnitFace> in) {
        if (in == null || in.isEmpty()) return List.of();

        // Dedup + normalize into unit-cells on a plane
        HashSet<Cell> cells = new HashSet<>(in.size() * 2);
        for (UnitFace f : in) {
            Cell c = Cell.fromUnitFace(f);
            if (c != null) cells.add(c);
        }

        // Group by plane
        HashMap<Plane, ArrayList<Cell>> planes = new HashMap<>();
        for (Cell c : cells) planes.computeIfAbsent(new Plane(c.axis, c.planeC), a -> new ArrayList<>()).add(c);

        ArrayList<Face> out = new ArrayList<>();

        // Merge per plane into maximal rectangles
        for (var e : planes.entrySet()) {
            Plane p = e.getKey();
            ArrayList<Cell> list = e.getValue();

            // Build rows: v -> sorted u's
            HashMap<Integer, IntList> rowU = new HashMap<>();
            for (Cell c : list) rowU.computeIfAbsent(c.v, a -> new IntList()).add(c.u);

            for (IntList ul : rowU.values()) ul.sortUnique();

            // Turn each row into horizontal segments [u0,u1)
            HashMap<Integer, ArrayList<Seg>> segsByV = new HashMap<>();
            for (var rv : rowU.entrySet()) {
                int v = rv.getKey();
                int[] us = rv.getValue().a;
                int n = rv.getValue().n;

                ArrayList<Seg> segs = new ArrayList<>();
                for (int i = 0; i < n; ) {
                    int u0 = us[i], u1 = u0 + 1;
                    i++;
                    while (i < n && us[i] == u1) { u1++; i++; }
                    segs.add(new Seg(u0, u1));
                }
                segsByV.put(v, segs);
            }

            // Merge vertical: stack identical segments across consecutive v
            int[] vs = segsByV.keySet().stream().mapToInt(x -> x).sorted().toArray();

            HashMap<Seg, Active> active = new HashMap<>();
            int prevV = Integer.MIN_VALUE;

            for (int v : vs) {
                boolean contiguousRow = (prevV != Integer.MIN_VALUE && v == prevV + 1);

                // If there is a gap, flush everything
                if (!contiguousRow) {
                    flushAll(out, active, p);
                    active.clear();
                }

                HashSet<Seg> seenThisRow = new HashSet<>();
                for (Seg s : segsByV.get(v)) {
                    seenThisRow.add(s);
                    Active a = active.get(s);
                    if (a == null) active.put(s, new Active(v, v + 1)); // [v0, v1)
                    else a.v1 = v + 1;
                }

                // Any active segment not present in this row must be flushed
                if (contiguousRow) {
                    Iterator<Map.Entry<Seg, Active>> it = active.entrySet().iterator();
                    while (it.hasNext()) {
                        var ent = it.next();
                        if (!seenThisRow.contains(ent.getKey())) {
                            emit(out, p, ent.getKey(), ent.getValue());
                            it.remove();
                        }
                    }
                }

                prevV = v;
            }

            flushAll(out, active, p);
        }

        return out;
    }

    // ---------- emit helpers ----------

    private static void flushAll(List<Face> out, HashMap<Seg, Active> active, Plane p) {
        for (var ent : active.entrySet()) emit(out, p, ent.getKey(), ent.getValue());
    }

    private static void emit(List<Face> out, Plane p, Seg s, Active a) {
        // rectangle corners in (u,v) coordinates: [u0,u1) x [v0,v1)
        out.add(makeFace(p.axis, p.c, s.u0, s.u1, a.v0, a.v1));
    }

    private static Face makeFace(Axis axis, int c, int u0, int u1, int v0, int v1) {
        Vector3i A, B, C, D;
        switch (axis) {
            case X -> { // plane x=c, u=y, v=z
                A = new Vector3i(c, u0, v0);
                B = new Vector3i(c, u1, v0);
                C = new Vector3i(c, u1, v1);
                D = new Vector3i(c, u0, v1);
            }
            case Y -> { // plane y=c, u=x, v=z
                A = new Vector3i(u0, c, v0);
                B = new Vector3i(u1, c, v0);
                C = new Vector3i(u1, c, v1);
                D = new Vector3i(u0, c, v1);
            }
            case Z -> { // plane z=c, u=x, v=y
                A = new Vector3i(u0, v0, c);
                B = new Vector3i(u1, v0, c);
                C = new Vector3i(u1, v1, c);
                D = new Vector3i(u0, v1, c);
            }
            default -> throw new IllegalStateException();
        }
        return Face.of(A, B, C, D);
    }

    // ---------- data model ----------

    private enum Axis { X, Y, Z }
    private record Plane(Axis axis, int c) {}
    private record Seg(int u0, int u1) {}       // [u0,u1)
    private static final class Active { int v0, v1; Active(int v0, int v1){this.v0=v0;this.v1=v1;} }

    /** A unit cell on a plane: (axis, planeC) plus its lower-left (u,v). */
    private record Cell(Axis axis, int planeC, int u, int v) {
        static Cell fromUnitFace(UnitFace f) {
            Vector3ic a = f.a(), b = f.b(), c = f.c(), d = f.d();

            boolean x = a.x()==b.x() && a.x()==c.x() && a.x()==d.x();
            boolean y = a.y()==b.y() && a.y()==c.y() && a.y()==d.y();
            boolean z = a.z()==b.z() && a.z()==c.z() && a.z()==d.z();

            if (x) {
                int u0 = min4(a.y(),b.y(),c.y(),d.y()), u1 = max4(a.y(),b.y(),c.y(),d.y());
                int v0 = min4(a.z(),b.z(),c.z(),d.z()), v1 = max4(a.z(),b.z(),c.z(),d.z());
                if (u1-u0 != 1 || v1-v0 != 1) return null;
                return new Cell(Axis.X, a.x(), u0, v0);
            } else if (y) {
                int u0 = min4(a.x(),b.x(),c.x(),d.x()), u1 = max4(a.x(),b.x(),c.x(),d.x());
                int v0 = min4(a.z(),b.z(),c.z(),d.z()), v1 = max4(a.z(),b.z(),c.z(),d.z());
                if (u1-u0 != 1 || v1-v0 != 1) return null;
                return new Cell(Axis.Y, a.y(), u0, v0);
            } else if (z) {
                int u0 = min4(a.x(),b.x(),c.x(),d.x()), u1 = max4(a.x(),b.x(),c.x(),d.x());
                int v0 = min4(a.y(),b.y(),c.y(),d.y()), v1 = max4(a.y(),b.y(),c.y(),d.y());
                if (u1-u0 != 1 || v1-v0 != 1) return null;
                return new Cell(Axis.Z, a.z(), u0, v0);
            }

            return null; // non-axis-aligned -> ignored by this optimizer
        }
    }

    private static int min4(int a, int b, int c, int d) { return Math.min(Math.min(a,b), Math.min(c,d)); }
    private static int max4(int a, int b, int c, int d) { return Math.max(Math.max(a,b), Math.max(c,d)); }

    // tiny int list helper
    private static final class IntList {
        int[] a = new int[8];
        int n = 0;
        void add(int v) { if (n == a.length) a = Arrays.copyOf(a, a.length * 2); a[n++] = v; }
        void sortUnique() {
            Arrays.sort(a, 0, n);
            int w = 0;
            for (int i = 0; i < n; i++) if (i == 0 || a[i] != a[i-1]) a[w++] = a[i];
            n = w;
        }
    }
}
