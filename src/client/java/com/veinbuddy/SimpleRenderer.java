package com.veinbuddy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
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
    private GpuBuffer wallVertices;
    private MappableRingBuffer wallIndices;
    private GpuBuffer gridVertices;
    private BuiltBuffer.SortState wallSortState;

    public final boolean drawGrid;
    public final boolean drawWalls;

    public SimpleRenderer() {
        this(true, true);
    }

    public SimpleRenderer(boolean drawGrid, boolean drawWalls) {
        WorldRenderEvents.LAST.register(this::onRender);
        this.drawGrid = drawGrid;
        this.drawWalls = drawWalls;
    }

    private void onRender(WorldRenderContext ctx) {
        allocator.reset();

        if (gridVertices != null && drawGrid)
            draw(ctx, GRID, gridVertices, null);
        if (wallVertices != null && drawWalls)
            draw(ctx, WALLS, wallVertices, new Pair<>(getWallIndices(ctx), wallSortState.indexType()));
    }
    private GpuBuffer getWallIndices(WorldRenderContext ctx) {
        // sort quads for correct translucency
        Vec3d camera = ctx.camera().getPos();
        ByteBuffer buffer = wallSortState.sortAndStore(allocator, VertexSorter.byDistance(camera.toVector3f())).getBuffer();
        int size = buffer.remaining();

        if (wallIndices == null || wallIndices.size() < size) {
            wallIndices = new MappableRingBuffer(() -> "veinbuddy wall indices", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }

        // send to gpu
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = encoder.mapBuffer(wallIndices.getBlocking().slice(0, buffer.remaining()), false, true)) {
            MemoryUtil.memCopy(buffer, mappedView.data());
        }

        return wallIndices.getBlocking();
    }

    private static Vector3f[] collectCentroids(ByteBuffer buffer, int vertexCount, VertexFormat format) {
        int i = format.getOffset(VertexFormatElement.POSITION);
        if (i == -1) {
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");
        } else {
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            int j = format.getVertexSize() / 4;
            int k = j * 4;
            int l = vertexCount / 4;
            Vector3f[] vector3fs = new Vector3f[l];

            for(int m = 0; m < l; ++m) {
                int n = m * k + i;
                int o = n + j * 2;
                float f = floatBuffer.get(n + 0);
                float g = floatBuffer.get(n + 1);
                float h = floatBuffer.get(n + 2);
                float p = floatBuffer.get(o + 0);
                float q = floatBuffer.get(o + 1);
                float r = floatBuffer.get(o + 2);
                vector3fs[m] = new Vector3f((f + p) / 2.0F, (g + q) / 2.0F, (h + r) / 2.0F);
            }

            return vector3fs;
        }
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

    public void draw(Collection<DigShape> shapes){
        clear();
        if (shapes.isEmpty()) return;

        // build the walls
        if (drawWalls) {
            BufferBuilder wallBuilder = Tessellator.getInstance().begin(WALLS.getVertexFormatMode(), WALLS.getVertexFormat());
            for (DigShape shape : shapes) {
                shape.addWallsToBuffer(wallBuilder);
            }
            BuiltBuffer wallBuffer = wallBuilder.endNullable(); // save wall buffer to create indices later
            if (wallBuffer != null && wallBuffer.getDrawParameters().vertexCount() > 0) {
                BuiltBuffer.DrawParameters dp = wallBuffer.getDrawParameters();
                wallVertices = RenderSystem.getDevice().createBuffer(() -> "Walls", GpuBuffer.USAGE_VERTEX, wallBuffer.getBuffer());
                wallSortState = new BuiltBuffer.SortState(collectCentroids(wallBuffer.getBuffer(), dp.vertexCount(), WALLS.getVertexFormat()), dp.indexType());
            }
        }
        if (drawGrid) {
            BufferBuilder gridBuilder = Tessellator.getInstance().begin(GRID.getVertexFormatMode(), GRID.getVertexFormat());
            for (DigShape shape : shapes) {
                shape.addGridToBuffer(gridBuilder);
            }
            BuiltBuffer gridBuffer = gridBuilder.endNullable();

            if (gridBuffer != null && gridBuffer.getDrawParameters().vertexCount() > 0)
                gridVertices = RenderSystem.getDevice().createBuffer(() -> "Grid", GpuBuffer.USAGE_VERTEX, gridBuffer.getBuffer());
        }
    }

    public void clear() {
        if (wallVertices != null) wallVertices.close();
        if (wallIndices != null) wallIndices.close();
        if (gridVertices != null) gridVertices.close();
        allocator.reset();
        wallSortState = null;
        wallVertices = null;
        wallIndices = null;
        gridVertices = null;
    }

    @Override
    public void close() throws Exception {
        clear();
    }
}
