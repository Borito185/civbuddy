package com.veinbuddy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.nio.ByteBuffer;
import java.util.*;

public class SimpleRenderer implements AutoCloseable {
    private final RenderPipeline WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("veinbuddy", "walls_pipeline"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthBias(-1.0f, -0.001f) // ensures it draws over blocks
            .withDepthWrite(true) // hides clouds cus the look weird
            .withCull(false) // shows the backface
            .build());

    private final RenderPipeline GRID = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("veinbuddy", "grid_pipeline"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withDepthBias(-0.5f, -0.002f)
            .build());

    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.SOLID_BUFFER_SIZE);
    private BuiltBuffer wallBuffer;
    private GpuBuffer wallVertices;
    private GpuBuffer gridVertices;

    public SimpleRenderer() {
        WorldRenderEvents.LAST.register(this::onRender);
    }

    private void onRender(WorldRenderContext ctx) {
        if (gridVertices != null)
            draw(ctx, GRID, gridVertices, null);
        if (wallVertices != null)
            draw(ctx, WALLS, wallVertices, getWallIndices(ctx));
    }

    private GpuBuffer wallIndices;
    private Pair<GpuBuffer, VertexFormat.IndexType> getWallIndices(WorldRenderContext ctx) {
        // sort quads for correct translucency
        Vec3d camera = ctx.camera().getPos();
        wallBuffer.sortQuads(allocator, VertexSorter.byDistance(camera.toVector3f()));
        ByteBuffer sortedBuffer = wallBuffer.getSortedBuffer();

        // send to gpu
        wallIndices = WALLS.getVertexFormat().uploadImmediateIndexBuffer(sortedBuffer);
        return new Pair<>(wallIndices, wallBuffer.getDrawParameters().indexType());
    }

    private void draw(WorldRenderContext ctx, RenderPipeline pipeline, GpuBuffer vertices, @Nullable Pair<GpuBuffer, VertexFormat.IndexType> indices) {
        Framebuffer fb = BlockRenderLayer.TRANSLUCENT.getFramebuffer();

        Vec3d camera = ctx.camera().getPos();
        Matrix4f m = new Matrix4f(RenderSystem.getModelViewMatrix());
        m.translate((float)-camera.x, (float)-camera.y, (float)-camera.z);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
                m,
                new Vector4f(1f, 1f, 1f, 1f),
                RenderSystem.getModelOffset(),
                RenderSystem.getTextureMatrix(),
                1f
        );

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "veinbuddy walls", fb.getColorAttachmentView(), OptionalInt.empty(), fb.getDepthAttachmentView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, vertices);
            if (indices == null) {
                renderPass.draw(0, vertices.size() / pipeline.getVertexFormat().getVertexSize());
                return;
            }
            GpuBuffer indicesBuffer = indices.getLeft();
            VertexFormat.IndexType indexType = indices.getRight();
            renderPass.setIndexBuffer(indicesBuffer, indexType);
            renderPass.drawIndexed(0, 0, indicesBuffer.size() / indexType.size, 1);
        }
    }

    public void draw(Collection<Wall> walls){
        clear();
        if (walls.isEmpty()) return;

        // build the walls
        BufferBuilder wallBuilder = new BufferBuilder(allocator, WALLS.getVertexFormatMode(), WALLS.getVertexFormat());
        for (Wall wall : walls) {
            wall.addWallsToBuffer(wallBuilder);
        }
        wallBuffer = wallBuilder.endNullable(); // save wall buffer to create indices later

        // build the grid
        BufferBuilder gridBuilder = new BufferBuilder(allocator, GRID.getVertexFormatMode(), GRID.getVertexFormat());
        for (Wall wall : walls) {
            wall.addGridToBuffer(gridBuilder);
        }
        BuiltBuffer gridBuffer = gridBuilder.endNullable();

        // write to GPU
        if (wallBuffer != null && wallBuffer.getDrawParameters().vertexCount() > 0)
            wallVertices = RenderSystem.getDevice().createBuffer(() -> "Walls", GpuBuffer.USAGE_VERTEX, wallBuffer.getBuffer());
        if (gridBuffer != null && gridBuffer.getDrawParameters().vertexCount() > 0)
            gridVertices = RenderSystem.getDevice().createBuffer(() -> "Grid", GpuBuffer.USAGE_VERTEX, gridBuffer.getBuffer());
    }

    public void clear() {
        if (wallBuffer != null) wallBuffer.close();
        if (wallVertices != null) wallVertices.close();
        if (wallIndices != null) wallIndices.close();
        if (gridVertices != null) gridVertices.close();

        wallBuffer = null;
        wallVertices = null;
        wallIndices = null;
        gridVertices = null;
    }

    @Override
    public void close() throws Exception {
        clear();
    }
}
