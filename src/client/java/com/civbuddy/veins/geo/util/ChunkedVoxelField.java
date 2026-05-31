package com.civbuddy.veins.geo.util;

import com.civbuddy.veins.geo.primitives.UnitFace;
import org.joml.Vector3i;

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

    // =========================================================
    // PUBLIC API
    // =========================================================

    public short get(int x, int y, int z) {
        Chunk chunk = chunks.get(chunkKey(
                floorDiv16(x),
                floorDiv16(y),
                floorDiv16(z)
        ));

        if (chunk == null) {
            return 0;
        }

        return chunk.voxels[index(
                x & MASK,
                y & MASK,
                z & MASK
        )];
    }

    public void set(int x, int y, int z, short value) {

        int cx = floorDiv16(x);
        int cy = floorDiv16(y);
        int cz = floorDiv16(z);

        long key = chunkKey(cx, cy, cz);

        Chunk chunk = chunks.computeIfAbsent(key, k -> new Chunk());

        int localX = x & MASK;
        int localY = y & MASK;
        int localZ = z & MASK;

        int index = index(localX, localY, localZ);

        if (chunk.voxels[index] == value) {
            return;
        }

        chunk.voxels[index] = value;
        chunk.dirty = true;

        // Border edits affect neighboring chunk meshes
        if (localX == 0) markDirty(cx - 1, cy, cz);
        if (localX == 15) markDirty(cx + 1, cy, cz);

        if (localY == 0) markDirty(cx, cy - 1, cz);
        if (localY == 15) markDirty(cx, cy + 1, cz);

        if (localZ == 0) markDirty(cx, cy, cz - 1);
        if (localZ == 15) markDirty(cx, cy, cz + 1);
    }

    public void shift(int x, int y, int z, short diff) {
        if (diff == 0) return;

        int cx = floorDiv16(x);
        int cy = floorDiv16(y);
        int cz = floorDiv16(z);

        long key = chunkKey(cx, cy, cz);

        Chunk chunk = chunks.computeIfAbsent(key, k -> new Chunk());

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

    /**
     * Returns merged mesh of all chunk meshes.
     * Only dirty chunks are rebuilt.
     */
    public List<UnitFace> extractSurface() {

        List<UnitFace> faces = new ArrayList<>();

        for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {

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

            faces.addAll(chunk.faces);
        }

        return faces;
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
                chunk.faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz),
                        new Vector3i(wx, wy + 1, wz),
                        new Vector3i(wx, wy + 1, wz + 1),
                        new Vector3i(wx, wy, wz + 1)
                ));
            }

            // +X
            if (!isSolid(wx + 1, wy, wz)) {
                chunk.faces.add(UnitFace.of(
                        new Vector3i(wx + 1, wy, wz),
                        new Vector3i(wx + 1, wy, wz + 1),
                        new Vector3i(wx + 1, wy + 1, wz + 1),
                        new Vector3i(wx + 1, wy + 1, wz)
                ));
            }

            // -Y
            if (!isSolid(wx, wy - 1, wz)) {
                chunk.faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz),
                        new Vector3i(wx, wy, wz + 1),
                        new Vector3i(wx + 1, wy, wz + 1),
                        new Vector3i(wx + 1, wy, wz)
                ));
            }

            // +Y
            if (!isSolid(wx, wy + 1, wz)) {
                chunk.faces.add(UnitFace.of(
                        new Vector3i(wx, wy + 1, wz),
                        new Vector3i(wx + 1, wy + 1, wz),
                        new Vector3i(wx + 1, wy + 1, wz + 1),
                        new Vector3i(wx, wy + 1, wz + 1)
                ));
            }

            // -Z
            if (!isSolid(wx, wy, wz - 1)) {
                chunk.faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz),
                        new Vector3i(wx + 1, wy, wz),
                        new Vector3i(wx + 1, wy + 1, wz),
                        new Vector3i(wx, wy + 1, wz)
                ));
            }

            // +Z
            if (!isSolid(wx, wy, wz + 1)) {
                chunk.faces.add(UnitFace.of(
                        new Vector3i(wx, wy, wz + 1),
                        new Vector3i(wx, wy + 1, wz + 1),
                        new Vector3i(wx + 1, wy + 1, wz + 1),
                        new Vector3i(wx + 1, wy, wz + 1)
                ));
            }
        }

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

    private static class Chunk {

        final short[] voxels = new short[VOLUME];

        final List<UnitFace> faces = new ArrayList<>();

        boolean dirty = true;
    }
}