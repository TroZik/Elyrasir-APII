package fr.elyrasirapii.parcels;

import com.google.gson.*;
import fr.elyrasirapii.parcels.utils.PolygonUtils;
import fr.elyrasirapii.server.utils.MinecraftServerHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Gestion centralisée des parcelles :
 * - routage District/Ville/Territoire/Hors-structure
 * - anti-chevauchement dans le scope ciblé
 * - rechargement global pour la détection (getParcelAt)
 */
public class ParcelsManager {

    private static final ParcelsManager INSTANCE = new ParcelsManager();
    public static ParcelsManager get() { return INSTANCE; }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Sélections locales (si tu en as l’usage côté client)
    private final Map<UUID, List<BlockPos>> playerSelections = new HashMap<>();

    // Cache côté client (aperçu local)
    private final List<List<BlockPos>> existingParcels = new ArrayList<>();

    // Détection d’entrée/sortie
    private final Map<UUID, String> playerParcelMap = new HashMap<>();
    private final Map<String, List<BlockPos>> parcels = new HashMap<>();

    /* ------------------------------------------------------------
     *                API de finalisation côté serveur
     * ------------------------------------------------------------ */

    /**
     * Appelée depuis le packet côté serveur pour sauvegarder proprement.
     * - Route vers le bon fichier (district > ville > territoire > hors-structure)
     * - Bloque si chevauchement avec une parcelle existante du même scope
     * - Écrit la parcelle
     */
    public void finalizeSelectionFromClient(Player playerGeneric, String name, List<BlockPos> points) {
        if (!(playerGeneric instanceof ServerPlayer player)) {
            playerGeneric.sendSystemMessage(Component.literal("§cErreur: action serveur invalide."));
            return;
        }
        if (points == null || points.size() < 3) {
            player.sendSystemMessage(Component.literal("§cVeuillez sélectionner au moins 3 points."));
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            player.sendSystemMessage(Component.literal("§cErreur : serveur indisponible."));
            return;
        }

        // 1) Résoudre le fichier cible en fonction Territoire/Ville/District
        File targetFile = resolveTargetParcelsFile(server, points);
        if (targetFile == null) {
            player.sendSystemMessage(Component.literal("§cSélection invalide (ville/district non trouvé)."));
            return;
        }

        // 2) Charger les parcelles existantes dans ce scope (AVANT écriture)
        List<List<BlockPos>> existing = readParcelsArray(targetFile);

        // 3) Anti-chevauchement (même scope)
        for (List<BlockPos> other : existing) {
            if (PolygonUtils.polygonsOverlapXZ(points, other)) {
                player.sendSystemMessage(Component.literal("§cChevauchement détecté avec une parcelle existante de ce secteur."));
                return;
            }
        }

        // 4) Sauvegarde (append) — une seule fois
        appendParcel(targetFile, name, points);

        // 5) Feedback
        player.sendSystemMessage(Component.literal("§aParcelle '" + name + "' enregistrée avec " + points.size() + " sommets."));
    }


    /* ------------------------------------------------------------
     *                Routage District / Ville / Territoire
     * ------------------------------------------------------------ */
    private File resolveTargetParcelsFile(MinecraftServer server, List<BlockPos> poly) {
        // IMPORTANT: racine en minuscule (comme ton screenshot)
        Path rootPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve("elyrasir");
        File rootDir = rootPath.toFile();
        if (!rootDir.exists()) rootDir.mkdirs();

        return resolveTargetParcelsFile(rootDir, poly);
    }


