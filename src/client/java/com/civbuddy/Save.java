package com.civbuddy;

import com.civbuddy.serializers.Vector3iSerializer;
import com.civbuddy.veins.geo.AABBShape;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.civbuddy.serializers.GSONSerializer.GSON;

public class Save {
    public static class Data {
        public HashSet<AABBShape> selections = new HashSet<>();

        public Vector4f rangeWallColor = new Vector4f(1,0,0,0.2f);
        public Vector4f selectionWallColor = new Vector4f(0,1,0,0.2f);
        public Vector4f highlightWallColor = new Vector4f(0);

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

        public VeinCounterData(String s) {
            key = s;
        }
    }
    @FunctionalInterface
    public interface SaveLoaded {
        void handle(Data data);
    }
    private static File file;
    public static Data data = new Data();

    public static final Event<SaveLoaded> SAVE_LOADED = EventFactory.createArrayBacked(SaveLoaded.class, listeners -> data -> {
        for (SaveLoaded listener : listeners) {
            listener.handle(data);
        }
    });

    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> load(client));
    }

    private static void load(MinecraftClient client) {
        file = getSaveFile(client);
        data = new Data();

        if (file.exists()) {
            try (Reader r = new FileReader(file)) {
                Save.data = GSON.fromJson(r, Data.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        SAVE_LOADED.invoker().handle(data);
    }

    private static File getSaveFile(MinecraftClient client) {
        ServerInfo serverInfo = client.getCurrentServerEntry();
        IntegratedServer server = client.getServer();

        String key = null;

        if (server != null) {
            key = server.getSaveProperties().getLevelName();
        }

        if (serverInfo != null) {
            key = serverInfo.address;
        }

        if (key == null)
            return null;

        return new File(client.runDirectory, "data/veinbuddy/" + key + ".gson");
    }

    public static void save() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (Writer w = new BufferedWriter(new FileWriter(file))) {
            GSON.toJson(data, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
