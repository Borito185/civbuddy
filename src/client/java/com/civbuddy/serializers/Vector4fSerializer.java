package com.civbuddy.serializers;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.joml.Vector4f;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Vector4fSerializer extends TypeAdapter<Vector4f> {
    @Override
    public void write(JsonWriter out, Vector4f v) throws IOException {
        if (v == null) v = new Vector4f(0);

        out.value(Stream.of(v.x, v.y, v.z, v.w)
                .map(d -> Float.toString(d))
                .collect(Collectors.joining(" ")));
    }

    @Override
    public Vector4f read(JsonReader in) throws IOException {
        String[] parts = in.nextString().split(" ");
        if (parts.length != 4) throw new IOException("Invalid Vector4f format");
        float x = Float.parseFloat(parts[0]);
        float y = Float.parseFloat(parts[1]);
        float z = Float.parseFloat(parts[2]);
        float w = Float.parseFloat(parts[3]);
        return new Vector4f(x, y, z, w);
    }
}
