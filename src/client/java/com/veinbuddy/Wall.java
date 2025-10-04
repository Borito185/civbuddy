package com.veinbuddy;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Vector3i;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Wall {
    private int x;
    private int y;
    private int z;

    private final boolean[] neighbors = new boolean[27];
    private static int nIdx(int dx, int dy, int dz) {
        // z-major: (dx+1) + (dy+1)*3 + (dz+1)*9
        return (dx + 1) + (dy + 1) * 3 + (dz + 1) * 9;
    }

    public void AddToBuffer(BufferBuilder buffer) {
        // bottom (0,-1,0)
        if (!neighbors[nIdx(0, -1, 0)]) {
            buffer.vertex(x,   y, z);
            buffer.vertex(x+1, y, z+1);
            buffer.vertex(x+1, y, z);
            buffer.vertex(x,   y, z);
            buffer.vertex(x,   y, z+1);
            buffer.vertex(x+1, y, z+1);
        }

        // top (0,1,0)
        if (!neighbors[nIdx(0, 1, 0)]) {
            buffer.vertex(x,   y+1, z);
            buffer.vertex(x+1, y+1, z);
            buffer.vertex(x+1, y+1, z+1);
            buffer.vertex(x,   y+1, z);
            buffer.vertex(x+1, y+1, z+1);
            buffer.vertex(x,   y+1, z+1);
        }

        // front (0,0,1)  (z+1)
        if (!neighbors[nIdx(0, 0, 1)]) {
            buffer.vertex(x+1, y,   z);
            buffer.vertex(x+1, y,   z+1);
            buffer.vertex(x+1, y+1, z+1);
            buffer.vertex(x+1, y,   z);
            buffer.vertex(x+1, y+1, z+1);
            buffer.vertex(x+1, y+1, z);
        }

        // back (0,0,-1)
        if (!neighbors[nIdx(0, 0, -1)]) {
            buffer.vertex(x, y,   z);
            buffer.vertex(x, y+1, z+1);
            buffer.vertex(x, y,   z+1);
            buffer.vertex(x, y,   z);
            buffer.vertex(x, y+1, z);
            buffer.vertex(x, y+1, z+1);
        }

        // left (-1,0,0)
        if (!neighbors[nIdx(-1, 0, 0)]) {
            buffer.vertex(x,   y, z);
            buffer.vertex(x+1, y+1, z);
            buffer.vertex(x+1, y, z);
            buffer.vertex(x,   y, z);
            buffer.vertex(x,   y+1, z);
            buffer.vertex(x+1, y+1, z);
        }

        // right (1,0,0)
        if (!neighbors[nIdx(1, 0, 0)]) {
            buffer.vertex(x,   y,   z+1);
            buffer.vertex(x+1, y,   z+1);
            buffer.vertex(x+1, y+1, z+1);
            buffer.vertex(x,   y,   z+1);
            buffer.vertex(x+1, y+1, z+1);
            buffer.vertex(x,   y+1, z+1);
        }
    }
    // Fill from some world/block source
    public Wall(Vector3i center) {
        x = center.x;
        y = center.y;
        z = center.z;
    }

    public Wall(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean IsWall() {
        boolean hasTrue = false;
        boolean hasFalse = false;

        if (!neighbors[13]) return false;

        for (boolean b : neighbors) {
            if (b)
                hasTrue = true;
            else
                hasFalse = true;
        }

        return hasTrue && hasFalse;
    }

    public boolean IsNotWall(){
        return !IsWall();
    }

    public void AddSelection(Bounds bounds, Vector3i mem) {
        if (mem == null)
            mem = new Vector3i();

        int i = 0;
        for (int lx = -1; lx <= 1; lx++)
        for (int ly = -1; ly <= 1; ly++)
        for (int lz = -1; lz <= 1; lz++) {
            mem.set(x + lx, y + ly, z + lz);
            neighbors[i] = neighbors[i] || bounds.contains(mem);
            i++;
        }
    }

    public static void CreateWalls(ArrayList<Wall> walls, Bounds bounds){
        Vector3i c = bounds.center(), r = bounds.range(); // range = half-extents
        int minX = c.x - r.x, maxX = c.x + r.x;
        int minY = c.y - r.y, maxY = c.y + r.y;
        int minZ = c.z - r.z, maxZ = c.z + r.z;

        for (int x = minX; x <= maxX; x++)
        for (int y = minY; y <= maxY; y++)
        for (int z = minZ; z <= maxZ; z++) {
            if (x != minX && x != maxX &&
                    y != minY && y != maxY &&
                    z != minZ && z != maxZ) {
                continue;
            }
            walls.add(new Wall(x, y, z));
        }
    }


}
