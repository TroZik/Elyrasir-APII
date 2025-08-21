package fr.elyrasirapii.server.utils;

import fr.elyrasirapii.client.network.PacketDisplayTitle;
import fr.elyrasirapii.parcels.ParcelsManager;
import fr.elyrasirapii.server.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.Objects;
import java.util.UUID;

// ====== Imports existants HUD route ======
import fr.elyrasirapii.server.regions.RegionLookup;
import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.road.network.PacketSetRoadHud;
import fr.elyrasirapii.road.server.RoadIOPaths;
import fr.elyrasirapii.road.server.RoadRuntimeIndex;

// ====== Nouveaux imports pour LASERS ======
import fr.elyrasirapii.server.regions.RegionPathHelper;
import fr.elyrasirapii.server.utils.laser.PacketSetLaserLines;
import fr.elyrasirapii.server.utils.laser.PacketSetLaserLines.Kind;
import fr.elyrasirapii.client.render.laser.LaserLine;
import fr.elyrasirapii.client.render.laser.LaserColors;

public class onServerTick {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        BlockPos pos = player.blockPosition();
        UUID uuid = player.getUUID();

        // ===== Parcelles (inchangé) =====
        String currentParcel = ParcelsManager.get().getParcelAt(pos);   // peut contenir "@fichier"
        String lastParcel = ParcelsManager.get().getPlayerParcelMap().get(uuid);

        String currDisplay = displayOnlyName(currentParcel);
        String lastDisplay = displayOnlyName(lastParcel);

        if (!Objects.equals(currentParcel, lastParcel)) {
            if (lastParcel != null) {
                PacketHandler.sendToClient((ServerPlayer) player,
                        new PacketDisplayTitle("Vous quittez : " + lastDisplay));
            }
            if (currentParcel != null) {
                PacketHandler.sendToClient((ServerPlayer) player,
                        new PacketDisplayTitle("Vous entrez : " + currDisplay));
            }
            ParcelsManager.get().getPlayerParcelMap().put(uuid, currentParcel);
        }

        // ===== HUD route (inchangé) =====
        updateRoadHud((ServerPlayer) player);

