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
            // ABCD winding (front face); if you want double-sided, also emit DCBA.
            vc.vertex(mat, f.a().x, f.a().y, f.a().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.b().x, f.b().y, f.b().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.c().x, f.c().y, f.c().z).color(c.x, c.y, c.z, c.w);
            vc.vertex(mat, f.d().x, f.d().y, f.d().z).color(c.x, c.y, c.z, c.w);
        }
    }

    private void drawLines(WorldRenderContext ctx, Matrix4f mat) {
        var vc = ctx.consumers().getBuffer(RenderLayer.getLines());
        Collection<Face> faces = shape.getFaces();
        HashSet<Edge> edges = new HashSet<>();
        ShapeUtils.generateEdges(edges, faces);
        for (Edge e : edges) {
            var a = e.a();
            var b = e.b();
            // Vanilla line layer expects a "normal" too; using (0,1,0) is fine for debug lines.
            vc.vertex(mat, a.x, a.y, a.z).color(0, 0, 0, .3f).normal(0f, 1f, 0f);
            vc.vertex(mat, b.x, b.y, b.z).color(0, 0, 0, .3f).normal(0f, 1f, 0f);
        }
    }
}
