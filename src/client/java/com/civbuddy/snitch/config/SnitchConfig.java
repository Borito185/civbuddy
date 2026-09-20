package com.civbuddy.snitch.config;

import com.civbuddy.common.storage.config.RendererConfig;
import org.joml.Vector4f;

import java.util.Map;

public class SnitchConfig {
    public boolean enabled = true;
    public RendererConfig highlight = new RendererConfig(new Vector4f(0.15f, 0.45f, 0.55f, 0.5f),false, true);

    public Vector4f breakColor = new Vector4f(1.00f, 0.23f, 0.19f, 0.50f);
    public Vector4f placeColor = new Vector4f(1.00f, 0.84f, 0.04f, 0.50f);
    public Vector4f killedColor = new Vector4f(0.56f, 0.00f, 0.19f, 0.50f);
    public Vector4f openedColor = new Vector4f(0.69f, 0.32f, 0.87f, 0.50f);
    public Vector4f enterColor = new Vector4f(0.00f, 0.72f, 0.66f, 0.50f);
    public Vector4f loginColor = new Vector4f(0.20f, 0.85f, 0.35f, 0.50f);
    public Vector4f leaveColor = new Vector4f(0.04f, 0.52f, 1.00f, 0.50f);
    public Vector4f logoutColor = new Vector4f(1.00f, 0.45f, 0.10f, 0.50f);
    public Vector4f itemExchangeColor = new Vector4f(0.20f, 0.78f, 0.35f, 0.50f);

    public Map<String, Integer> eventColors() {
        return Map.ofEntries(
                Map.entry("Break ", toArgb(breakColor)),
                Map.entry("Place ", toArgb(placeColor)),
                Map.entry("Killed ", toArgb(killedColor)),
                Map.entry("Opened ", toArgb(openedColor)),
                Map.entry("Enter ", toArgb(enterColor)),
                Map.entry("Login ", toArgb(loginColor)),
                Map.entry("Leave ", toArgb(leaveColor)),
                Map.entry("Logout ", toArgb(logoutColor)),
                Map.entry("ItemExchange ", toArgb(itemExchangeColor))
        );
    }

    private static int toArgb(Vector4f color) {
        int r = Math.round(color.x * 255);
        int g = Math.round(color.y * 255);
        int b = Math.round(color.z * 255);
        int a = Math.round(color.w * 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
