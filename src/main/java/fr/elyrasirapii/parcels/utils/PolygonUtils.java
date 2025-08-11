package fr.elyrasirapii.parcels.utils;

import net.minecraft.core.BlockPos;

import java.util.*;

public class PolygonUtils {

    private record Vec2(int x, int z) {}

    private record Segment(Vec2 a, Vec2 b) {}

    public static boolean isValidPolygon(List<BlockPos> points) {
        if (points.size() < 3) return false;

        // Conversion XZ uniquement
        Vec2[] flatPoints = points.stream()
                .map(p -> new Vec2(p.getX(), p.getZ()))
                .toArray(Vec2[]::new);

        Map<Integer, Set<Vec2>> segmentToBlocks = new HashMap<>();
        Set<String> seenSegments = new HashSet<>();
        Set<Vec2> allTraversedBlocks = new HashSet<>();

        for (int i = 0; i < flatPoints.length; i++) {
            Vec2 a1 = flatPoints[i];
            Vec2 a2 = flatPoints[(i + 1) % flatPoints.length];
            Segment seg1 = new Segment(a1, a2);

            // Normalisation du segment (ordre des points)
            String key = normalizeSegment(a1, a2);
            if (seenSegments.contains(key)) {
                return false; // Segment déjà vu → boucle ou superposition
            }
            seenSegments.add(key);

            // Collecte des blocs traversés
            Set<Vec2> blocks = computeTraversedBlocks(a1, a2);
            segmentToBlocks.put(i, blocks);
        }

        // Vérifie les chevauchements entre segments non consécutifs
        for (int i = 0; i < flatPoints.length; i++) {
            for (int j = i + 1; j < flatPoints.length; j++) {
                if (Math.abs(i - j) <= 1 || (i == 0 && j == flatPoints.length - 1)) continue;

                Set<Vec2> setA = segmentToBlocks.get(i);
                Set<Vec2> setB = segmentToBlocks.get(j);

                for (Vec2 b : setB) {
                    if (setA.contains(b)) return false;
                }
            }
        }

        // Vérification aire non nulle
        double area = computePolygonArea(flatPoints);
        return area > 0.001;
    }

    private static Set<Vec2> computeTraversedBlocks(Vec2 start, Vec2 end) {
        Set<Vec2> blocks = new HashSet<>();

        int x1 = start.x;
        int z1 = start.z;
        int x2 = end.x;
        int z2 = end.z;

        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);

        int sx = Integer.compare(x2, x1);
        int sz = Integer.compare(z2, z1);

        int err = dx - dz;

        int x = x1;
        int z = z1;

        while (true) {
            blocks.add(new Vec2(x, z));
            if (x == x2 && z == z2) break;
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                z += sz;
            }
        }

        return blocks;
    }

    private static String normalizeSegment(Vec2 a, Vec2 b) {
        return (a.x <= b.x && a.z <= b.z) ? a.x + "_" + a.z + "_" + b.x + "_" + b.z
                : b.x + "_" + b.z + "_" + a.x + "_" + a.z;
    }

    private static double computePolygonArea(Vec2[] points) {
        double area = 0;
        int n = points.length;
        for (int i = 0; i < n; i++) {
            Vec2 p1 = points[i];
            Vec2 p2 = points[(i + 1) % n];
            area += (p1.x * p2.z) - (p2.x * p1.z);
        }
        return Math.abs(area) / 2.0;
    }

    /**
     * Vérifie si un point (x, z) est dans ou sur le bord du polygone (XZ).
     * @param x Coordonnée X du point
     * @param z Coordonnée Z du point
     * @param polygon Liste ordonnée des sommets du polygone (BlockPos)
     * @return true si dans ou sur le bord, false sinon
     */
    public static boolean isPointInsidePolygon(int x, int z, List<BlockPos> polygon) {
        if (polygon.size() < 3) {
            //  System.out.println("[DEBUG] Polygone invalide : moins de 3 points");
            return false;
        }

        boolean inside = false;
        int n = polygon.size();
        double px = x + 0.5; // centre du bloc
        double pz = z + 0.5;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            int xi = polygon.get(i).getX();
            int zi = polygon.get(i).getZ();
            int xj = polygon.get(j).getX();
            int zj = polygon.get(j).getZ();

            // Vérifie si le point est exactement sur une arête (utilisé comme tolérance de bord)
            if (isPointOnSegment(px, pz, xi + 0.5, zi + 0.5, xj + 0.5, zj + 0.5)) {
                // System.out.println("[DEBUG] Point (" + x + "," + z + ") est sur le bord entre (" + xi + "," + zi + ") et (" + xj + "," + zj + ")");
                return true;
            }

            boolean intersect = ((zi > pz) != (zj > pz)) &&
                    (px < (double)(xj - xi) * (pz - zi) / (double)(zj - zi + 1e-8) + xi);

            if (intersect) {
                inside = !inside;
            }
        }

        // System.out.println("[DEBUG] Point (" + x + "," + z + ") est " + (inside ? "dans" : "hors") + " du polygone");
        return inside;
    }

    /**
     * Vérifie si un point est exactement sur un segment (tolérance de type bloc Minecraft)
     */
    private static boolean isPointOnSegment(double px, double pz, double x1, double z1, double x2, double z2) {
        double cross = (px - x1) * (z2 - z1) - (pz - z1) * (x2 - x1);
        if (Math.abs(cross) > 1e-5) return false;

        double dot = (px - x1) * (x2 - x1) + (pz - z1) * (z2 - z1);
        if (dot < 0) return false;

        double squaredLen = (x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1);
        return dot <= squaredLen;
    }





}
