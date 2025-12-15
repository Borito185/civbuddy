package com.civbuddy.veins.render;

import com.civbuddy.veins.geo.primitives.Edge;
import com.civbuddy.veins.geo.primitives.Face;
import com.civbuddy.veins.geo.shapes.CompoundShape;
import com.civbuddy.veins.geo.util.Face2Edge;
import com.civbuddy.veins.geo.util.GridAlignedEdgeOptimizer;
import com.civbuddy.veins.geo.util.GridAlignedFaceOptimizer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import java.util.Collection;

public class ShapeRenderer {
    private final CompoundShape shape = new CompoundShape();
    private Collection<Face> faces;
    private Collection<Edge> edges;

    public ShapeRenderer() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::draw);
    }

    public CompoundShape getShape() {
        return shape;
    }

    public void notifyChange() {
        faces = shape.getFaces();
        edges = Face2Edge.generateEdges(faces);

        faces = GridAlignedFaceOptimizer.optimize(faces);
        edges = GridAlignedEdgeOptimizer.optimize(edges);
    }

    private void draw(WorldRenderContext ctx) {
        if (ctx.matrixStack() == null || ctx.consumers() == null) return;

        var ms = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = ms.peek().getPositionMatrix();

        drawLines(ctx, mat);
        drawFaces(ctx, mat);

        ms.pop();
    }

    private void drawFaces(WorldRenderContext ctx, Matrix4f mat) {
        var vc = ctx.consumers().getBuffer(RenderLayer.getDebugQuads());
        for (Face f : faces) {
            Vector4f c = new Vector4f(1,0,0,0.2f); // 0..1 TODO: hardcoded
            vc.vertex(mat, f.a().x, f.a().y, f.a().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.b().x, f.b().y, f.b().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.c().x, f.c().y, f.c().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.d().x, f.d().y, f.d().z).color(c.x, c.y, c.z, c.w);
        }
    }

    private void drawLines(WorldRenderContext ctx, Matrix4f mat) {
        var vc = ctx.consumers().getBuffer(RenderLayer.getDebugLineStrip(.2));
        float r = 0, g = 0, b = 0, a = 1f;
        float nx = 0, ny = 0, nz = 0;

        for (Edge e : edges) {
            Vector3i A = e.a();
            Vector3i B = e.b();

            vc.vertex(mat, A.x, A.y, A.z).color(r, g, b, 0).normal(nx, ny, nz);

            vc.vertex(mat, A.x, A.y, A.z).color(r, g, b, a).normal(nx, ny, nz);
            vc.vertex(mat, B.x, B.y, B.z).color(r, g, b, a).normal(nx, ny, nz);

            // Break the strip so the next edge doesn't connect to this one
            vc.vertex(mat, B.x, B.y, B.z).color(r, g, b, 0).normal(nx, ny, nz);
        }
    }
}
