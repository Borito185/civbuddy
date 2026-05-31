package com.civbuddy.veins.geo.util;

import com.civbuddy.veins.geo.primitives.Edge;
import com.civbuddy.veins.geo.primitives.Face;
import com.civbuddy.veins.geo.primitives.UnitFace;
import com.civbuddy.veins.geo.shapes.AABBShape;
import com.civbuddy.veins.geo.shapes.VoxelShape;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.*;

/**
 * Optimized chunked voxel field with cached chunk meshes.
 *
 * Key optimization:
 * - Only rebuild meshes for dirty chunks.
 * - Neighbor chunks are also dirtied when editing borders.
 * - extractSurface() only concatenates cached meshes.
 */
public class ChunkedVoxelField {

    public static final int SIZE = 16;
    public static final int MASK = SIZE - 1;
    public static final int VOLUME = SIZE * SIZE * SIZE;

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final Set<VoxelShape> shapes = new HashSet<>();

    // =========================================================
    // PUBLIC API
    // =========================================================
    public Collection<Chunk> set(Collection<VoxelShape> newShapes) {
        if (newShapes == null || newShapes.isEmpty()) {
            chunks.clear();
            shapes.clear();
            return List.of();
        }

        HashSet<VoxelShape> ref = new HashSet<>(newShapes);

        HashSet<VoxelShape> toRemove = new HashSet<>(shapes);
        toRemove.removeAll(ref);
        for (VoxelShape voxelShape : toRemove)
            shiftShape(voxelShape, false);

        HashSet<VoxelShape> toAdd = new HashSet<>(ref);
        toAdd.removeAll(shapes);
        for (VoxelShape voxelShape : toAdd)
            shiftShape(voxelShape, true);

        return getChunks();
    }

    public Collection<Chunk> getChunks() {
        List<Chunk> chunks = new ArrayList<>();

        for (Map.Entry<Long, Chunk> entry : this.chunks.entrySet()) {

            long key = entry.getKey();
            Chunk chunk = entry.getValue();

            if (chunk.dirty) {
                rebuildChunkMesh(
                        unpackX(key),
                        unpackY(key),
                        unpackZ(key),
                        chunk
                );
            }

            chunks.add(chunk);
        }

        return chunks;
    }

    public Collection<VoxelShape> getInnerShapes() {
        return shapes;
    }

    // =========================================================
    // SHAPE SHIFTING
    // =========================================================

    private void shiftShape(VoxelShape shape, boolean isAdd) {
        if (isAdd) {
            boolean add = shapes.add(shape);
            if (!add) return;
        } else {
            boolean removed = shapes.remove(shape);
            if (!removed) return;
        }
        short shiftAmount = (short)(isAdd ? 1 : -1);

        if (shape instanceof AABBShape aabb) {
            Vector3ic center = aabb.center();
            Vector3ic radius = aabb.radius();

            for (int x = center.x() - radius.x(); x <= center.x() + radius.x(); x++)
                for (int y = center.y() - radius.y(); y <= center.y() + radius.y(); y++)
                    for (int z = center.z() - radius.z(); z <= center.z() + radius.z(); z++)
                        shift(x, y, z, shiftAmount);
        } else {
            throw new NotImplementedException();
        }
    }

    private void shift(int x, int y, int z, short diff) {
        if (diff == 0) return;

        int cx = floorDiv16(x);
        int cy = floorDiv16(y);
        int cz = floorDiv16(z);

        long key = chunkKey(cx, cy, cz);

        Chunk chunk = chunks.computeIfAbsent(key, k ->
                new Chunk(new Vector3i(
                        (cx << 4) + 8,
                        (cy << 4) + 8,
                        (cz << 4) + 8
                ))
        );

        int localX = x & MASK;
        int localY = y & MASK;
        int localZ = z & MASK;

        int index = index(localX, localY, localZ);

        chunk.voxels[index] += diff;
        chunk.dirty = true;

        // Border edits affect neighboring chunk meshes
        if (localX == 0) markDirty(cx - 1, cy, cz);
        if (localX == 15) markDirty(cx + 1, cy, cz);

        if (localY == 0) markDirty(cx, cy - 1, cz);
        if (localY == 15) markDirty(cx, cy + 1, cz);

        if (localZ == 0) markDirty(cx, cy, cz - 1);
        if (localZ == 15) markDirty(cx, cy, cz + 1);
    }

    // =========================================================
    // CHUNK REBUILD
    // =========================================================

