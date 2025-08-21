package fr.elyrasirapii.road.server;

import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.server.regions.RegionPathHelper;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.nio.file.Path;

public final class RoadIOPaths {
    private RoadIOPaths() {}

    /** Dossier racine du territoire (world/serverconfig/elyrasir/<territoire>) */
    public static Path territoryRoot(MinecraftServer server, String territoryName) {
        File terrDir = RegionPathHelper.territoryDir(server, territoryName);
        return terrDir.toPath();
    }

    /** Fichier des routes, par type, à la racine du territoire. */
    public static Path routesFile(MinecraftServer server, String territoryName, RoadType type) {
        String file = switch (type) {
            case RUE -> "streets.json";
            case AVENUE -> "avenues.json";
            case BOULEVARD -> "boulevards.json";
        };
        return territoryRoot(server, territoryName).resolve(file);
    }
}
