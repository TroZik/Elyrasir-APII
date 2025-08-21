package fr.elyrasirapii.road.server;

import fr.elyrasirapii.server.regions.RegionLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Pont routes -> système de régions existant. */
public final class RoadRegionResolver {

    public record RegionInfo(String territory, String city, String district) {}

    private RoadRegionResolver() {}

    /** Détermine le territoire d'un point (fallback "territoire_1" si introuvable). */
    public static String findTerritory(ServerLevel level, BlockPos p) {
        var at = RegionLookup.findAt(level.getServer(), p);
        String terr = at.territoryOrNull();
        return terr != null ? terr : "territoire_1";
    }

    /** Renvoie territoire/ville/district pour un point. (peut contenir des null pour ville/district) */
    public static RegionInfo locate(ServerLevel level, BlockPos p) {
        var at = RegionLookup.findAt(level.getServer(), p);
        return new RegionInfo(at.territoryOrNull(), at.cityOrNull(), at.districtOrNull());
    }
}
