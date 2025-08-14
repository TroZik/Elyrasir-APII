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


    /** Retourne vrai si chaque sommet de inner est à l'intérieur (ou sur le bord) de outer. */
    public static boolean containsPolygonAllowTouch(java.util.List<net.minecraft.core.BlockPos> outer,
                                                    java.util.List<net.minecraft.core.BlockPos> inner) {
        for (net.minecraft.core.BlockPos p : inner) {
            if (!isPointInsidePolygon(p.getX(), p.getZ(), outer)) {
                return false;
            }
        }
        return true;
    }

    /** Retourne vrai s'il existe un recouvrement avec surface non nulle.
     *  - segments qui se croisent hors extrémités => overlap
     *  - un sommet strictement à l'intérieur de l'autre => overlap
     *  Le simple contact bord-à-bord (extrémités ou colinéaire sans aire) n'est pas considéré comme overlap.
     */
    public static boolean polygonsOverlapStrict(java.util.List<net.minecraft.core.BlockPos> A,
                                                java.util.List<net.minecraft.core.BlockPos> B) {
        // 1) intersections de segments
        for (int i = 0; i < A.size(); i++) {
            var a1 = A.get(i);
            var a2 = A.get((i + 1) % A.size());
            for (int j = 0; j < B.size(); j++) {
                var b1 = B.get(j);
                var b2 = B.get((j + 1) % B.size());

                if (segmentsProperlyIntersect2D(a1, a2, b1, b2)) return true;
            }
        }
        // 2) sommet strictement à l'intérieur
        if (pointStrictlyInside(A, B.get(0)) || pointStrictlyInside(B, A.get(0))) return true;

        return false;
    }

    // Intersections strictes : croisement hors extrémités (pas juste toucher)
    private static boolean segmentsProperlyIntersect2D(net.minecraft.core.BlockPos a1, net.minecraft.core.BlockPos a2,
                                                       net.minecraft.core.BlockPos b1, net.minecraft.core.BlockPos b2) {
        // On réutilise l'orientation ccw et on exclut les cas colinéaires ou endpoints partagés
        int x1=a1.getX(), z1=a1.getZ(), x2=a2.getX(), z2=a2.getZ();
        int x3=b1.getX(), z3=b1.getZ(), x4=b2.getX(), z4=b2.getZ();

        long d1 = (long)(x4 - x3) * (z1 - z3) - (long)(z4 - z3) * (x1 - x3);
        long d2 = (long)(x4 - x3) * (z2 - z3) - (long)(z4 - z3) * (x2 - x3);
        long d3 = (long)(x2 - x1) * (z3 - z1) - (long)(z2 - z1) * (x3 - x1);
        long d4 = (long)(x2 - x1) * (z4 - z1) - (long)(z2 - z1) * (x4 - x1);

        if (d1 == 0 || d2 == 0 || d3 == 0 || d4 == 0) {
            // au moins colinéaire/tangent → pas "proper"
            return false;
        }
        return (d1 > 0) != (d2 > 0) && (d3 > 0) != (d4 > 0);
    }

    private static boolean pointStrictlyInside(java.util.List<net.minecraft.core.BlockPos> poly,
                                               net.minecraft.core.BlockPos p) {
        // Sur le bord => false ici (strict). Ta isPointInsidePolygon retourne true pour bord,
        // donc on la re-teste et on exclude les cas "sur bord".
        if (!isPointInsidePolygon(p.getX(), p.getZ(), poly)) return false;

        // Test "sur bord" rapide : vérifier si p est colinéaire avec au moins une arête et dans ses bornes.
        for (int i = 0; i < poly.size(); i++) {
            var a = poly.get(i);
            var b = poly.get((i + 1) % poly.size());
            if (pointOnSegment2D(p, a, b)) return false; // sur bord → pas "strict"
        }
        return true;
    }

    private static boolean pointOnSegment2D(net.minecraft.core.BlockPos p,
                                            net.minecraft.core.BlockPos a,
                                            net.minecraft.core.BlockPos b) {
        int px=p.getX(), pz=p.getZ(), ax=a.getX(), az=a.getZ(), bx=b.getX(), bz=b.getZ();
        long cross = (long)(px - ax) * (bz - az) - (long)(pz - az) * (bx - ax);
        if (cross != 0) return false;
        return Math.min(ax, bx) <= px && px <= Math.max(ax, bx)
                && Math.min(az, bz) <= pz && pz <= Math.max(az, bz);
    }


    // --- Chevauchement entre deux polygones en XZ ---
    public static boolean polygonsOverlapXZ(java.util.List<net.minecraft.core.BlockPos> polyA,
                                            java.util.List<net.minecraft.core.BlockPos> polyB) {
        if (polyA.size() < 3 || polyB.size() < 3) return false;

        // 1) Test intersections d'arêtes
        for (int i = 0; i < polyA.size(); i++) {
            net.minecraft.core.BlockPos a1 = polyA.get(i);
            net.minecraft.core.BlockPos a2 = polyA.get((i + 1) % polyA.size());
            for (int j = 0; j < polyB.size(); j++) {
                net.minecraft.core.BlockPos b1 = polyB.get(j);
                net.minecraft.core.BlockPos b2 = polyB.get((j + 1) % polyB.size());
                if (segmentsIntersect2D(a1, a2, b1, b2)) {
                    return true;
                }
            }
        }

        // 2) Si pas d’intersection : A contient un point de B, ou B contient un point de A
        net.minecraft.core.BlockPos b0 = polyB.get(0);
        if (isPointInsidePolygon(b0.getX(), b0.getZ(), polyA)) return true;

        net.minecraft.core.BlockPos a0 = polyA.get(0);
        return isPointInsidePolygon(a0.getX(), a0.getZ(), polyB);
    }

    private static boolean segmentsIntersect2D(net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b,
                                               net.minecraft.core.BlockPos c, net.minecraft.core.BlockPos d) {
        return linesIntersect(a.getX(), a.getZ(), b.getX(), b.getZ(),
                c.getX(), c.getZ(), d.getX(), d.getZ());
    }

    private static boolean linesIntersect(int x1, int y1, int x2, int y2,
                                          int x3, int y3, int x4, int y4) {
        int d1 = direction(x3, y3, x4, y4, x1, y1);
        int d2 = direction(x3, y3, x4, y4, x2, y2);
        int d3 = direction(x1, y1, x2, y2, x3, y3);
        int d4 = direction(x1, y1, x2, y2, x4, y4);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }

        return (d1 == 0 && onSegment(x3, y3, x4, y4, x1, y1)) ||
                (d2 == 0 && onSegment(x3, y3, x4, y4, x2, y2)) ||
                (d3 == 0 && onSegment(x1, y1, x2, y2, x3, y3)) ||
                (d4 == 0 && onSegment(x1, y1, x2, y2, x4, y4));
    }

    private static int direction(int xi, int yi, int xj, int yj, int xk, int yk) {
        return (xk - xi) * (yj - yi) - (xj - xi) * (yk - yi);
    }

    private static boolean onSegment(int xi, int yi, int xj, int yj, int xk, int yk) {
        return Math.min(xi, xj) <= xk && xk <= Math.max(xi, xj) &&
                Math.min(yi, yj) <= yk && yk <= Math.max(yi, yj);
    }



}
