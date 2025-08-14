package fr.elyrasirapii.server.regions;

import fr.elyrasirapii.parcels.utils.PolygonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.Optional;

public final class RegionLookup {

    private RegionLookup() {}

    /** Snapshot hiérarchique de la position du joueur. */
    public static record RegionAt(String territory, String city, String district) {
        public String territoryOrNull() { return territory; }
        public String cityOrNull()      { return city; }
        public String districtOrNull()  { return district; }
    }

    /** Retourne le territoire/ville/district (si trouvés) pour le point XZ de pos. */
    public static RegionAt findAt(MinecraftServer server, BlockPos pos) {
        int px = pos.getX();
        int pz = pos.getZ();

        String foundTerritory = null;
        String foundCity = null;
        String foundDistrict = null;

        // Territoire
        for (String terr : RegionPathHelper.listTerritories(server)) {
            File tf = RegionPathHelper.territoryJson(RegionPathHelper.territoryDir(server, terr));
            var poly = RegionPathHelper.readPolygon(tf);
            if (poly.isPresent() && PolygonUtils.isPointInsidePolygon(px, pz, poly.get())) {
                foundTerritory = terr;
                break;
            }
        }

        if (foundTerritory != null) {
            // Ville dans ce territoire
            for (String city : RegionPathHelper.listCities(server, foundTerritory)) {
                File cf = RegionPathHelper.cityJson(RegionPathHelper.cityDir(server, foundTerritory, city));
                var poly = RegionPathHelper.readPolygon(cf);
                if (poly.isPresent() && PolygonUtils.isPointInsidePolygon(px, pz, poly.get())) {
                    foundCity = city;

                    // District dans cette ville
                    for (String dist : RegionPathHelper.listDistricts(server, foundTerritory, foundCity)) {
                        File df = RegionPathHelper.districtJson(
                                RegionPathHelper.districtDir(server, foundTerritory, foundCity, dist));
                        var dpoly = RegionPathHelper.readPolygon(df);
                        if (dpoly.isPresent() && PolygonUtils.isPointInsidePolygon(px, pz, dpoly.get())) {
                            foundDistrict = dist;
                            break;
                        }
                    }
                    break;
                }
            }
        }

        return new RegionAt(foundTerritory, foundCity, foundDistrict);
    }
}
