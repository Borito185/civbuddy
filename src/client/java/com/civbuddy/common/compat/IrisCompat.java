package com.civbuddy.common.compat;

import com.civbuddy.common.render.ShapeRenderer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

import static com.civbuddy.common.render.RenderLayers.LINES_PIPELINE;
import static com.civbuddy.common.render.RenderLayers.TRANSLUCENT_QUADS_PIPELINE;
import static com.civbuddy.common.render.RenderLayers.SEE_THROUGH_QUADS_PIPELINE;

public class IrisCompat {
    private static boolean isLoaded = false;
    public static void initialize() {
        if (isLoaded) {
            return;
        }

        isLoaded = true;

        ShapeRenderer.grid_alpha = 0.4f;

        // Assign custom render pipelines to iris programs here
        IrisApi.getInstance().assignPipeline(TRANSLUCENT_QUADS_PIPELINE, IrisProgram.BASIC);
        IrisApi.getInstance().assignPipeline(SEE_THROUGH_QUADS_PIPELINE, IrisProgram.BASIC);
        IrisApi.getInstance().assignPipeline(LINES_PIPELINE, IrisProgram.LINES);
    }
}
