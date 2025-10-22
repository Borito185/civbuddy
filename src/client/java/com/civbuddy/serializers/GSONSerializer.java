package com.civbuddy.serializers;

import com.civbuddy.veins.geo.AABBShape;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector3i;
import org.joml.Vector4f;

public class GSONSerializer {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Vector3i.class, new Vector3iSerializer())
            .registerTypeAdapter(Vector4f.class, new Vector4fSerializer())
            .registerTypeAdapter(AABBShape.class, new AABBShapeSerializer())
            .create();
}