        // ===== LASERS (routes/régions) =====
        updateLaserLines((ServerPlayer) player);
    }

    private static String displayOnlyName(String raw) {
        if (raw == null) return null;
        int at = raw.indexOf('@');
        return at >= 0 ? raw.substring(0, at) : raw;
    }

    // ====== HUD ROUTE (inchangé sauf Y_TOLERANCE que tu as mis à 4.0) ======

    private static final Map<UUID, String> lastRoadShown = new HashMap<>();
    private static final double Y_TOLERANCE = 4.0;

    private static void updateRoadHud(ServerPlayer sp) {
        var server = sp.getServer();
        if (server == null) return;

        var reg = RegionLookup.findAt(server, sp.blockPosition());
        String territory = reg.territoryOrNull();
        if (territory == null) {
            clearRoadHud(sp);
            return;
        }

        boolean anyFile = false;
        for (RoadType t : RoadType.values()) {
            if (java.nio.file.Files.exists(RoadIOPaths.routesFile(server, territory, t))) {
                anyFile = true;
                break;
            }
        }
        if (!anyFile) {
            clearRoadHud(sp);
            return;
        }

        List<RoadRuntimeIndex.Segment> segs = RoadRuntimeIndex.getSegmentsFor(server, territory);
        if (segs.isEmpty()) {
            clearRoadHud(sp);
            return;
        }

        String bestName = findRoadUnderPlayer(sp, segs);
        UUID id = sp.getUUID();
        String prev = lastRoadShown.get(id);

        if (!Objects.equals(bestName, prev)) {
            lastRoadShown.put(id, bestName);
            PacketHandler.sendToClient(sp, new PacketSetRoadHud(bestName == null ? "" : bestName));
        }
    }

    private static void clearRoadHud(ServerPlayer sp) {
        UUID id = sp.getUUID();
        String prev = lastRoadShown.get(id);
        if (prev != null && !prev.isEmpty()) {
            lastRoadShown.remove(id);
            PacketHandler.sendToClient(sp, new PacketSetRoadHud(""));
        }
    }

    private static String findRoadUnderPlayer(ServerPlayer sp, List<RoadRuntimeIndex.Segment> segs) {
        double px = sp.getX(), pz = sp.getZ(), py = sp.getY();

        double bestDist = Double.MAX_VALUE;
        String bestName = null;

        for (var s : segs) {
            double distXZ = distPointSeg2D(px, pz, s.a().getX(), s.a().getZ(), s.b().getX(), s.b().getZ());
            double halfWidth = widthBlocks(s.type()) / 2.0;
            if (distXZ > halfWidth + 1e-3) continue;

            double yOnSeg = yAtClosestPointOnSegment(px, pz, s.a(), s.b());
            if (Math.abs(py - yOnSeg) > Y_TOLERANCE) continue;

            if (distXZ < bestDist) {
                bestDist = distXZ;
                bestName = label(s.type(), s.routeName());
            }
        }
        return bestName;
    }

    private static int widthBlocks(RoadType t) {
        return switch (t) {
            case RUE -> 3;
            case AVENUE -> 5;
            case BOULEVARD -> 7;
        };
    }

    private static String label(RoadType t, String name) {
        String kind = switch (t) {
            case RUE -> "Rue";
            case AVENUE -> "Avenue";
            case BOULEVARD -> "Boulevard";
        };
        return kind + " " + name;
    }

    private static double distPointSeg2D(double px, double pz,
                                         double sx, double sz,
                                         double tx, double tz) {
        double vx = tx - sx, vz = tz - sz;
        double wx = px - sx, wz = pz - sz;
        double vv = vx * vx + vz * vz;
        if (vv < 1e-12) return Math.hypot(px - sx, pz - sz);
        double t = (wx * vx + wz * vz) / vv;
        if (t <= 0) return Math.hypot(px - sx, pz - sz);
        if (t >= 1) return Math.hypot(px - tx, pz - tz);
        double projx = sx + t * vx, projz = sz + t * vz;
        return Math.hypot(px - projx, pz - projz);
    }

    private static double yAtClosestPointOnSegment(double px, double pz, BlockPos a, BlockPos b) {
        double ax = a.getX(), az = a.getZ();
        double bx = b.getX(), bz = b.getZ();
        double vx = bx - ax, vz = bz - az;
        double wx = px - ax, wz = pz - az;
        double vv = vx * vx + vz * vz;
        if (vv < 1e-12) return a.getY();
        double t = (wx * vx + wz * vz) / vv;
        if (t <= 0) return a.getY();
        if (t >= 1) return b.getY();
        return a.getY() + t * (b.getY() - a.getY());
    }

    // ==================== LASERS ====================

    private static final double RADIUS = 125.0; // ≈ 250 de diamètre
    private static final double Y_OFFSET = 2.0; // Y+1 pour les lasers
    private static final Map<UUID, Integer> lastSentTick = new HashMap<>();
    private static final int SEND_EVERY_TICKS = 10; // envoi toutes ~10 ticks

    private static void updateLaserLines(ServerPlayer sp) {
        // cooldown d'envoi pour éviter le spam
        int tick = sp.getServer().getTickCount();
        Integer last = lastSentTick.get(sp.getUUID());
        if (last != null && (tick - last) < SEND_EVERY_TICKS) return;
        lastSentTick.put(sp.getUUID(), tick);

        boolean wantRoutes = hasRoadStick(sp);
        boolean wantRegions = hasArchitectStick(sp);

        if (!wantRoutes && !wantRegions) {
            PacketHandler.sendToClient(sp, new PacketSetLaserLines(Kind.ROUTE, List.of()));
            PacketHandler.sendToClient(sp, new PacketSetLaserLines(Kind.REGION, List.of()));
            return;
        }

        var server = sp.getServer();
        var loc = RegionLookup.findAt(server, sp.blockPosition());
        String territory = loc.territoryOrNull();

        if (wantRoutes) {
            List<LaserLine> routeLines = computeRouteLines(server, territory, sp.getX(), sp.getY(), sp.getZ());
            //debug
            //sp.displayClientMessage(Component.literal("routes: " + routeLines.size()), true);


            PacketHandler.sendToClient(sp, new PacketSetLaserLines(Kind.ROUTE, routeLines));
        } else {
            PacketHandler.sendToClient(sp, new PacketSetLaserLines(Kind.ROUTE, List.of()));
        }

        if (wantRegions) {
            List<LaserLine> regionLines = computeRegionLines(server, territory, sp.getX(), sp.getY(), sp.getZ());
            PacketHandler.sendToClient(sp, new PacketSetLaserLines(Kind.REGION, regionLines));
        } else {
            PacketHandler.sendToClient(sp, new PacketSetLaserLines(Kind.REGION, List.of()));
        }
    }

    // Inventaire : adapte si tu as des RegistryObjects pour tes items
    private static boolean hasRoadStick(ServerPlayer sp) {
        for (ItemStack st : sp.getInventory().items) {
            if (!st.isEmpty() && "item.elyrasirapii.road_stick".equals(st.getDescriptionId())) return true;
        }
        return false;
    }


    private static boolean hasArchitectStick(ServerPlayer sp) {
        for (ItemStack st : sp.getInventory().items) {
            if (!st.isEmpty() && st.getDescriptionId() != null && st.getDescriptionId().endsWith(".architect_stick"))
                return true;
        }
        return false;
    }

    private static List<LaserLine> computeRouteLines(MinecraftServer server,
                                                     String territory, double px, double py, double pz) {
        List<LaserLine> out = new ArrayList<>();
        double r2 = RADIUS * RADIUS;

        // Helper qui charge et filtre un territoire
        java.util.function.Consumer<String> collectFrom = terr -> {
            var segs = RoadRuntimeIndex.getSegmentsFor(server, terr);
            for (var s : segs) {
                double cx = 0.5 * (s.a().getX() + s.b().getX());
                double cz = 0.5 * (s.a().getZ() + s.b().getZ());
                double dx = cx - px, dz = cz - pz;
                if ((dx * dx + dz * dz) > r2) continue;

                int col = LaserColors.colorFor(s.type());
                out.add(new LaserLine(
                        s.a().getX() + 0.5, s.a().getY() + Y_OFFSET, s.a().getZ() + 0.5,
                        s.b().getX() + 0.5, s.b().getY() + Y_OFFSET, s.b().getZ() + 0.5,
                        col
                ));
            }
        };

        if (territory != null) {
            // Ne dessine que si au moins un fichier existe, sinon on tente quand même le fallback
            boolean any = false;
            for (RoadType t : RoadType.values()) {
                if (java.nio.file.Files.exists(RoadIOPaths.routesFile(server, territory, t))) {
                    any = true;
                    break;
                }
            }
            if (any) collectFrom.accept(territory);
        }

        // Fallback: aucun territoire détecté / aucun fichier : scanner tous les territoires (dans le rayon)
        if (out.isEmpty()) {
            for (String terr : RegionPathHelper.listTerritories(server)) {
                collectFrom.accept(terr);
            }
        }
        return out;
    }

    // ===== Régions → lignes “laser” =====
    private static List<LaserLine> computeRegionLines(net.minecraft.server.MinecraftServer server,
                                                      String territory, double px, double py, double pz) {
        List<LaserLine> out = new ArrayList<>();

        if (territory != null) {
            collectRegionLinesFor(server, territory, px, pz, out);
            // >>> AJOUT : parcelles visibles autour du joueur
            collectParcelLines(server, px, pz, out);
        }

        if (out.isEmpty()) {
            for (String terr : RegionPathHelper.listTerritories(server)) {
                collectRegionLinesFor(server, terr, px, pz, out);
            }
            // >>> AJOUT : parcelles visibles autour du joueur (fallback hors territoire)
            collectParcelLines(server, px, pz, out);
        }
        return out;
    }



    /**
     * Ajoute au buffer les bordures du territoire + villes + districts (filtrées au rayon).
     */
    private static void collectRegionLinesFor(net.minecraft.server.MinecraftServer server, String terr,
                                              double px, double pz, List<LaserLine> out) {
        // Territoire (noir)
        var terrFile = RegionPathHelper.territoryJson(RegionPathHelper.territoryDir(server, terr));
        RegionPathHelper.readPolygon(terrFile).ifPresent(poly ->
                addPolyline(out, poly, LaserColors.TERRITORY, px, pz));

        // Villes (blanc)
        var cities = RegionPathHelper.listCities(server, terr);
        for (String city : cities) {
            var cf = RegionPathHelper.cityJson(RegionPathHelper.cityDir(server, terr, city));
            RegionPathHelper.readPolygon(cf).ifPresent(poly ->
                    addPolyline(out, poly, LaserColors.CITY, px, pz));

            // Districts (palette alternée par ville)
            int idx = 0;
            for (String dist : RegionPathHelper.listDistricts(server, terr, city)) {
                var df = RegionPathHelper.districtJson(RegionPathHelper.districtDir(server, terr, city, dist));
                int col = LaserColors.palette(idx++);
                RegionPathHelper.readPolygon(df).ifPresent(poly ->
                        addPolyline(out, poly, col, px, pz));
            }
        }
    }

    /**
     * Ajoute un polyligne fermé si au moins 1 point est dans (rayon * 1.2).
     */
    private static void addPolyline(List<LaserLine> out, List<BlockPos> poly, int color, double px, double pz) {
        if (poly == null || poly.size() < 2) return;

        double r2 = RADIUS * RADIUS;
        boolean near = false;
        for (BlockPos p : poly) {
            double dx = (p.getX() + 0.5) - px;
            double dz = (p.getZ() + 0.5) - pz;
            if (dx * dx + dz * dz <= r2 * 1.2) {
                near = true;
                break;
            }
        }
        if (!near) return;

        for (int i = 0; i < poly.size(); i++) {
            BlockPos a = poly.get(i);
            BlockPos b = poly.get((i + 1) % poly.size()); // fermé
            out.add(new LaserLine(
                    a.getX() + 0.5, a.getY() + Y_OFFSET, a.getZ() + 0.5,
                    b.getX() + 0.5, b.getY() + Y_OFFSET, b.getZ() + 0.5,
                    color
            ));
        }
    }

    // ===== Parcelles → lignes “laser” (utilise le cache en mémoire du ParcelsManager) =====
    private static void collectParcelLines(net.minecraft.server.MinecraftServer server,
                                           double px, double pz, List<LaserLine> out) {
        // Le ParcelsManager est rafraîchi à chaque tick via getParcelAt(); on lit la map actuelle
        var pm = fr.elyrasirapii.parcels.ParcelsManager.get();
        var map = pm.getParcels(); // Map<String name@path, List<BlockPos> polygon>

        if (map == null || map.isEmpty()) return;

        int idx = 0;
        double r2 = RADIUS * RADIUS;

        for (var entry : map.entrySet()) {
            String key = entry.getKey();
            List<BlockPos> poly = entry.getValue();
            if (poly == null || poly.size() < 2) continue;

            // Filtre : on garde si au moins 1 point est proche du joueur (rayon * 1.2)
            boolean near = false;
            for (BlockPos p : poly) {
                double dx = (p.getX() + 0.5) - px;
                double dz = (p.getZ() + 0.5) - pz;
                if (dx * dx + dz * dz <= r2 * 1.2) { near = true; break; }
            }
            if (!near) continue;

            // Couleur stable par parcelle : on dérive l’index de la clé (évite de changer au fil des ticks)
            int color = LaserColors.palette(Math.abs(key.hashCode()));

            // Ajoute le contour fermé
            for (int i = 0; i < poly.size(); i++) {
                BlockPos a = poly.get(i);
                BlockPos b = poly.get((i + 1) % poly.size());
                out.add(new LaserLine(
                        a.getX() + 0.5, a.getY() + Y_OFFSET, a.getZ() + 0.5,
                        b.getX() + 0.5, b.getY() + Y_OFFSET, b.getZ() + 0.5,
                        color
                ));
            }

            idx++;
        }
    }




}