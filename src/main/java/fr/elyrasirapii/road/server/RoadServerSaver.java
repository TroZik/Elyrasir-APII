package fr.elyrasirapii.road.server;

import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.road.server.RoadJsonModel.RegionSpan;
import fr.elyrasirapii.road.server.RoadJsonModel.RouteEntry;
import fr.elyrasirapii.road.server.RoadJsonModel.RouteFile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RoadServerSaver {
    private RoadServerSaver() {}

    public static void saveRoute(MinecraftServer server, ServerPlayer sender, String name, RoadType type, List<BlockPos> points) {
        if (points == null || points.size() < 2) {
            if (sender != null) sender.displayClientMessage(Component.literal("§cRoute invalide (moins de 2 points)."), false);
            return;
        }

        ServerLevel level = sender.serverLevel();// monde courant
        // Territoire: déterminé sur le 1er point
        String territory = RoadRegionResolver.findTerritory(level, points.get(0));

        // Fabrique l'entrée
        RouteEntry entry = new RouteEntry();
        entry.id = generateNumericId(type);
        entry.name = name;
        entry.points = new ArrayList<>(points);

        // Découpe en groupes (from..to) selon appartenance régionale (évaluée au milieu de chaque segment)
        buildRegionSpans(level, points, entry.segments);

        // Fichier cible (par type, sous le territoire)
        var file = RoadIOPaths.routesFile(server, territory, type);


        // >>> invalider le cache runtime du territoire pour forcer le rechargement
        fr.elyrasirapii.road.server.RoadRuntimeIndex.invalidate(territory);

        // Charge, append, save
        RouteFile all = RoadJsonIO.loadOrNew(file);
        all.routes.add(entry);
        try {
            RoadJsonIO.save(file, all);
            if (sender != null) {
                sender.displayClientMessage(Component.literal(
                        "§aRoute enregistrée: " + name + " (#" + entry.id + ") → " + territory), false);
            }
        } catch (IOException e) {
            if (sender != null) {
                sender.displayClientMessage(Component.literal("§cErreur sauvegarde route: " + e.getMessage()), false);
            }
            e.printStackTrace();
        }
    }

    /** ID numérique: T + epochSeconds(10) + rand(3), ex: 1169901234507 */
    private static String generateNumericId(RoadType type) {
        int t = switch (type) { case RUE -> 1; case AVENUE -> 2; case BOULEVARD -> 3; };
        long epochSec = System.currentTimeMillis() / 1000L;
        int rnd = ThreadLocalRandom.current().nextInt(1000); // 000..999
        return String.format("%d%010d%03d", t, epochSec, rnd);
    }

    private static void buildRegionSpans(ServerLevel level, List<BlockPos> pts, List<RegionSpan> out) {
        if (pts.size() < 2) return;

        // On évalue la région au milieu de chaque segment [i..i+1]
        int n = pts.size();
        RoadRegionResolver.RegionInfo prevInfo = null;
        int groupStart = 0;

        for (int i = 0; i < n - 1; i++) {
            BlockPos a = pts.get(i);
            BlockPos b = pts.get(i + 1);

            // point milieu (en double -> on arrondit)
            double mx = (a.getX() + b.getX()) * 0.5;
            double my = (a.getY() + b.getY()) * 0.5;
            double mz = (a.getZ() + b.getZ()) * 0.5;
            BlockPos mid = new BlockPos((int)Math.round(mx), (int)Math.round(my), (int)Math.round(mz));

            var info = RoadRegionResolver.locate(level, mid);

            if (prevInfo == null) {
                prevInfo = info;
                groupStart = i;
            } else {
                if (!sameRegion(prevInfo, info)) {
                    // clôture groupe [groupStart .. i]
                    out.add(makeSpan(groupStart, i + 1, prevInfo));
                    prevInfo = info;
                    groupStart = i;
                }
            }
        }
        // dernier groupe -> [groupStart .. n-1]
        out.add(makeSpan(groupStart, n - 1, prevInfo));
    }

    private static boolean sameRegion(RoadRegionResolver.RegionInfo a, RoadRegionResolver.RegionInfo b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return eq(a.territory(), b.territory()) && eq(a.city(), b.city()) && eq(a.district(), b.district());
    }

    private static boolean eq(String x, String y) {
        return (x == null) ? (y == null) : x.equals(y);
    }

    private static RegionSpan makeSpan(int from, int to, RoadRegionResolver.RegionInfo info) {
        RegionSpan s = new RegionSpan();
        s.from = from;
        s.to = to;
        s.territory = info != null ? info.territory() : null;
        s.city      = info != null ? info.city() : null;
        s.district  = info != null ? info.district() : null;
        if (s.territory == null) s.territory = "territoire_1";
        return s;
    }
}
