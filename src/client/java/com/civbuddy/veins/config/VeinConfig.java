package com.civbuddy.veins.config;

import org.joml.Vector3i;
import org.joml.Vector4f;

public final class VeinConfig {
    public boolean doRender = true;

    public float placeMoveSpeed = 0.2f;
    public float placeRange = 6.0f;
    public int placeDelayTicks = 5;
    public Vector3i markRange = new Vector3i(5,5,5);

    public Vector4f borderWallColor = new Vector4f(1,0,0,0.2f);
    public boolean borderHasGrid = true;
    public Vector4f markingWallColor = new Vector4f(0,1,0,0.2f);
    public boolean markingHasGrid = true;
    public Vector4f highlightWallColor = new Vector4f(0);
    public boolean highlightHasGrid = true;

    public int getMaxTicks() {
        return (int) (placeRange / placeMoveSpeed);
    }
}
