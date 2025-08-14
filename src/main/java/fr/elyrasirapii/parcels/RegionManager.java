package fr.elyrasirapii.parcels;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
//import fr.elyrasirapii.client.mode.ArchitectModeManager;
import fr.elyrasirapii.parcels.selection.RegionEditMode;
import fr.elyrasirapii.parcels.utils.PolygonUtils;
import fr.elyrasirapii.server.regions.RegionPathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegionManager {

    private static final RegionManager INSTANCE = new RegionManager();
    public static RegionManager get() { return INSTANCE; }
    private RegionManager() {}

    public boolean finalizeRegionFromClient(ServerPlayer player,
                                            RegionEditMode mode,
                                            String name,
                                            List<BlockPos> points) {
        if (player == null) return false;
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        // Vérifs de base (server-side)
        if (points == null || points.size() < 3) {
            player.displayClientMessage(Component.literal("§cSélection invalide (min 3 points)."), true);
            return false;
        }
        if (!PolygonUtils.isValidPolygon(points)) {
            player.displayClientMessage(Component.literal("§cForme invalide (auto-intersections ou aire nulle)."), true);
            return false;
        }

        try {
            switch (mode) {
                case TERRITORY -> {
                    // Crée le dossier et écrit <territory>/<territory>.json
                    File tDir = RegionPathHelper.territoryDir(server, name);
                    tDir.mkdirs();
                    writeSingleRegionFile(RegionPathHelper.territoryJson(tDir), name, points);
                    player.displayClientMessage(Component.literal("§aTerritoire '" + name + "' enregistré."), true);
                }
                case CITY -> {
                    // Trouver territoire conteneur
                    Optional<String> terr = RegionPathHelper.findContainingTerritory(
                            server, PolygonUtils::containsPolygonAllowTouch, points);

                    if (terr.isEmpty()) {
                        player.displayClientMessage(Component.literal("§cAucun territoire ne contient entièrement cette ville."), true);
                        return false;
                    }

                    File cDir = RegionPathHelper.cityDir(server, terr.get(), name);
                    cDir.mkdirs();
                    writeSingleRegionFile(RegionPathHelper.cityJson(cDir), name, points);
                    player.displayClientMessage(Component.literal("§aVille '" + name + "' enregistrée dans le territoire '" + terr.get() + "'."), true);
                }
                case DISTRICT -> {
                    // Trouver (territoire, ville) conteneur
                    Optional<RegionPathHelper.CityRef> cityRef = RegionPathHelper.findContainingCity(
                            server, PolygonUtils::containsPolygonAllowTouch, points);

                    if (cityRef.isEmpty()) {
                        player.displayClientMessage(Component.literal("§cAucune ville ne contient entièrement ce district."), true);
                        return false;
                    }

                    String terrName = cityRef.get().territory();
                    String cityName = cityRef.get().city();

                    // Vérifier overlap avec autres districts de cette ville (contact bord autorisé)
                    List<List<BlockPos>> existingDistricts = loadExistingDistricts(server, terrName, cityName);
                    for (List<BlockPos> other : existingDistricts) {
                        if (PolygonUtils.polygonsOverlapStrict(points, other)) {
                            player.displayClientMessage(Component.literal("§cChevauchement interdit avec un autre district de la ville."), true);
                            return false;
                        }
                    }

                    File dDir = RegionPathHelper.districtDir(server, terrName, cityName, name);
                    dDir.mkdirs();
                    writeSingleRegionFile(RegionPathHelper.districtJson(dDir), name, points);

                    player.displayClientMessage(Component.literal("§aDistrict '" + name + "' enregistré dans la ville '" + cityName + "' (" + terrName + ")."), true);
                }
                default -> {
                    player.displayClientMessage(Component.literal("§cMode non pris en charge."), true);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            player.displayClientMessage(Component.literal("§cErreur pendant l’enregistrement."), true);
            return false;
        }
    }

    /* --------- Helpers I/O --------- */

    private void writeSingleRegionFile(File file, String name, List<BlockPos> vertices) throws Exception {
        file.getParentFile().mkdirs();

        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.add("vertices", toVertices(vertices));

        try (FileWriter w = new FileWriter(file, false)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        }
    }

    private JsonArray toVertices(List<BlockPos> vertices) {
        JsonArray arr = new JsonArray();
        for (BlockPos p : vertices) {
            JsonObject o = new JsonObject();
            o.addProperty("x", p.getX());
            o.addProperty("y", p.getY()); // on garde Y même si on travaille en XZ
            o.addProperty("z", p.getZ());
            arr.add(o);
        }
        return arr;
    }

    private List<List<BlockPos>> loadExistingDistricts(MinecraftServer server, String territory, String city) {
        List<List<BlockPos>> out = new ArrayList<>();
        for (String d : RegionPathHelper.listDistricts(server, territory, city)) {
            File dj = RegionPathHelper.districtJson(RegionPathHelper.districtDir(server, territory, city, d));
            RegionPathHelper.readPolygon(dj).ifPresent(out::add);
        }
        return out;
    }
}