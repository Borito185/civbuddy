package com.civbuddy.veins.config;

import com.civbuddy.common.storage.config.RendererConfig;
import org.joml.Vector3i;
import org.joml.Vector4f;

public final class VeinConfig {
    public boolean doRender = true;
    public ShapeMode shapeMode = ShapeMode.Cuboid;
    public int borderThreshold = 1;

    public float placeMoveSpeed = 0.2f;
    public float placeRange = 6.0f;
    public int placeDelayTicks = 5;
    public Vector3i markRange = new Vector3i(5,5,5);

    public RendererConfig border = new RendererConfig(new Vector4f(1,0,0,0.2f), true, false);
    public RendererConfig marking = new RendererConfig(new Vector4f(0,1,0,0.2f), true, false);
    public RendererConfig highlight = new RendererConfig(new Vector4f(.6f, .6f, .6f, .2f), true, false);

    public int getMaxTicks() {
        return (int) (placeRange / placeMoveSpeed);
    }

    public enum ShapeMode {
        Cuboid,
        Spheroid
    }
}
