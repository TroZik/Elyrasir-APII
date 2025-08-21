package fr.elyrasirapii.road.server;

import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.road.server.RoadJsonModel.RouteFile;
import fr.elyrasirapii.road.server.RoadJsonModel.RouteEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Charge et met en cache les segments de routes par territoire pour lookup rapide. */
public final class RoadRuntimeIndex {

    public record Segment(String routeName, RoadType type, BlockPos a, BlockPos b) {}

    private static final Map<String, List<Segment>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> SIGNATURES = new ConcurrentHashMap<>();

    private RoadRuntimeIndex() {}

    /** Récupère (ou charge) tous les segments du territoire. Auto-reload si fichiers changés. */
    public static List<Segment> getSegmentsFor(MinecraftServer server, String territory) {
        if (territory == null) return List.of();

        long sig = signature(server, territory);
        Long prev = SIGNATURES.get(territory);
        if (prev == null || prev != sig) {
            // fichiers modifiés → recharger
            List<Segment> fresh = loadTerritory(server, territory);
            CACHE.put(territory, fresh);
            SIGNATURES.put(territory, sig);
            return fresh;
        }
        return CACHE.computeIfAbsent(territory, t -> {
            List<Segment> fresh = loadTerritory(server, t);
            SIGNATURES.put(t, signature(server, t));
            return fresh;
        });
    }

    /** Invalide le cache d'un territoire (à appeler après écriture). */
    public static void invalidate(String territory) {
        CACHE.remove(territory);
        SIGNATURES.remove(territory);
    }

    private static long signature(MinecraftServer server, String territory) {
        // somme des lastModifiedTime des 3 fichiers (absents = 0)
        long sum = 0L;
        for (RoadType type : RoadType.values()) {
            Path p = RoadIOPaths.routesFile(server, territory, type);
            try {
                if (Files.exists(p)) {
                    FileTime ft = Files.getLastModifiedTime(p);
                    sum += ft.toMillis() ^ p.toString().hashCode();
                }
            } catch (Exception ignored) {}
        }
        return sum;
    }

    private static List<Segment> loadTerritory(MinecraftServer server, String territory) {
        List<Segment> out = new ArrayList<>();

        for (RoadType type : RoadType.values()) {
            var path = RoadIOPaths.routesFile(server, territory, type);
            if (!Files.exists(path)) continue;

            RouteFile rf = RoadJsonIO.loadOrNew(path);
            if (rf == null || rf.routes == null || rf.routes.isEmpty()) continue;

            for (RouteEntry route : rf.routes) {
                List<BlockPos> pts = route.points;
                if (pts == null || pts.size() < 2) continue;

                for (int i = 0; i < pts.size() - 1; i++) {
                    out.add(new Segment(route.name, type, pts.get(i), pts.get(i + 1)));
                }
            }
        }
        return out;
    }
}

