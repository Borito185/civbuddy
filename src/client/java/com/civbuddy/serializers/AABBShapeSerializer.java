package com.civbuddy.serializers;

import com.civbuddy.utils.JsonFileHelper;
import com.civbuddy.veins.geo.AABBShape;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.io.IOException;

public class AABBShapeSerializer extends TypeAdapter<AABBShape> {
    @Override
    public void write(JsonWriter out, AABBShape v) throws IOException {
        out.beginArray();
        JsonFileHelper.GSON.toJson(v.center(), Vector3i.class, out);
        JsonFileHelper.GSON.toJson(v.radius(), Vector3i.class, out);
        JsonFileHelper.GSON.toJson(v.color(), Vector4f.class, out);
        out.value(v.hasGrid());
        out.endArray();
    }

    @Override
    public AABBShape read(JsonReader in) throws IOException {
        in.beginArray();
        Vector3i c = JsonFileHelper.GSON.fromJson(in, Vector3i.class);
        Vector3i r = JsonFileHelper.GSON.fromJson(in, Vector3i.class);
        Vector4f color = JsonFileHelper.GSON.fromJson(in, Vector4f.class);
        boolean grid = in.nextBoolean();
        in.endArray();

        return new AABBShape(c, r, color, grid);
    }
}