    private void rebuildChunkMesh(int cx, int cy, int cz, Chunk chunk) {

        chunk.faces.clear();

        int baseX = cx << 4;
        int baseY = cy << 4;
        int baseZ = cz << 4;

        short[] voxels = chunk.voxels;

        List<UnitFace> faces = new ArrayList<>();

        for (int z = 0; z < SIZE; z++)
        for (int y = 0; y < SIZE; y++)
        for (int x = 0; x < SIZE; x++) {

            int i = index(x, y, z);

            if (voxels[i] <= 0) {
                continue;
            }

            int wx = baseX + x;
            int wy = baseY + y;
            int wz = baseZ + z;

            // -X
            if (!isSolid(wx - 1, wy, wz)) {
                faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz),
                        new Vector3i(wx, wy + 1, wz),
                        new Vector3i(wx, wy + 1, wz + 1),
                        new Vector3i(wx, wy, wz + 1)
                ));
            }

            // +X
            if (!isSolid(wx + 1, wy, wz)) {
                faces.add(UnitFace.of(
                        new Vector3i(wx + 1, wy, wz),
                        new Vector3i(wx + 1, wy, wz + 1),
                        new Vector3i(wx + 1, wy + 1, wz + 1),
                        new Vector3i(wx + 1, wy + 1, wz)
                ));
            }

            // -Y
            if (!isSolid(wx, wy - 1, wz)) {
                faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz),
                        new Vector3i(wx, wy, wz + 1),
                        new Vector3i(wx + 1, wy, wz + 1),
                        new Vector3i(wx + 1, wy, wz)
                ));
            }

            // +Y
            if (!isSolid(wx, wy + 1, wz)) {
                faces.add(UnitFace.of(
                        new Vector3i(wx, wy + 1, wz),
                        new Vector3i(wx + 1, wy + 1, wz),
                        new Vector3i(wx + 1, wy + 1, wz + 1),
                        new Vector3i(wx, wy + 1, wz + 1)
                ));
            }

            // -Z
            if (!isSolid(wx, wy, wz - 1)) {
                faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz),
                        new Vector3i(wx + 1, wy, wz),
                        new Vector3i(wx + 1, wy + 1, wz),
                        new Vector3i(wx, wy + 1, wz)
                ));
            }

            // +Z
            if (!isSolid(wx, wy, wz + 1)) {
                faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz + 1),
                        new Vector3i(wx, wy + 1, wz + 1),
                        new Vector3i(wx + 1, wy + 1, wz + 1),
                        new Vector3i(wx + 1, wy, wz + 1)
                ));
            }
        }

        chunk.faces = GridAlignedFaceOptimizer.optimize(faces);
        chunk.edges = new ArrayList<>(Face2Edge.generateEdges(chunk.faces));

        chunk.dirty = false;
    }

    // =========================================================
    // FAST SOLID TEST
    // =========================================================

    private boolean isSolid(int x, int y, int z) {
        Chunk chunk = chunks.get(chunkKey(
                floorDiv16(x),
                floorDiv16(y),
                floorDiv16(z)
        ));

        if (chunk == null) {
            return false;
        }

        return chunk.voxels[index(
                x & MASK,
                y & MASK,
                z & MASK
        )] > 0;
    }

    // =========================================================
    // DIRTY MARKING
    // =========================================================

    private void markDirty(int cx, int cy, int cz) {
        Chunk c = chunks.get(chunkKey(cx, cy, cz));

        if (c != null) {
            c.dirty = true;
        }
    }

    // =========================================================
    // INTERNALS
    // =========================================================

    private static int index(int x, int y, int z) {
        return x | (y << 4) | (z << 8);
    }

    private static int floorDiv16(int v) {
        return Math.floorDiv(v, 16);
    }

    private static long chunkKey(int x, int y, int z) {
        return ((long)(x & 0x1FFFFF) << 42)
                | ((long)(y & 0x1FFFFF) << 21)
                | ((long)(z & 0x1FFFFF));
    }

    private static int unpackX(long k) {
        return signExtend21((int)(k >> 42));
    }

    private static int unpackY(long k) {
        return signExtend21((int)(k >> 21));
    }

    private static int unpackZ(long k) {
        return signExtend21((int)k);
    }

    private static int signExtend21(int v) {
        v &= 0x1FFFFF;

        if ((v & (1 << 20)) != 0) {
            v |= ~0x1FFFFF;
        }

        return v;
    }

    // =========================================================
    // CHUNK
    // =========================================================

    public static class Chunk {
        public Vector3i center;

        final short[] voxels = new short[VOLUME];

        public List<Face> faces = new ArrayList<>();
        public List<Edge> edges = new ArrayList<>();

        boolean dirty = true;

        public Chunk(Vector3i center) {
            this.center = center;
        }
    }
}