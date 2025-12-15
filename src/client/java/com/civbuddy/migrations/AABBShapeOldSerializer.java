package com.civbuddy.migrations;

import com.civbuddy.serializers.GSONSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.io.IOException;

public class AABBShapeOldSerializer extends TypeAdapter<LoadOldSave.AABBShapeOld> {
    @Override
    public void write(JsonWriter out, LoadOldSave.AABBShapeOld v) throws IOException {
        out.beginArray();
        GSONSerializer.GSON.toJson(v.center(), Vector3i.class, out);
        GSONSerializer.GSON.toJson(v.radius(), Vector3i.class, out);
        GSONSerializer.GSON.toJson(v.color(), Vector4f.class, out);
        out.value(v.hasGrid());
        out.endArray();
    }

    @Override
    public LoadOldSave.AABBShapeOld read(JsonReader in) throws IOException {
        in.beginArray();
        Vector3i c = GSONSerializer.GSON.fromJson(in, Vector3i.class);
        Vector3i r = GSONSerializer.GSON.fromJson(in, Vector3i.class);
        Vector4f color = GSONSerializer.GSON.fromJson(in, Vector4f.class);
        boolean grid = in.nextBoolean();
        in.endArray();

        return new LoadOldSave.AABBShapeOld(c, r, color, grid);
    }
}
