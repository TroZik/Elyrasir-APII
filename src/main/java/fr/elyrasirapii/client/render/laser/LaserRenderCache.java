package fr.elyrasirapii.client.render.laser;

import java.util.ArrayList;
import java.util.List;

public final class LaserRenderCache {
    private static final List<LaserLine> ROUTE_LINES = new ArrayList<>();
    private static final List<LaserLine> REGION_LINES = new ArrayList<>();

    private LaserRenderCache() {}

    public static synchronized void setRouteLines(List<LaserLine> lines) {
        ROUTE_LINES.clear(); ROUTE_LINES.addAll(lines);
    }
    public static synchronized void setRegionLines(List<LaserLine> lines) {
        REGION_LINES.clear(); REGION_LINES.addAll(lines);
    }
    public static synchronized List<LaserLine> snapshotAll() {
        List<LaserLine> out = new ArrayList<>(ROUTE_LINES.size() + REGION_LINES.size());
        out.addAll(ROUTE_LINES); out.addAll(REGION_LINES);
        return out;
    }
}