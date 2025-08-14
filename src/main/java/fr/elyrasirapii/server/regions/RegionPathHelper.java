package fr.elyrasirapii.server.regions;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class RegionPathHelper {

    private RegionPathHelper() {}

    /* ---------- Chemins ---------- */

    public static Path baseDir(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD)
                .getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve("elyrasir");
    }

    public static File territoryDir(MinecraftServer server, String territoryName) {
        return baseDir(server).resolve(sanitize(territoryName)).toFile();
    }

    public static File territoryJson(File territoryDir) {
        // <territory>/<territory>.json
        return new File(territoryDir, territoryDir.getName() + ".json");
    }

    public static File cityDir(MinecraftServer server, String territoryName, String cityName) {
        return new File(territoryDir(server, territoryName), sanitize(cityName));
    }

    public static File cityJson(File cityDir) {
        // <territory>/<city>/<city>.json
        return new File(cityDir, cityDir.getName() + ".json");
    }

    public static File districtDir(MinecraftServer server, String territoryName, String cityName, String districtName) {
        return new File(cityDir(server, territoryName, cityName), sanitize(districtName));
    }

    public static File districtJson(File districtDir) {
        // <territory>/<city>/<district>/<district>.json
        return new File(districtDir, districtDir.getName() + ".json");
    }

    // Fichiers de parcelles (qu’on utilisera tout de suite après)
    public static File territoryParcelsFile(File territoryDir) {
        return new File(territoryDir, "Parcelles" + territoryDir.getName() + ".json");
    }

    public static File cityParcelsFile(File cityDir) {
        return new File(cityDir, "Parcelles" + cityDir.getName() + ".json");
    }

    public static File districtParcelsFile(File districtDir) {
        return new File(districtDir, "Parcelles" + districtDir.getName() + ".json");
    }

    /* ---------- Listing ---------- */

    public static List<String> listTerritories(MinecraftServer server) {
        File base = baseDir(server).toFile();
        File[] dirs = base.listFiles(File::isDirectory);
        if (dirs == null) return Collections.emptyList();
        return Arrays.stream(dirs).map(File::getName).collect(Collectors.toList());
    }

    public static List<String> listCities(MinecraftServer server, String territory) {
        File terr = territoryDir(server, territory);
        File[] dirs = terr.listFiles(File::isDirectory);
        if (dirs == null) return Collections.emptyList();
        return Arrays.stream(dirs).map(File::getName).collect(Collectors.toList());
    }

    public static List<String> listDistricts(MinecraftServer server, String territory, String city) {
        File cityFolder = cityDir(server, territory, city);
        File[] dirs = cityFolder.listFiles(File::isDirectory);
        if (dirs == null) return Collections.emptyList();
        return Arrays.stream(dirs).map(File::getName).collect(Collectors.toList());
    }

    /* ---------- Lecture JSON ---------- */

    /** Lit un polygone depuis un fichier JSON. Format accepté :
     *  { "name":"...", "vertices":[{"x":..,"y":..,"z":..}, ...] }
     *  ou bien un tableau d'objets et on prend celui dont "name" == baseName (sinon le 1er).
     */
    public static Optional<List<BlockPos>> readPolygon(File file) {
        if (!file.exists()) return Optional.empty();
        try (JsonReader r = new JsonReader(new FileReader(file))) {
            JsonElement el = JsonParser.parseReader(r);
            if (el.isJsonObject()) {
                return Optional.of(parseVertices(el.getAsJsonObject().getAsJsonArray("vertices")));
            } else if (el.isJsonArray()) {
                String baseName = file.getName().replace(".json", "");
                JsonArray arr = el.getAsJsonArray();
                JsonObject chosen = null;
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) {
                        JsonObject obj = e.getAsJsonObject();
                        if (obj.has("name") && baseName.equalsIgnoreCase(obj.get("name").getAsString())) {
                            chosen = obj; break;
                        }
                        if (chosen == null) chosen = obj; // fallback = first
                    }
                }
                if (chosen != null && chosen.has("vertices")) {
                    return Optional.of(parseVertices(chosen.getAsJsonArray("vertices")));
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private static List<BlockPos> parseVertices(JsonArray arr) {
        List<BlockPos> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            int x = o.get("x").getAsInt();
            int y = o.get("y").getAsInt();
            int z = o.get("z").getAsInt();
            out.add(new BlockPos(x, y, z));
        }
        return out;
    }

    /* ---------- Trouver le conteneur ---------- */

    /** Retourne le territoire qui contient complètement le polygone (touche-bord autorisé). */
    public static Optional<String> findContainingTerritory(MinecraftServer server,
                                                           java.util.function.BiFunction<List<BlockPos>, List<BlockPos>, Boolean> contains,
                                                           List<BlockPos> poly) {
        for (String terr : listTerritories(server)) {
            File tf = territoryJson(territoryDir(server, terr));
            Optional<List<BlockPos>> territoryPoly = readPolygon(tf);
            if (territoryPoly.isPresent() && contains.apply(territoryPoly.get(), poly)) {
                return Optional.of(terr);
            }
        }
        return Optional.empty();
    }

    /** Retourne (territory, city) qui contient complètement le polygone. */
    public static Optional<CityRef> findContainingCity(MinecraftServer server,
                                                       java.util.function.BiFunction<List<BlockPos>, List<BlockPos>, Boolean> contains,
                                                       List<BlockPos> poly) {
        for (String terr : listTerritories(server)) {
            for (String city : listCities(server, terr)) {
                File cf = cityJson(cityDir(server, terr, city));
                Optional<List<BlockPos>> cityPoly = readPolygon(cf);
                if (cityPoly.isPresent() && contains.apply(cityPoly.get(), poly)) {
                    return Optional.of(new CityRef(terr, city));
                }
            }
        }
        return Optional.empty();
    }

    public record CityRef(String territory, String city) {}

    /* ---------- Utils ---------- */

    private static String sanitize(String s) {
        // petit nettoyage pour nom de dossier/fichier
        return s.trim().replaceAll("[\\\\/:*?\"<>|]", "_").replace(' ', '_');
    }
}
