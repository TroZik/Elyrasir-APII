package fr.elyrasirapii.road.server;

import com.google.gson.*;
import net.minecraft.core.BlockPos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RoadJsonIO {
    private RoadJsonIO() {}

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .setPrettyPrinting()
            .create();

    public static RoadJsonModel.RouteFile loadOrNew(Path file) {
        try {
            if (Files.exists(file)) {
                try (BufferedReader r = Files.newBufferedReader(file)) {
                    RoadJsonModel.RouteFile data = GSON.fromJson(r, RoadJsonModel.RouteFile.class);
                    return data != null ? data : new RoadJsonModel.RouteFile();
                }
            }
        } catch (IOException ignored) {}
        return new RoadJsonModel.RouteFile();
    }

    public static void save(Path file, RoadJsonModel.RouteFile data) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            GSON.toJson(data, w);
        }
    }

    /* ==== Gson adapter BlockPos en [x,y,z] OU {x,y,z} ==== */
    private static final class BlockPosAdapter implements JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {
        @Override
        public JsonElement serialize(BlockPos src, Type typeOfSrc, JsonSerializationContext ctx) {
            JsonArray a = new JsonArray();
            a.add(src.getX()); a.add(src.getY()); a.add(src.getZ());
            return a;
        }
        @Override
        public BlockPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            if (json.isJsonArray()) {
                JsonArray a = json.getAsJsonArray();
                if (a.size() < 3) throw new JsonParseException("BlockPos array size < 3");
                return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
            } else if (json.isJsonObject()) {
                JsonObject o = json.getAsJsonObject();
                return new BlockPos(
                        o.get("x").getAsInt(),
                        o.get("y").getAsInt(),
                        o.get("z").getAsInt()
                );
            }
            throw new JsonParseException("Unsupported BlockPos JSON: " + json);
        }
    }
}