    /**
     * Choisit le fichier JSON cible pour écrire la parcelle, en respectant la hiérarchie :
     * Elyrasir/Territoires/<Territoire>/[Villes/<Ville>/[Districts/<District>/Parcelles_<District>.json]]
     * ou bien fichiers ParcellesTerritoire_*.json / ParcellesVille_*.json / ParcellesHorsTerritoire.json
     */
    private File resolveTargetParcelsFile(File rootDir, List<BlockPos> poly) {
        // ----------- NIVEAU TERRITOIRE -----------
        // ① mode “avec Territoires/”
        File terrContainer = new File(rootDir, "Territoires");
        File[] territoryDirs = (terrContainer.exists() && terrContainer.isDirectory())
                ? terrContainer.listFiles(File::isDirectory)
                : null;

        // ② fallback : territoires directement sous la racine (comme ta structure)
        if (territoryDirs == null || territoryDirs.length == 0) {
            territoryDirs = rootDir.listFiles(file ->
                    file.isDirectory() && !file.getName().equalsIgnoreCase("Territoires"));
        }
        if (territoryDirs == null) territoryDirs = new File[0];

        File matchedTerritoryDir = null;
        String matchedTerritoryName = null;

        for (File tDir : territoryDirs) {
            // On s’attend à un fichier <territoire>.json dans le dossier
            File tJson = new File(tDir, tDir.getName() + ".json");
            List<BlockPos> terrPoly = readRegionPolygon(tJson);
            if (terrPoly == null || terrPoly.size() < 3) continue;

            if (containsAllVertices(terrPoly, poly)) {
                matchedTerritoryDir = tDir;
                matchedTerritoryName = tDir.getName();
                break;
            }
        }

        if (matchedTerritoryDir == null) {
            // rien ne contient la sélection
            File hors = new File(rootDir, "ParcellesHorsTerritoire.json");
            System.out.println("[Parcelles] HorsTerritoire -> " + hors.getAbsolutePath());
            return hors;
        }

        // ----------- NIVEAU VILLE -----------
        // ① mode “avec Villes/”
        File citiesRoot = new File(matchedTerritoryDir, "Villes");
        File[] cityDirs = (citiesRoot.exists() && citiesRoot.isDirectory())
                ? citiesRoot.listFiles(File::isDirectory)
                : null;

        // ② fallback : villes directement dans le territoire (comme ta structure)
        if (cityDirs == null || cityDirs.length == 0) {
            cityDirs = matchedTerritoryDir.listFiles(file ->
                    file.isDirectory() &&
                            !file.getName().equalsIgnoreCase("Villes") &&
                            !file.getName().equalsIgnoreCase("Territoires"));
        }
        if (cityDirs == null) cityDirs = new File[0];

        File matchedCityDir = null;
        String matchedCityName = null;

        for (File cDir : cityDirs) {
            File cityJson = new File(cDir, cDir.getName() + ".json");
            List<BlockPos> cityPoly = readRegionPolygon(cityJson);
            if (cityPoly == null || cityPoly.size() < 3) continue;

            if (containsAllVertices(cityPoly, poly)) {
                matchedCityDir = cDir;
                matchedCityName = cDir.getName();
                break;
            }
        }

        if (matchedCityDir == null) {
            // pas dans une ville → fichier au niveau territoire
            File f = new File(matchedTerritoryDir, "ParcellesTerritoire_" + matchedTerritoryName + ".json");
            System.out.println("[Parcelles] Territoire -> " + f.getAbsolutePath());
            return f;
        }

        // ----------- NIVEAU DISTRICT -----------
        // ① mode “avec Districts/”
        File districtsRoot = new File(matchedCityDir, "Districts");
        File[] districtDirs = (districtsRoot.exists() && districtsRoot.isDirectory())
                ? districtsRoot.listFiles(File::isDirectory)
                : null;

        // ② fallback : districts directement dans la ville (comme ta structure)
        if (districtDirs == null || districtDirs.length == 0) {
            districtDirs = matchedCityDir.listFiles(file ->
                    file.isDirectory() &&
                            !file.getName().equalsIgnoreCase("Districts") &&
                            !file.getName().equalsIgnoreCase("Villes"));
        }
        if (districtDirs == null) districtDirs = new File[0];

        // Si la ville n’a pas/plus de sous-dossiers (donc pas de districts),
        // on écrit au niveau ville :
        if (districtDirs.length == 0) {
            File f = new File(matchedCityDir, "ParcellesVille_" + matchedCityName + ".json");
            System.out.println("[Parcelles] Ville(sans districts) -> " + f.getAbsolutePath());
            return f;
        }

        // Sinon : tenter de trouver exactement 1 district qui contient entièrement la sélection
        File matchedDistrictDir = null;
        String matchedDistrictName = null;
        int matches = 0;

        for (File dDir : districtDirs) {
            File dJson = new File(dDir, dDir.getName() + ".json"); // ex: couille1/couille1.json
            List<BlockPos> distPoly = readRegionPolygon(dJson);
            if (distPoly == null || distPoly.size() < 3) continue;

            if (containsAllVertices(distPoly, poly)) {
                matchedDistrictDir = dDir;
                matchedDistrictName = dDir.getName();
                matches++;
            }
        }

        if (matches == 1 && matchedDistrictDir != null) {
            File f = new File(matchedDistrictDir, "Parcelles_" + matchedDistrictName + ".json");
            System.out.println("[Parcelles] District -> " + f.getAbsolutePath());
            return f;
        }

        // Plusieurs ou zéro districts contiennent la sélection → invalide
        System.out.println("[Parcelles] ERREUR: Ville avec districts, sélection non strictement dans un district unique.");
        return null;
    }







