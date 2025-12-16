package com.civbuddy.veins.render;

import com.civbuddy.veins.geo.primitives.Edge;
import com.civbuddy.veins.geo.primitives.Face;
import com.civbuddy.veins.geo.primitives.UnitFace;
import com.civbuddy.veins.geo.shapes.CompoundShape;
import com.civbuddy.veins.geo.shapes.VoxelShape;
import com.civbuddy.veins.geo.util.Face2Edge;
import com.civbuddy.veins.geo.util.GridAlignedEdgeOptimizer;
import com.civbuddy.veins.geo.util.GridAlignedFaceOptimizer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Math;
import org.joml.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static com.civbuddy.veins.render.RenderLayers.LINES;
import static com.civbuddy.veins.render.RenderLayers.TRANSLUCENT_QUADS;

public class ShapeRenderer {
    public static float grid_alpha = 1;
    private final static float NORMAL_BIAS = 0.001f;
    private final CompoundShape shape = new CompoundShape();
    private Collection<Face> faces;
    private Collection<Edge> edges;

    private Vector4f color = new Vector4f(1,0,0,0.2f);
    private boolean hasGrid = true;

    public ShapeRenderer() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::draw);
    }

    public void setStyle(Vector4f color, boolean hasGrid) {
        this.color = color;
        this.hasGrid = hasGrid;
    }

    public Collection<VoxelShape> getInnerShapes() {
        return new ArrayList<>(shape.getInnerShapes());
    }

    public void setInnerShapes(Set<VoxelShape> shapes) {
        shape.set(shapes);
        remesh();
    }

    private void remesh() {
        Collection<UnitFace> unitFaces = shape.getFaces();
        edges = Face2Edge.generateEdges(unitFaces);

        faces = GridAlignedFaceOptimizer.optimize(unitFaces);
        edges = GridAlignedEdgeOptimizer.optimize(edges);
    }

    private void draw(WorldRenderContext ctx) {
        if (ctx.matrixStack() == null || ctx.consumers() == null) return;
        if (!hasFaces() && !hasGrid()) return;

        Profiler profiler = Profilers.get();

        profiler.push("vein_rendering");

        var ms = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = ms.peek().getPositionMatrix();

        profiler.push("grid");

        if (hasGrid())
            drawLines(ctx, mat);
        profiler.swap("walls");
        if (hasFaces())
            drawFaces(ctx, mat);
        profiler.pop();

        ms.pop();
        profiler.pop();
    }

    private boolean hasFaces() {
        return faces != null && !faces.isEmpty();
    }

    private boolean hasGrid() {
        return hasGrid && edges != null && !edges.isEmpty();
    }

    private void drawFaces(WorldRenderContext ctx, Matrix4f mat) {
        final VertexConsumer vc = ctx.consumers().getBuffer(TRANSLUCENT_QUADS);

        final Vec3d cp = ctx.camera().getCameraPos();
        final float cx = (float) cp.x, cy = (float) cp.y, cz = (float) cp.z;

        final float r = color.x, g = color.y, b = color.z, a = color.w;

        for (Face f : faces) {
            // normal (assumed normalized)
            final Vector3fc n0 = f.normal();
            float nx = n0.x(), ny = n0.y(), nz = n0.z();

            // center
            final Vector3fc ctr = f.center();
            final float tx = cx - ctr.x();
            final float ty = cy - ctr.y();
            final float tz = cz - ctr.z();

            // flip normal toward camera
            if (nx * tx + ny * ty + nz * tz < 0f) {
                nx = -nx; ny = -ny; nz = -nz;
            }

            final float ox = nx * NORMAL_BIAS;
            final float oy = ny * NORMAL_BIAS;
            final float oz = nz * NORMAL_BIAS;

            final Vector3ic A = f.a(), B = f.b(), C = f.c(), D = f.d();

            vc.vertex(mat, A.x() + ox, A.y() + oy, A.z() + oz).color(r, g, b, a);
            vc.vertex(mat, B.x() + ox, B.y() + oy, B.z() + oz).color(r, g, b, a);
            vc.vertex(mat, C.x() + ox, C.y() + oy, C.z() + oz).color(r, g, b, a);
            vc.vertex(mat, D.x() + ox, D.y() + oy, D.z() + oz).color(r, g, b, a);
        }
    }

    private void drawLines(WorldRenderContext ctx, Matrix4f mat) {
        final VertexConsumer vc = ctx.consumers().getBuffer(LINES);

        final Vec3d cp = ctx.camera().getCameraPos();
        final float cx = (float) cp.x, cy = (float) cp.y, cz = (float) cp.z;

        final float r = 0f, g = 0f, b = 0f, a = grid_alpha;

        for (Edge e : edges) {
            final Vector3ic A = e.a();
            final Vector3ic Bp = e.b();

            // bias A toward camera
            {
                final float dx = cx - A.x();
                final float dy = cy - A.y();
                final float dz = cz - A.z();
                final float inv = NORMAL_BIAS * invLen(dx, dy, dz);
                vc.vertex(mat, A.x() + dx * inv, A.y() + dy * inv, A.z() + dz * inv).color(r, g, b, a);
            }

            // bias B toward camera
            {
                final float dx = cx - Bp.x();
                final float dy = cy - Bp.y();
                final float dz = cz - Bp.z();
                final float inv = NORMAL_BIAS * invLen(dx, dy, dz);
                vc.vertex(mat, Bp.x() + dx * inv, Bp.y() + dy * inv, Bp.z() + dz * inv).color(r, g, b, a);
            }
        }
    }

    /** Returns 1/len, safely (0 if len==0). */
    private static float invLen(float x, float y, float z) {
        final float d2 = x * x + y * y + z * z;
        return d2 > 0f ? (float) (1.0 / Math.sqrt(d2)) : 0f;
    }
}
