package com.veinbuddy;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Vector3i;
import java.util.ArrayList;

public class Wall {
    private int x;
    private int y;
    private int z;

    private final boolean[] neighbors = new boolean[27];
    public void AddToBuffer(VertexConsumer buffer) {
        // just add whole block for now

        // floor (y)
        buffer.vertex(x,   y, z);
        buffer.vertex(x+1, y, z);
        buffer.vertex(x+1, y, z+1);
        buffer.vertex(x,   y, z+1);

        // roof (y+1)
        buffer.vertex(x,   y+1, z);
        buffer.vertex(x,   y+1, z+1);
        buffer.vertex(x+1, y+1, z+1);
        buffer.vertex(x+1, y+1, z);

        // forward (z+1)
        buffer.vertex(x,   y,   z+1);
        buffer.vertex(x+1, y,   z+1);
        buffer.vertex(x+1, y+1, z+1);
        buffer.vertex(x,   y+1, z+1);

        // backwards (z)
        buffer.vertex(x,   y+1, z);
        buffer.vertex(x+1, y+1, z);
        buffer.vertex(x+1, y,   z);
        buffer.vertex(x,   y,   z);

        // left (x)
        buffer.vertex(x,   y,   z+1);
        buffer.vertex(x,   y,   z);
        buffer.vertex(x,   y+1, z);
        buffer.vertex(x,   y+1, z+1);

        // right (x+1)
        buffer.vertex(x+1, y,   z);
        buffer.vertex(x+1, y,   z+1);
        buffer.vertex(x+1, y+1, z+1);
        buffer.vertex(x+1, y+1, z);
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

        if (!neighbors[14]) return false;

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
