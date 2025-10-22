package com.civbuddy.veins;

import com.civbuddy.veins.geo.Edge;
import com.civbuddy.veins.geo.Face;
import com.civbuddy.veins.geo.ShapeUtils;
import com.civbuddy.veins.geo.VoxelShape;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.util.*;

public class SimpleRenderer implements AutoCloseable {
    private final RenderPipeline WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("civbuddy", "walls_pipeline"))
            .withVertexShader(Identifier.of("civbuddy", "vertex"))
            .withFragmentShader(Identifier.of("civbuddy", "fragment"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthBias(-1.0f, -0.001f) // ensures it draws over blocks
            .withDepthWrite(false) // hides clouds cus the look weird
            .withCull(false) // shows the backface
            .build());

    private final RenderPipeline GRID = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("civbuddy", "grid_pipeline"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.DEBUG_LINES)
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
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onRender);
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
    private Vector3f lastCameraPos;

    private GpuBuffer getWallIndices(WorldRenderContext ctx) {
        // sort quads for correct translucency
        Vector3f cameraPos = ctx.camera().getPos().toVector3f();
        if (cameraPos.floor() == lastCameraPos) return wallIndices.getBlocking();

        lastCameraPos = cameraPos.floor();
        ByteBuffer buffer = wallSortState.sortAndStore(allocator, VertexSorter.byDistance(cameraPos)).getBuffer();
        int size = buffer.remaining();

        if (wallIndices == null || wallIndices.size() < size) {
            wallIndices = new MappableRingBuffer(() -> "civbuddy wall indices", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }

        // send to gpu
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = encoder.mapBuffer(wallIndices.getBlocking().slice(0, buffer.remaining()), false, true)) {
            MemoryUtil.memCopy(buffer, mappedView.data());
        }

        return wallIndices.getBlocking();
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
                .createRenderPass(() -> "civbuddy walls", fb.getColorAttachmentView(), OptionalInt.empty(), fb.getDepthAttachmentView(), OptionalDouble.empty())) {
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

    public void draw(Collection<VoxelShape> shapes) {
        clear();

        Collection<Face> faces = shapes.stream().flatMap(s -> s.getFaces().stream()).toList();
        HashSet<Edge> edges = new HashSet<>();
        ShapeUtils.generateEdges(edges, faces);

        // build the walls
        if (drawWalls && !faces.isEmpty()) {
            BufferBuilder wallBuilder = Tessellator.getInstance().begin(WALLS.getVertexFormatMode(), WALLS.getVertexFormat());
            for (Face face : faces) {
                Vector4f color = face.color();
                wallBuilder.vertex(face.a()).color(color.x, color.y, color.z, color.w);
                wallBuilder.vertex(face.b()).color(color.x, color.y, color.z, color.w);
                wallBuilder.vertex(face.c()).color(color.x, color.y, color.z, color.w);
                wallBuilder.vertex(face.d()).color(color.x, color.y, color.z, color.w);
            }

            BuiltBuffer wallBuffer = wallBuilder.endNullable(); // save wall buffer to create indices later
            if (wallBuffer != null && wallBuffer.getDrawParameters().vertexCount() > 0) {
                BuiltBuffer.DrawParameters dp = wallBuffer.getDrawParameters();
                wallVertices = RenderSystem.getDevice().createBuffer(() -> "Walls", GpuBuffer.USAGE_VERTEX, wallBuffer.getBuffer());

                wallSortState = new BuiltBuffer.SortState(faces.stream().map(Face::center).toArray(Vector3f[]::new), dp.indexType());
            }

            if (wallBuffer != null) wallBuffer.close();
        }
        if (drawGrid && !edges.isEmpty()) {
            BufferBuilder gridBuilder = Tessellator.getInstance().begin(GRID.getVertexFormatMode(), GRID.getVertexFormat());
            for (Edge edge : edges) {
                gridBuilder.vertex(edge.a());
                gridBuilder.vertex(edge.b());
            }

            BuiltBuffer gridBuffer = gridBuilder.endNullable();

            if (gridBuffer != null && gridBuffer.getDrawParameters().vertexCount() > 0)
                gridVertices = RenderSystem.getDevice().createBuffer(() -> "Grid", GpuBuffer.USAGE_VERTEX, gridBuffer.getBuffer());

            if (gridBuffer != null) gridBuffer.close();
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
