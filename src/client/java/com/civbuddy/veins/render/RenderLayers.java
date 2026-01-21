package com.civbuddy.veins.render;

import com.civbuddy.CivBuddyClient;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalDouble;

public final class RenderLayers {
    private RenderLayers() {}
    public static final RenderPipeline TRANSLUCENT_QUADS_PIPELINE = RenderPipeline
            .builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS) // only need a position and color per vertex
            .withLocation(ResourceLocation.fromNamespaceAndPath(CivBuddyClient.MODID,"pipeline/translucent_quads"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false) // best to keep this turned off unless we swap to proper sorting
            .withCull(false) // render both sides of quads
            .build();
    public static final RenderPipeline LINES_PIPELINE = RenderPipeline
            .builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
            .withLocation(ResourceLocation.fromNamespaceAndPath(CivBuddyClient.MODID,"pipeline/lines"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .build();


    public static final RenderType TRANSLUCENT_QUADS = RenderType.create(
            "civbuddy_translucent_quads", 2048, false, true,
            TRANSLUCENT_QUADS_PIPELINE,
            RenderType.CompositeState.builder()
                    .setLayeringState(RenderStateShard.LayeringStateShard.VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false));
    public static final RenderType LINES = RenderType.create(
            "civbuddy_lines", 8192, false, false,
            LINES_PIPELINE,
            RenderType.CompositeState.builder()
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(1)))
                    .createCompositeState(false));
}
