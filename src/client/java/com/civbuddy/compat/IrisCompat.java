package com.civbuddy.compat;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

import static com.civbuddy.veins.render.RenderLayers.LINES;
import static com.civbuddy.veins.render.RenderLayers.TRANSLUCENT_QUADS;

public class IrisCompat {
    private static boolean isLoaded = false;
    public static void initialize() {
        if (isLoaded) {
            return;
        }

        isLoaded = true;

        // Assign custom render pipelines to iris programs here
        IrisApi.getInstance().assignPipeline(TRANSLUCENT_QUADS.iris$getPipeline(), IrisProgram.BASIC);
        IrisApi.getInstance().assignPipeline(LINES.iris$getPipeline(), IrisProgram.LINES);
    }
}
