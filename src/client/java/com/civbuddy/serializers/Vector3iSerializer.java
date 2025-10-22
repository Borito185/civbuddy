package com.civbuddy.serializers;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.joml.Vector3i;
import java.io.IOException;

public class Vector3iSerializer extends TypeAdapter<Vector3i> {
    @Override
    public void write(JsonWriter out, Vector3i v) throws IOException {
        if (v == null) v = new Vector3i(0);

        out.value(String.format("%d %d %d", v.x, v.y, v.z));
    }

    @Override
    public Vector3i read(JsonReader in) throws IOException {
        String[] parts = in.nextString().split(" ");
        if (parts.length != 3) throw new IOException("Invalid Vector3i format");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new Vector3i(x, y, z);
    }
}
