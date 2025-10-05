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
import org.joml.Vector3i;
import org.joml.Vector4f;
import java.nio.ByteBuffer;
import java.util.*;

public class SimpleRenderer implements AutoCloseable {
    private final RenderPipeline WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("veinbuddy", "walls_pipeline"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthBias(-1.0f, -0.001f)
            .withDepthWrite(true)
            .withCull(false)
            .build());

    private final RenderPipeline GRID = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("veinbuddy", "grid_pipeline"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withDepthBias(-0.5f, -0.002f)
            .build());

    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.SOLID_BUFFER_SIZE);
    private BuiltBuffer wallBuiltBuffer;
    private GpuBuffer wallVertices;
    private GpuBuffer gridVertices;

    public SimpleRenderer() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::onRender);
    }

    private void onRender(WorldRenderContext ctx) {
        if (gridVertices != null)
            draw(ctx, GRID, gridVertices, null);
        if (wallBuiltBuffer != null) {
            draw(ctx, WALLS, wallVertices, createWallIndices(ctx));
        }
    }

    private Pair<GpuBuffer, VertexFormat.IndexType> createWallIndices(WorldRenderContext ctx) {
        Vec3d camera = ctx.camera().getPos();
        wallBuiltBuffer.sortQuads(allocator, VertexSorter.byDistance(camera.toVector3f()));
        ByteBuffer sortedBuffer = wallBuiltBuffer.getSortedBuffer();
        GpuBuffer gpuBuffer = WALLS.getVertexFormat().uploadImmediateIndexBuffer(sortedBuffer);
        return new Pair<>(gpuBuffer, wallBuiltBuffer.getDrawParameters().indexType());
    }

    private void draw(WorldRenderContext ctx, RenderPipeline pipeline, GpuBuffer vertices, @Nullable Pair<GpuBuffer, VertexFormat.IndexType> indices) {
        Framebuffer fb = BlockRenderLayerGroup.TRANSLUCENT.getFramebuffer();

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
            if (indices != null) {
                GpuBuffer indicesBuffer = indices.getLeft();
                VertexFormat.IndexType indexType = indices.getRight();
                renderPass.setIndexBuffer(indicesBuffer, indexType);
                renderPass.drawIndexed(0, 0, indicesBuffer.size() / indexType.size, 1);
            } else {
                renderPass.draw(0, vertices.size() / pipeline.getVertexFormat().getVertexSize());
            }

        }
    }

    public void buildMesh(List<Bounds> selections){
        if (wallBuiltBuffer != null) {
            wallBuiltBuffer.close();
            wallBuiltBuffer = null;
        }
        if (gridVertices != null) {
            gridVertices.close();
            gridVertices = null;
        }

        HashSet<Wall> walls = new HashSet<>();

        // find boundaries
        for (Bounds selection : selections) {
            Wall.createWalls(walls, selection, new Vector4f(1f,0,0,0.25f), new Vector4f(0,0,0,1));
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
            wall.addWallsToBuffer(builder);
        }
        wallBuiltBuffer = builder.end();

        if (null != wallVertices) wallVertices.close();
        wallVertices = RenderSystem.getDevice().createBuffer(() -> "Walls", GpuBuffer.USAGE_VERTEX, wallBuiltBuffer.getBuffer());

        builder = new BufferBuilder(allocator, GRID.getVertexFormatMode(), GRID.getVertexFormat());
        for (Wall wall : walls) {
            wall.addGridToBuffer(builder);
        }
        BuiltBuffer grid = builder.end();

        if (null != gridVertices) gridVertices.close();

        gridVertices = RenderSystem.getDevice().createBuffer(() -> "Grid", GpuBuffer.USAGE_VERTEX, grid.getBuffer());
    }

    @Override
    public void close() throws Exception {
        if (wallBuiltBuffer != null) wallBuiltBuffer.close();
        if (wallVertices != null) wallVertices.close();
        if (gridVertices != null) gridVertices.close();
    }
}
