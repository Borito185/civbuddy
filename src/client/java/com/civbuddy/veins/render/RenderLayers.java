package com.civbuddy.veins.render;

import com.civbuddy.CivBuddyClient;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;

public final class RenderLayers {
    private RenderLayers() {}
    public static final RenderPipeline TRANSLUCENT_QUADS_PIPELINE = RenderPipeline
            .builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS) // only need a position and color per vertex
            .withLocation(Identifier.fromNamespaceAndPath(CivBuddyClient.MODID,"pipeline/translucent_quads"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false) // best to keep this turned off unless we swap to proper sorting
            .withCull(false) // render both sides of quads
            .build();
    public static final RenderPipeline LINES_PIPELINE = RenderPipeline
            .builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
            .withLocation(Identifier.fromNamespaceAndPath(CivBuddyClient.MODID,"pipeline/lines"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .build();


    public static final RenderType TRANSLUCENT_QUADS = RenderType.create("civbuddy_translucent_quads", RenderSetup
            .builder(TRANSLUCENT_QUADS_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup());
    public static final RenderType LINES = RenderType.create("civbuddy_lines", RenderSetup
            .builder(LINES_PIPELINE)
            .createRenderSetup());
}
