package com.civbuddy.veins.render;

import com.civbuddy.veins.geo.primitives.Edge;
import com.civbuddy.veins.geo.primitives.Face;
import com.civbuddy.veins.geo.shapes.CompoundShape;
import com.civbuddy.veins.geo.shapes.VoxelShape;
import com.civbuddy.veins.geo.util.Face2Edge;
import com.civbuddy.veins.geo.util.GridAlignedEdgeOptimizer;
import com.civbuddy.veins.geo.util.GridAlignedFaceOptimizer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class ShapeRenderer {
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
        faces = shape.getFaces();
        edges = Face2Edge.generateEdges(faces);

        faces = GridAlignedFaceOptimizer.optimize(faces);
        edges = GridAlignedEdgeOptimizer.optimize(edges);
    }

    private void draw(WorldRenderContext ctx) {
        if (ctx.matrixStack() == null || ctx.consumers() == null) return;
        if (!hasFaces() && !hasGrid()) return;

        var ms = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = ms.peek().getPositionMatrix();

        if (hasGrid())
            drawLines(ctx, mat);
        if (hasFaces())
            drawFaces(ctx, mat);

        ms.pop();
    }

    private boolean hasFaces() {
        return faces != null && !faces.isEmpty();
    }

    private boolean hasGrid() {
        return hasGrid && edges != null && !edges.isEmpty();
    }

    private void drawFaces(WorldRenderContext ctx, Matrix4f mat) {
        var vc = ctx.consumers().getBuffer(RenderLayer.getDebugStructureQuads());
        Vec3d cameraPos = ctx.camera().getCameraPos();
        for (Face f : faces) {
            Vector3f offset = biasTowardCamera(f, cameraPos);
            Vector4f c = color; // 0..1

            setPositionColor(vc, mat, new Vector3f(f.a()).add(offset), c);
            setPositionColor(vc, mat, new Vector3f(f.b()).add(offset), c);
            setPositionColor(vc, mat, new Vector3f(f.c()).add(offset), c);
            setPositionColor(vc, mat, new Vector3f(f.d()).add(offset), c);
        }
    }

    private void drawLines(WorldRenderContext ctx, Matrix4f mat) {
        var vc = ctx.consumers().getBuffer(RenderLayer.getDebugLineStrip(.2));
        Vector4f black = new Vector4f(0,0,0,1);
        Vector4f hidden = new Vector4f(0,0,0,0);
        float nx = 0, ny = 0, nz = 0;
        Vec3d cameraPos = ctx.camera().getCameraPos();

        for (Edge e : edges) {
            Vector3f A = biasTowardCamera(e.a(), cameraPos);
            Vector3f B = biasTowardCamera(e.b(), cameraPos);

            setPositionColor(vc, mat, A, hidden).normal(nx, ny, nz);
            setPositionColor(vc, mat, A, black).normal(nx, ny, nz);
            setPositionColor(vc, mat, B, black).normal(nx, ny, nz);
            setPositionColor(vc, mat, B, hidden).normal(nx, ny, nz);
        }
    }

    /** Returns world-space offset along the face normal, flipped to face the camera. */
    private static Vector3f biasTowardCamera(Face f, Vec3d cp) {
        Vector3f n = f.normal();          // normalized
        Vector3f ctr = f.center();

        // world-space camera position
        Vector3f toCam = new Vector3f((float)cp.x, (float)cp.y, (float)cp.z).sub(ctr);

        if (n.dot(toCam) < 0f) n.negate(); // make normal point toward camera
        return n.mul(NORMAL_BIAS);
    }

    private static Vector3f biasTowardCamera(Vector3i pos, Vec3d cp) {
        float dx = (float) cp.x - pos.x;
        float dy = (float) cp.y - pos.y;
        float dz = (float) cp.z - pos.z;

        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0f) return new Vector3f(pos);

        float inv = NORMAL_BIAS / len;
        return new Vector3f(
                pos.x + dx * inv,
                pos.y + dy * inv,
                pos.z + dz * inv
        );
    }
    private static VertexConsumer setPositionColor(VertexConsumer vc, Matrix4f mat, Vector3f pos, Vector4f color) {
        return vc.vertex(mat, pos.x, pos.y, pos.z).color(color.x, color.y, color.z, color.w);
    }
}
