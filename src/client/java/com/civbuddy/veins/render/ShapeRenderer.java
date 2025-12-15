package com.civbuddy.veins.render;

import com.civbuddy.veins.geo.CompoundShape;
import com.civbuddy.veins.geo.Edge;
import com.civbuddy.veins.geo.Face;
import com.civbuddy.veins.geo.ShapeUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.Collection;
import java.util.HashSet;

public class ShapeRenderer {
    private final CompoundShape shape = new CompoundShape();

    public ShapeRenderer() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::draw);
    }

    public CompoundShape getShape() {
        return shape;
    }

    public void notifyChange() {

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
        Collection<Face> faces = shape.getFaces();
        for (Face f : faces) {
            Vector4f c = f.color(); // 0..1
            vc.vertex(mat, f.a().x, f.a().y, f.a().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.b().x, f.b().y, f.b().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.c().x, f.c().y, f.c().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.d().x, f.d().y, f.d().z).color(c.x, c.y, c.z, c.w);
        }
    }

    private void drawLines(WorldRenderContext ctx, Matrix4f mat) {
        var vc = ctx.consumers().getBuffer(RenderLayer.getDebugLineStrip(.2));
        Collection<Face> faces = shape.getFaces();
        HashSet<Edge> edges = new HashSet<>();
        ShapeUtils.generateEdges(edges, faces);
        float r = 0, g = 0, b = 0, a = 1f;
        float nx = 0, ny = 0, nz = 0;

        for (Edge e : edges) {
            Vector3f A = e.a();
            Vector3f B = e.b();

            vc.vertex(mat, A.x, A.y, A.z).color(r, g, b, 0).normal(nx, ny, nz);

            vc.vertex(mat, A.x, A.y, A.z).color(r, g, b, a).normal(nx, ny, nz);
            vc.vertex(mat, B.x, B.y, B.z).color(r, g, b, a).normal(nx, ny, nz);

            // Break the strip so the next edge doesn't connect to this one
            vc.vertex(mat, B.x, B.y, B.z).color(r, g, b, 0).normal(nx, ny, nz);
        }
    }
}
