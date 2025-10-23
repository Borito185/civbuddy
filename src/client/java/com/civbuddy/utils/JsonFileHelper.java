package com.civbuddy.utils;

import com.civbuddy.Save;
import com.civbuddy.serializers.AABBShapeSerializer;
import com.civbuddy.serializers.Vector3iSerializer;
import com.civbuddy.serializers.Vector4fSerializer;
import com.civbuddy.veins.geo.AABBShape;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.io.*;

public class JsonFileHelper {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Vector3i.class, new Vector3iSerializer())
            .registerTypeAdapter(Vector4f.class, new Vector4fSerializer())
            .registerTypeAdapter(AABBShape.class, new AABBShapeSerializer())
            .create();

    public static <T> void save(File file, T data) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        try (Writer w = new BufferedWriter(new FileWriter(file))) {
            GSON.toJson(data, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T load(File file, Class<T> type) {
        if (file == null || !file.exists()) return null;

        try (Reader r = new FileReader(file)) {
            return GSON.fromJson(r, type);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
