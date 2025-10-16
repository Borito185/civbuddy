package com.veinbuddy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector3i;
import org.joml.Vector4f;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SaveLoader {
    public static class Save {
        public HashSet<Bounds> selections = new HashSet<>();

        public Vector4f rangeWallColor = new Vector4f(1,0,0,0.2f);
        public Vector4f rangeGridColor = new Vector4f(0,0,0,1);
        public Vector4f selectionWallColor = new Vector4f(0,1,0,0.2f);
        public Vector4f selectionGridColor = new Vector4f(0);
        public Vector4f highlightWallColor = new Vector4f(0);
        public Vector4f highlightGridColor = new Vector4f(0,0,0,1);

        public Vector3i digRange = new Vector3i(5, 5, 5);
        public boolean render = true;
        
        public String countGroup = "";
        public String currentVeinKey = "";
        public Map<String, VeinCounterData> veins = new HashMap<>();
    }
    
    // Serializable version of VeinCounter
    public static class VeinCounterData {
        public String key;
        public int count;
        public long createdTime;
        public long lastUpdateTime;
        
        public VeinCounterData() {} // For GSON
        
        public VeinCounterData(String key, int count, long createdTime, long lastUpdateTime) {
            this.key = key;
            this.count = count;
            this.createdTime = createdTime;
            this.lastUpdateTime = lastUpdateTime;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void Save(File file, Save save) {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (Writer w = new FileWriter(file)) {
            GSON.toJson(save, w);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Save load(File file) {
        if (!file.exists()) return new SaveLoader.Save();
        try (Reader r = new FileReader(file)) {
            return GSON.fromJson(r, Save.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new SaveLoader.Save();
        }
    }
}
