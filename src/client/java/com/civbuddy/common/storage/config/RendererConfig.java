package com.civbuddy.common.storage.config;

import org.joml.Vector4f;

public class RendererConfig {
    public Vector4f color;
    public boolean grid;
    public boolean see_through;

    public RendererConfig(Vector4f color, boolean grid, boolean see_through) {
        this.color = color;
        this.grid = grid;
        this.see_through = see_through;
    }

    public RendererConfig clone() {
        return new RendererConfig(
                color, grid, see_through
        );
    }
}
