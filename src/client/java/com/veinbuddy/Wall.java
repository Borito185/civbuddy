package com.veinbuddy;

import net.minecraft.client.render.BufferBuilder;
import org.joml.*;
import java.util.ArrayList;

public class Wall {
    private static final Vector3fc[] vertices = {
            new Vector3f(0,0,0),
            new Vector3f(1,0,0),
            new Vector3f(1,0,1),
            new Vector3f(0,0,1),

            new Vector3f(0,1,0),
            new Vector3f(1,1,0),
            new Vector3f(1,1,1),
            new Vector3f(0,1,1)
    };
    private static final Vector3ic[] offsets = {
            new Vector3i(1,0,0),
            new Vector3i(-1,0,0),
            new Vector3i(0,1,0),
            new Vector3i(0,-1,0),
            new Vector3i(0,0,1),
            new Vector3i(0,0,-1)
    };

    private static final int[][] quads = {
            { 1, 5, 6, 2 }, // +X
            { 0, 3, 7, 4 }, // -X
            { 4, 7, 6, 5 }, // +Y (top)
            { 0, 1, 2, 3 }, // -Y (bottom)
            { 2, 6, 7, 3 }, // +Z
            { 0, 4, 5, 1 }  // -Z
    };

    private final int x, y, z;
    private final Vector4fc color;
    private int neighborMask;

    public Wall(int x, int y, int z, Vector4fc color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public static void createWalls(ArrayList<Wall> walls, Bounds bounds, Vector4fc color){
        Vector3ic c = bounds.center(), r = bounds.range(); // range = half-extents

        int minX = c.x() - r.x(), maxX = c.x() + r.x();
        int minY = c.y() - r.y(), maxY = c.y() + r.y();
        int minZ = c.z() - r.z(), maxZ = c.z() + r.z();

        for (int x = minX; x <= maxX; x++)
        for (int y = minY; y <= maxY; y++)
        for (int z = minZ; z <= maxZ; z++) {
            if (x != minX && x != maxX &&
                    y != minY && y != maxY &&
                    z != minZ && z != maxZ) {
                continue;
            }
            walls.add(new Wall(x, y, z, color));
        }
    }

    public void addSelection(Bounds bounds, Vector3i mem) {
        int missing = (~neighborMask) & 0x3F;     // bits not set yet
        if (missing == 0)
            return;

        if (mem == null)
            mem = new Vector3i();    // avoid allocs by reusing

        while (missing != 0) {
            int i = Integer.numberOfTrailingZeros(missing);
            missing &= (missing - 1);             // clear lowest-set bit

            mem.set(x,y,z).add(offsets[i]);
            if (bounds.contains(mem)) {
                neighborMask |= (1 << i);         // set the bit
            }
        }
    }

    public boolean isWall() {
        return (neighborMask & 0x3F) != 0x3F;
    }

    public boolean isNotWall(){
        return !isWall();
    }

    public void addToBuffer(BufferBuilder buffer) {
        for (int i = 0; i < 6; i++) {
            int p = 1 << i;
            if ((p & neighborMask) != 0) continue;

            for (int j : quads[i]) {
                Vector3fc vertex = vertices[j];
                buffer.vertex(x + vertex.x(), y + vertex.y(), z + vertex.z())
                        .color(color.x(), color.y(), color.z(), color.w());
            }
        }
    }
}