    /** true si chaque sommet de 'inner' est dans ou sur le bord de 'outer' (XZ). */
    private boolean containsAllVertices(List<BlockPos> outer, List<BlockPos> inner) {
        if (outer == null || inner == null || outer.size() < 3 || inner.isEmpty()) return false;
        for (BlockPos p : inner) {
            if (!fr.elyrasirapii.parcels.utils.PolygonUtils.isPointInsidePolygon(p.getX(), p.getZ(), outer)) {
                return false;
            }
        }
        return true;
    }
    /* ------------------------------------------------------------
     *                Lecture / écriture JSON
     * ------------------------------------------------------------ */

    /** Lit un polygone de région depuis un JSON (objet {vertices:...} ou tableau [ {vertices:...}, ... ]) */
    private List<BlockPos> readRegionPolygon(File jsonFile) {
        if (jsonFile == null || !jsonFile.exists()) return null;
        try (FileReader r = new FileReader(jsonFile)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray vertices = obj.getAsJsonArray("vertices");
            if (vertices == null) return null;
            List<BlockPos> pts = new ArrayList<>();
            for (JsonElement e : vertices) {
                JsonObject v = e.getAsJsonObject();
                pts.add(new BlockPos(v.get("x").getAsInt(), 0, v.get("z").getAsInt()));
            }
            return pts;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private List<BlockPos> jsonToPoly(JsonArray verts) {
        List<BlockPos> poly = new ArrayList<>();
        for (JsonElement v : verts) {
            JsonObject o = v.getAsJsonObject();
            int x = o.get("x").getAsInt();
            int y = o.get("y").getAsInt();
            int z = o.get("z").getAsInt();
            poly.add(new BlockPos(x, y, z));
        }
        return poly;
    }

    /** Lit un tableau de parcelles [{name, vertices:[...]}] depuis file ; vide si absent */
    private List<List<BlockPos>> readParcelsArray(File file) {
        List<List<BlockPos>> out = new ArrayList<>();
        if (!file.exists()) return out;
        try (FileReader fr = new FileReader(file)) {
            JsonElement root = JsonParser.parseReader(fr);
            if (!root.isJsonArray()) return out;
            for (JsonElement e : root.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonArray verts = e.getAsJsonObject().getAsJsonArray("vertices");
                if (verts == null) continue;
                out.add(jsonToPoly(verts));
            }
        } catch (IOException ignored) {}
        return out;
    }

    private void appendParcel(File file, String name, List<BlockPos> vertices) {
        file.getParentFile().mkdirs();

        JsonArray array = new JsonArray();
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                JsonElement root = JsonParser.parseReader(fr);
                if (root.isJsonArray()) array = root.getAsJsonArray();
            } catch (IOException ignored) {}
        }

        JsonObject parcel = new JsonObject();
        parcel.addProperty("name", name);
        JsonArray verts = new JsonArray();
        for (BlockPos p : vertices) {
            JsonObject o = new JsonObject();
            o.addProperty("x", p.getX());
            o.addProperty("y", p.getY());
            o.addProperty("z", p.getZ());
            verts.add(o);
        }
        parcel.add("vertices", verts);
        array.add(parcel);

        try (FileWriter fw = new FileWriter(file, false)) {
            GSON.toJson(array, fw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Retourne true si un dossier possède au moins un sous-dossier dont le nom commence par prefix. */
    private boolean hasChildrenDir(File parent, String prefix) {
        File[] arr = parent.listFiles((dir, name) -> new File(dir, name).isDirectory() && name.startsWith(prefix));
        return arr != null && arr.length > 0;
    }


    /* ------------------------------------------------------------
     *                Anciennes APIs (inchangées côté signature)
     *
     * ------------------------------------------------------------ */

    @OnlyIn(Dist.CLIENT)
    public void openNamingScreen(Player player) {
        Minecraft.getInstance().setScreen(new fr.elyrasirapii.client.gui.NameParcelScreen());
    }

    @OnlyIn(Dist.CLIENT)
    public BlockPos[] getSelection(Player player) {
        List<BlockPos> list = playerSelections.get(player.getUUID());
        if (list == null || list.size() != 2) return new BlockPos[0];
        return list.toArray(new BlockPos[0]);
    }

    @OnlyIn(Dist.CLIENT)
    public List<BlockPos> getFullSelection(Player player) {
        return new ArrayList<>(playerSelections.getOrDefault(player.getUUID(), new ArrayList<>()));
    }

    @OnlyIn(Dist.CLIENT)
    public List<List<BlockPos>> getVisibleParcels(Level level, BlockPos center, int radius) {
        return existingParcels.stream()
                .filter(poly -> poly.stream().anyMatch(pos -> pos.closerThan(center, radius)))
                .toList();
    }

    @OnlyIn(Dist.CLIENT)
    public List<BlockPos[]> getNearbyParcels(Player player, int radius) {
        // (Laisse tel quel si tu t’en sers côté client pour l’aperçu local)
        List<BlockPos[]> nearby = new ArrayList<>();
        MinecraftServer server = player.getServer();
        if (server == null) return nearby;

        // on scanne désormais tous les Parcelles*.json
        for (File f : listAllParcelsFiles(server)) {
            try (FileReader reader = new FileReader(f)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonArray()) continue;

                for (JsonElement e : element.getAsJsonArray()) {
                    JsonObject obj = e.getAsJsonObject();
                    JsonArray verticesJson = obj.getAsJsonArray("vertices");
                    if (verticesJson == null || verticesJson.size() < 2) continue;

                    List<BlockPos> vertexList = new ArrayList<>();
                    for (JsonElement vert : verticesJson) {
                        JsonObject v = vert.getAsJsonObject();
                        vertexList.add(new BlockPos(v.get("x").getAsInt(), v.get("y").getAsInt(), v.get("z").getAsInt()));
                    }

                    BlockPos center = getPolygonCenter(vertexList);
                    if (center.closerThan(player.blockPosition(), radius)) {
                        nearby.add(vertexList.toArray(new BlockPos[0]));
                    }
                }
            } catch (IOException ignored) {}
        }
        return nearby;
    }

    private BlockPos getPolygonCenter(List<BlockPos> points) {
        int x = 0, y = 0, z = 0;
        for (BlockPos p : points) { x += p.getX(); y += p.getY(); z += p.getZ(); }
        int n = Math.max(1, points.size());
        return new BlockPos(x / n, y / n, z / n);
    }

    /* ---------------- Détection d’appartenance à une parcelle ---------------- */

    public String getParcelAt(BlockPos pos) {
        reloadParcels(); // scan dynamique
        int px = pos.getX();
        int pz = pos.getZ();

        for (Map.Entry<String, List<BlockPos>> entry : parcels.entrySet()) {
            String name = entry.getKey();
            List<BlockPos> polygon = entry.getValue();
            boolean inside = PolygonUtils.isPointInsidePolygon(px, pz, polygon);
            if (inside) return name;
        }
        return null;
    }

    public Map<UUID, String> getPlayerParcelMap() { return playerParcelMap; }
    public Map<String, List<BlockPos>> getParcels() { return parcels; }

    public void reloadParcels() {
        parcels.clear();
        MinecraftServer server = MinecraftServerHolder.INSTANCE;
        if (server == null) return;

        // 👉 nouveau : on charge TOUTES les parcelles de l’arbo
        for (File f : listAllParcelsFiles(server)) {
            try (BufferedReader reader = Files.newBufferedReader(f.toPath())) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonArray()) continue;

                for (JsonElement element : root.getAsJsonArray()) {
                    JsonObject obj = element.getAsJsonObject();
                    String name = obj.has("name") ? obj.get("name").getAsString() : f.getName();
                    List<BlockPos> points = new ArrayList<>();
                    JsonArray verts = obj.getAsJsonArray("vertices");
                    if (verts == null) continue;
                    for (JsonElement v : verts) {
                        JsonObject p = v.getAsJsonObject();
                        int x = p.get("x").getAsInt();
                        int y = p.get("y").getAsInt();
                        int z = p.get("z").getAsInt();
                        points.add(new BlockPos(x, y, z));
                    }
                    if (points.size() >= 3) {
                        // clé unique : nom + chemin relatif (évite collisions de noms)
                        String key = name + " @ " + rootRelative(server, f);
                        parcels.put(key, points);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<File> listAllParcelsFiles(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig").resolve("elyrasir");
        List<File> out = new ArrayList<>();
        if (!Files.exists(root)) return out;

        try {
            Files.walk(root)
                    .filter(p -> p.getFileName().toString().startsWith("Parcelles") &&
                            p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> out.add(p.toFile()));
        } catch (IOException ignored) {}
        return out;
    }

    private String rootRelative(MinecraftServer server, File f) {
        Path root = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig").resolve("elyrasir");
        try {
            return root.relativize(f.toPath()).toString().replace('\\','/');
        } catch (IllegalArgumentException e) {
            return f.getName();
        }
    }

    /* ------------------------------------------------------------
     *              (Anciennes méthodes utilitaires)
     * ------------------------------------------------------------ */

    private JsonObject toJson(BlockPos pos) {
        JsonObject json = new JsonObject();
        json.addProperty("x", pos.getX());
        json.addProperty("y", pos.getY());
        json.addProperty("z", pos.getZ());
        return json;
    }

    // Gardée, mais plus utilisée pour l’écriture (on route désormais).
    private void saveParcel(MinecraftServer server, String name, List<BlockPos> vertices) {
        // Fallback : ancien unique fichier (toujours fonctionnel si tu l’appelles encore)
        Path configPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve("land_parcels.json");

        File file = configPath.toFile();
        file.getParentFile().mkdirs();

        JsonObject parcel = new JsonObject();
        parcel.addProperty("name", name);

        JsonArray vertexArray = new JsonArray();
        for (BlockPos vertex : vertices) {
            vertexArray.add(toJson(vertex));
        }
        parcel.add("vertices", vertexArray);

        JsonArray array = new JsonArray();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonElement existing = JsonParser.parseReader(reader);
                if (existing.isJsonArray()) array = existing.getAsJsonArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        array.add(parcel);

        try (FileWriter writer = new FileWriter(file, false)) {
            GSON.toJson(array, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

