package com.civbuddy.compat;

import com.civbuddy.veins.render.ShapeRenderer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

import static com.civbuddy.veins.render.RenderLayers.*;

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
        IrisApi.getInstance().assignPipeline(LINES_PIPELINE, IrisProgram.LINES);
    }
}
