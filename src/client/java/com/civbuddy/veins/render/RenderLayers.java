package com.civbuddy.veins.render;

import com.civbuddy.CivBuddyClient;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.util.OptionalDouble;

public final class RenderLayers {
    private RenderLayers() {}
    public static final RenderLayer TRANSLUCENT_QUADS = RenderLayer.of(
            "civbuddy_translucent_quads", 2048, false, true,
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS) // only need a position and color per vertex
                    .withLocation(Identifier.of(CivBuddyClient.MODID,"pipeline/translucent_quads"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false) // best to keep this turned off unless we swap to proper sorting
                    .withCull(false) // render both sides of quads
                    .build(),
            RenderLayer.MultiPhaseParameters.builder()
                    .layering(RenderPhase.Layering.VIEW_OFFSET_Z_LAYERING)
                    .build(false));
    public static final RenderLayer LINES = RenderLayer.of(
            "civbuddy_lines", 8192, false, false,
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                    .withLocation(Identifier.of(CivBuddyClient.MODID,"pipeline/lines"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build(),
            RenderLayer.MultiPhaseParameters.builder()
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(0.2)))
                    .build(false));
}
