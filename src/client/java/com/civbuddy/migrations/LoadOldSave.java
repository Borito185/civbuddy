package com.civbuddy.migrations;

import com.civbuddy.serializers.Vector3iSerializer;
import com.civbuddy.serializers.Vector4fSerializer;
import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinRow;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.civbuddy.veins.geo.AABBShape;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.civbuddy.serializers.GSONSerializer.GSON;

public class LoadOldSave {
    public static class Data {
        public HashSet<AABBShapeOld> selections = new HashSet<>();

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

    public record AABBShapeOld(Vector3i center, Vector3i radius, Vector4f color, boolean hasGrid) {}

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Vector3i.class, new Vector3iSerializer())
            .registerTypeAdapter(Vector4f.class, new Vector4fSerializer())
            .registerTypeAdapter(AABBShapeOld.class, new AABBShapeOldSerializer())
            .setPrettyPrinting()
            .create();

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

        return new File(client.runDirectory, "data/civbuddy/" + key + ".gson");
    }

    private static Data load(MinecraftClient client) {
        var file = getSaveFile(client);
        var data = new Data();

        if (file.exists()) {
            try (Reader r = new FileReader(file)) {
                data = GSON.fromJson(r, Data.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return data;
    }

    public static void migrate() {
        try {
            File saveFile = getSaveFile(MinecraftClient.getInstance());
            if (saveFile.exists()) {
                Data data = load(MinecraftClient.getInstance());

                for (VeinCounterData value : data.veins.values()) {
                    VeinDao.upsert(new VeinRow(value.key, value.count));
                }
                long id = VeinDao.getOrCreateId("default");
                for (AABBShapeOld selection : data.selections) {
                    VeinMarkingDao.upsert(new VeinMarkingRow(id, selection.center(), selection.radius()));
                }

                saveFile.delete();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
