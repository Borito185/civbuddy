package com.veinbuddy;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.*;
import org.joml.Math;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class DigShape {
    private static final Vec3i[] VERTICES = {
            new Vec3i(0,0,0),
            new Vec3i(1,0,0),
            new Vec3i(1,0,1),
            new Vec3i(0,0,1),

            new Vec3i(0,1,0),
            new Vec3i(1,1,0),
            new Vec3i(1,1,1),
            new Vec3i(0,1,1)
    };
    private static final Vec3i[] OFFSETS = {
            new Vec3i(1,0,0),
            new Vec3i(-1,0,0),
            new Vec3i(0,1,0),
            new Vec3i(0,-1,0),
            new Vec3i(0,0,1),
            new Vec3i(0,0,-1)
    };

    private static final int[][] QUADS = {
            { 1, 5, 6, 2 }, // +X
            { 0, 3, 7, 4 }, // -X
            { 4, 7, 6, 5 }, // +Y (top)
            { 0, 1, 2, 3 }, // -Y (bottom)
            { 2, 6, 7, 3 }, // +Z
            { 0, 4, 5, 1 }  // -Z
    };

    public static final class Border {
        public final Vec3i pos;
        private int neighbors;

        private Border(Vec3i pos) {
            this.pos = pos;
        }

        private void cullFaces(Range range) {
            int missing = (~neighbors) & 0x3F;     // bits not set yet
            if (missing == 0)
                return;

            while (missing != 0) {
                int i = Integer.numberOfTrailingZeros(missing);
                missing &= (missing - 1);             // clear lowest-set bit

                Vec3i offset = OFFSETS[i];

                if (!range.contains(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ()))
                    continue;
                neighbors |= (1 << i);
            }
        }

        public void addWallsToBuffer(BufferBuilder buffer, Vector4f color) {
            if (color.w() <= 0) return;

            for (int i = 0; i < 6; i++) {
                int p = 1 << i;
                if ((p & neighbors) != 0) continue;

                int[] quad = QUADS[i];

                addToBuffer(buffer, VERTICES[quad[0]], color);
                addToBuffer(buffer, VERTICES[quad[1]], color);
                addToBuffer(buffer, VERTICES[quad[2]], color);
                addToBuffer(buffer, VERTICES[quad[3]], color);
            }
        }

        public void addGridToBuffer(BufferBuilder buffer, Vector4f color) {
            if (color.w() <= 0) return;

            for (int i = 0; i < 6; i++) {
                int p = 1 << i;
                if ((p & neighbors) != 0) continue;

                int[] quad = QUADS[i];

                addToBuffer(buffer, VERTICES[quad[0]], color);
                addToBuffer(buffer, VERTICES[quad[1]], color);
                addToBuffer(buffer, VERTICES[quad[1]], color);
                addToBuffer(buffer, VERTICES[quad[2]], color);
                addToBuffer(buffer, VERTICES[quad[2]], color);
                addToBuffer(buffer, VERTICES[quad[3]], color);
                addToBuffer(buffer, VERTICES[quad[3]], color);
                addToBuffer(buffer, VERTICES[quad[0]], color);
            }
        }

        private void addToBuffer(BufferBuilder buffer, Vec3i offset, Vector4fc color) {
            buffer.vertex(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ())
                    .color(color.x(), color.y(), color.z(), color.w());
        }

        public boolean isWall() {
            return (neighbors & 0x3F) != 0x3F;
        }

        private void reset() {
            neighbors = 0;
        }
    }

    public static final class Range {
        public final Vec3i center;
        public final Vec3i radius;

        public Range(Vec3i center, Vec3i radius) {
            this.center = center;
            this.radius = radius;
        }

        public boolean contains(int x, int y, int z) {
            int dx = Math.abs(x - center.getX());
            int dy = Math.abs(y - center.getY());
            int dz = Math.abs(z - center.getZ());

            return dx <= radius.getX() && dy <= radius.getY() && dz <= radius.getZ();
        }

        public boolean intersects(Vec3d close, Vec3d far) {
            VoxelShape shape = VoxelShapes.cuboid(center.getX(), center.getY(), center.getZ(), center.getX() + 1, center.getY() + 1, center.getZ() + 1);
            BlockHitResult raycast = shape.raycast(close, far, BlockPos.ORIGIN);
            return raycast != null;
        }

        public Stream<Vec3i> stream() {
            Stream.Builder<Vec3i> builder = Stream.builder();

            Vec3i c = center, r=radius;
            int minX = c.getX() - r.getX(), maxX = c.getX() + r.getX();
            int minY = c.getY() - r.getY(), maxY = c.getY() + r.getY();
            int minZ = c.getZ() - r.getZ(), maxZ = c.getZ() + r.getZ();

            for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
            if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ)
                builder.add(new Vec3i(x, y, z));

            return builder.build();
        }
    }

    private final HashMap<Vec3i, Border> borders = new HashMap<>();
    private final ArrayList<Range> ranges = new ArrayList<>();

    private Vector4f wallColor;
    private Vector4f gridColor;

    public void setColors(Vector4fc wallColor, Vector4fc gridColor) {
        this.wallColor = new Vector4f(wallColor);
        this.gridColor = new Vector4f(gridColor);
    }

    public void add(Range range) {
        ranges.add(range);

        rebuild();
    }

    public void add(List<Range> newRanges) {
        ranges.addAll(newRanges);

        rebuild();
    }

    public void remove(Range range) {
        ranges.remove(range);

        rebuild();
    }

    public void removeFirst(Vec3d cameraPos, Vec3d cameraDir) {
        Range closest = null;
        float closestDist = Float.MAX_VALUE;

        Vec3d closeEnd = cameraPos.subtract(cameraDir);
        Vec3d farEnd = cameraPos.add(cameraDir.multiply(1000));

        for (Range bounds : ranges) {
            if (bounds.intersects(closeEnd, farEnd)) continue;

            float distance = (float) bounds.center.getSquaredDistanceFromCenter((double) cameraPos.x,(double) cameraPos.y, (double)cameraPos.z);
            if (distance >= closestDist) continue;

            closest = bounds;
            closestDist = distance;
        }

        if (closest == null) return;
        remove(closest);
    }

    public void clear() {
        ranges.clear();

        rebuild();
    }

    public void rebuild() {
        borders.clear();
        for (Range range : ranges) {
            range.stream().forEach(v -> {
                borders.put(v, new Border(v));
            });
        }

        cullFaces();
    }

    private void cullFaces() {
        List<Vec3i> toRemove = new ArrayList<>();
        for (Border value : borders.values()) {
            value.reset();
            for (Range range : ranges) {
                value.cullFaces(range);
            }
            if (value.isWall()) continue;

            toRemove.add(value.pos);
        }

        for (Vec3i pos : toRemove) {
            borders.remove(pos);
        }
    }

    public void addWallsToBuffer(BufferBuilder buff) {
        for (Border border : this.borders.values()) {
            border.addWallsToBuffer(buff, wallColor);
        }
    }

    public void addGridToBuffer(BufferBuilder buff) {
        for (Border border : this.borders.values()) {
            border.addGridToBuffer(buff, gridColor);
        }
    }
}
