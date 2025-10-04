package com.veinbuddy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class SimpleRenderer implements AutoCloseable {
    private final RenderPipeline WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("veinbuddy", "walls_pipeline"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthBias(-2.0f, -0.002f)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .build());

    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.SOLID_BUFFER_SIZE);
    private BuiltBuffer buffer;
    private GpuBuffer wallVertexBuffer;

    public SimpleRenderer() {
        WorldRenderEvents.LAST.register(this::onRender);
    }

    private void onRender(WorldRenderContext ctx) {
        if (buffer == null || wallVertexBuffer == null) return;

        draw(ctx, WALLS, buffer, wallVertexBuffer);
    }

    private static void draw(WorldRenderContext ctx, RenderPipeline pipeline, BuiltBuffer builtBuffer, GpuBuffer vertices) {
        BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
        VertexFormat format = drawParameters.format();
        Vec3d camera = ctx.camera().getPos();

        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.DrawMode.QUADS) {
            // Sort the quads if there is translucency
            builtBuffer.sortQuads(allocator, VertexSorter.byDistance(camera.toVector3f()));

            // Upload the index buffer
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.getSortedBuffer());
            indexType = builtBuffer.getDrawParameters().indexType();
        } else {
            // Use the general shape index buffer for non-quad draw modes
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getIndexBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.getIndexType();
        }

        // Actually execute the draw
        Matrix4f m = new Matrix4f(RenderSystem.getModelViewMatrix());
        m.translate((float)-camera.x, (float)-camera.y, (float)-camera.z);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(m, new Vector4f(1f, 1f, 1f, 1f), RenderSystem.getModelOffset(), RenderSystem.getTextureMatrix(), 1f);
        Framebuffer fb = BlockRenderLayerGroup.TRANSLUCENT.getFramebuffer();

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "veinbuddy walls", fb.getColorAttachmentView(), OptionalInt.empty(), fb.getDepthAttachmentView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // The base vertex is the starting index when we copied the data into the vertex buffer divided by vertex size
            //noinspection ConstantValue
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }
    }

    public void buildMesh(List<Bounds> selections){
        if (buffer != null)
        {
            buffer.close();
            buffer = null;
        }

        ArrayList<Wall> walls = new ArrayList<>();

        // find boundaries
        for (Bounds selection : selections) {
            Wall.createWalls(walls, selection, new Vector4f(1,0,0,0.5f));
        }

        // mark overlapping
        Vector3i temp = new Vector3i();
        for (Wall wall : walls) {
            for (Bounds selection : selections) {
                wall.addSelection(selection, temp);
                if (!wall.isWall()) break;
            }
        }

        // remove non-walls
        walls.removeIf(Wall::isNotWall);

        BufferBuilder builder = new BufferBuilder(allocator, WALLS.getVertexFormatMode(), WALLS.getVertexFormat());

        for (Wall wall : walls) {
            wall.addToBuffer(builder);
        }
        buffer = builder.end();

        if (null != wallVertexBuffer) wallVertexBuffer.close();
        wallVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Walls", GpuBuffer.USAGE_VERTEX, buffer.getBuffer());
    }

    @Override
    public void close() throws Exception {
        if (buffer != null) buffer.close();
        if (wallVertexBuffer != null) wallVertexBuffer.close();
    }
}
