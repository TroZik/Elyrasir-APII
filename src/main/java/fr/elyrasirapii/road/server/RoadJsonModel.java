package fr.elyrasirapii.road.server;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** POJOs pour (dé)serialiser les fichiers routes.json */
public final class RoadJsonModel {
    private RoadJsonModel() {}

    public static final class RouteFile {
        public List<RouteEntry> routes = new ArrayList<>();
    }

    public static final class RouteEntry {
        public String id;          // numérique sous forme String
        public String name;        // nom humain
        public List<BlockPos> points = new ArrayList<>();
        public List<RegionSpan> segments = new ArrayList<>();
    }

    /** Intervalle [from..to] (indices de points) & appartenance régionale. */
    public static final class RegionSpan {
        public int from;           // index point
        public int to;             // index point (>= from)
        public String territory;   // jamais null
        public String city;        // peut être null
        public String district;    // peut être null
    }
}
