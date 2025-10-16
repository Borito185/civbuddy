package com.civbuddy.veins;

import net.minecraft.client.render.BufferBuilder;
import org.joml.*;

import java.util.HashSet;
import java.util.Objects;

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
    private final Vector4fc lineColor;
    private int neighborMask;

    public Wall(int x, int y, int z, Vector4fc color, Vector4fc lineColor) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.lineColor = lineColor;
    }

    public static void createWalls(HashSet<Wall> walls, Bounds bounds, Vector4fc color, Vector4fc lineColor){
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
            walls.add(new Wall(x, y, z, color, lineColor));
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

    public void addWallsToBuffer(BufferBuilder buffer) {
        if (color.w() <= 0) return;

        for (int i = 0; i < 6; i++) {
            int p = 1 << i;
            if ((p & neighborMask) != 0) continue;

            int[] quad = quads[i];

            addToBuffer(buffer, vertices[quad[0]], color);
            addToBuffer(buffer, vertices[quad[1]], color);
            addToBuffer(buffer, vertices[quad[2]], color);
            addToBuffer(buffer, vertices[quad[3]], color);
        }
    }

    public void addGridToBuffer(BufferBuilder buffer) {
        if (lineColor.w() <= 0) return;

        for (int i = 0; i < 6; i++) {
            int p = 1 << i;
            if ((p & neighborMask) != 0) continue;

            int[] quad = quads[i];

            addToBuffer(buffer, vertices[quad[0]], lineColor);
            addToBuffer(buffer, vertices[quad[1]], lineColor);
            addToBuffer(buffer, vertices[quad[1]], lineColor);
            addToBuffer(buffer, vertices[quad[2]], lineColor);
            addToBuffer(buffer, vertices[quad[2]], lineColor);
            addToBuffer(buffer, vertices[quad[3]], lineColor);
            addToBuffer(buffer, vertices[quad[3]], lineColor);
            addToBuffer(buffer, vertices[quad[0]], lineColor);
        }
    }

    private void addToBuffer(BufferBuilder buffer, Vector3fc offset, Vector4fc color) {
        buffer.vertex(x + offset.x(), y + offset.y(), z + offset.z())
                .color(color.x(), color.y(), color.z(), color.w());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wall wall)) return false;
        return x == wall.x && y == wall.y && z == wall.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
