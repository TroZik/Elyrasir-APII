package fr.elyrasirapii.road.validation;

import fr.elyrasirapii.road.utils.RoadType;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Utilitaires dédiés aux routes :
 * - Auto-intersection en XZ avec tolérance verticale Y.
 * - Contrôle de pente par segment selon l’orientation (axial/diagonal).
 * - Espacement minimal entre segments non adjacents d'une même route.
 */
public final class RoadGeometryUtils {
    private RoadGeometryUtils() {}

    /* ================== AUTO-INTERSECTION XZ (avec tolérance Y) ================== */

    /**
     * @return true si la polyline s'auto-coupe en XZ avec moins de Y_CLEARANCE_BLOCKS de dénivelé entre
     * les deux segments au point d'intersection (croisement invalide).
     */
    public static boolean hasSelfIntersectionXZWithYClearance(List<BlockPos> pts, int yClearanceBlocks) {
        int n = pts.size();
        if (n < 4) return false; // besoin d'au moins 2 segments non adjacents

        for (int i = 0; i < n - 1; i++) {
            BlockPos a = pts.get(i);
            BlockPos b = pts.get(i + 1);
            for (int j = i + 2; j < n - 1; j++) {
                // éviter les segments adjacents (i,i+1) vs (i+1,i+2)
                if (j == i) continue;
                if (j == i + 1) continue;

                BlockPos c = pts.get(j);
                BlockPos d = pts.get(j + 1);

                if (segmentsIntersectXZ_withYClearance(a, b, c, d, yClearanceBlocks)) {
                    return true; // croisement “trop proche” en Y
                }
            }
        }
        return false;
    }

    private static boolean segmentsIntersectXZ_withYClearance(BlockPos a, BlockPos b,
                                                              BlockPos c, BlockPos d,
                                                              int yClearanceBlocks) {
        // Intersection 2D (XZ) via paramètres t,u ; exclut colinéarités parfaites
        Intersection2D it = intersect2D_XZ(a, b, c, d);
        if (!it.hit) return false;

        // Interpolation Y aux paramètres t et u
        double ya = a.getY() + it.t * (b.getY() - a.getY());
        double yc = c.getY() + it.u * (d.getY() - c.getY());
        double dy = Math.abs(ya - yc);

        return dy < yClearanceBlocks; // trop proche verticalement → auto-coupe invalide
    }

    private static Intersection2D intersect2D_XZ(BlockPos a, BlockPos b, BlockPos c, BlockPos d) {
        // Vecteurs en 2D: (x,z)
        double ax = a.getX(), az = a.getZ();
        double bx = b.getX(), bz = b.getZ();
        double cx = c.getX(), cz = c.getZ();
        double dx = d.getX(), dz = d.getZ();

        double rX = bx - ax, rZ = bz - az;
        double sX = dx - cx, sZ = dz - cz;

        double denom = rX * sZ - rZ * sX;
        if (Math.abs(denom) < 1e-9) {
            // Parallèles ou colinéaires -> on ignore (considéré non-coupant pour l’instant)
            return Intersection2D.noHit();
        }
        double t = ((cx - ax) * sZ - (cz - az) * sX) / denom;
        double u = ((cx - ax) * rZ - (cz - az) * rX) / denom;

        boolean hit = t > 0.0 && t < 1.0 && u > 0.0 && u < 1.0;
        return hit ? new Intersection2D(true, t, u) : Intersection2D.noHit();
    }

    private static final class Intersection2D {
        final boolean hit;
        final double t, u;
        private Intersection2D(boolean hit, double t, double u) { this.hit = hit; this.t = t; this.u = u; }
        static Intersection2D noHit() { return new Intersection2D(false, 0, 0); }
    }

    /* ================== PENTES PAR SEGMENT ================== */

    public static final class SlopeCheck {
        public final boolean ok;
        public final int indexA;        // segment = [indexA, indexA+1]
        public final double slopeTan;   // |dy| / hypot(dx,dz)
        public final double maxTan;     // seuil autorisé utilisé

        public SlopeCheck(boolean ok, int indexA, double slopeTan, double maxTan) {
            this.ok = ok; this.indexA = indexA; this.slopeTan = slopeTan; this.maxTan = maxTan;
        }
    }

    /**
     * Vérifie chaque segment pour la pente max autorisée.
     * - Segment “axial” (majoritairement X ou Z) : tanθ ≤ 1.0 (~45°)
     * - Segment “diagonal” : tanθ ≤ 0.4142 (~22.5°)
     *
     * @return null si tout va bien, sinon le premier échec (avec infos).
     */
    public static SlopeCheck firstSlopeViolation(List<BlockPos> pts) {
        int n = pts.size();
        for (int i = 0; i < n - 1; i++) {
            BlockPos a = pts.get(i);
            BlockPos b = pts.get(i + 1);

            int dx = b.getX() - a.getX();
            int dz = b.getZ() - a.getZ();
            int dy = b.getY() - a.getY();

            double h = Math.hypot(dx, dz);
            if (h < 1e-9) {
                // Segment quasi vertical ou nul -> interdit
                return new SlopeCheck(false, i, Double.POSITIVE_INFINITY, 0.0);
            }

            double tan = Math.abs(dy) / h;
            double max = RoadGeomRules.isDiagonal(dx, dz)
                    ? RoadGeomRules.MAX_SLOPE_DIAGONAL_TAN
                    : RoadGeomRules.MAX_SLOPE_AXIS_TAN;

            if (tan > max + 1e-9) {
                return new SlopeCheck(false, i, tan, max);
            }
        }
        return null;
    }

    /* ================== ESPACEMENT MINI (même route) ================== */

    /**
     * Vérifie que les segments non adjacents restent à une distance XZ >= gabarit/2 + marge,
     * où marge varie selon la pente moyenne (plus fort -> marge plus petite autorisée).
     */
    public static boolean hasMinSpacingWithinRoadOk(List<BlockPos> pts, RoadType type) {
        int n = pts.size();
        if (n < 4) return true;

        double halfGauge = RoadGeomRules.widthBlocks(type) / 2.0;

        for (int i = 0; i < n - 1; i++) {
            BlockPos a = pts.get(i);
            BlockPos b = pts.get(i + 1);
            double tanAB = segmentSlopeTan(a, b);

            for (int j = i + 2; j < n - 1; j++) {
                // skip adjacents
                if (j == i) continue;
                if (j == i + 1) continue;

                BlockPos c = pts.get(j);
                BlockPos d = pts.get(j + 1);
                double tanCD = segmentSlopeTan(c, d);

                double minDist = minDistanceXZ_segmentSegment(a, b, c, d);

                // marge interpolée en fonction de la “pente moyenne” des deux segments
                double avgTan = (finite(tanAB) && finite(tanCD)) ? 0.5 * (tanAB + tanCD) : 0.0;
                // normaliser vs pente max “axiale” (1.0)
                double t = Math.max(0.0, Math.min(1.0, avgTan / RoadGeomRules.MAX_SLOPE_AXIS_TAN));
                double margin = RoadGeomRules.lerp(RoadGeomRules.EXTRA_CLEARANCE_FLAT,
                        RoadGeomRules.EXTRA_CLEARANCE_STEEP, t);

                double required = halfGauge + margin;
                if (minDist < required - 1e-6) {
                    return false; // trop proche
                }
            }
        }
        return true;
    }

    /* ===== Helpers géométriques ===== */

    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }

    private static double segmentSlopeTan(BlockPos a, BlockPos b) {
        double h = Math.hypot(b.getX() - a.getX(), b.getZ() - a.getZ());
        if (h < 1e-9) return Double.POSITIVE_INFINITY;
        return Math.abs(b.getY() - a.getY()) / h;
    }

    /** Distance minimale en XZ entre deux segments [ab] et [cd]. */
    private static double minDistanceXZ_segmentSegment(BlockPos a, BlockPos b, BlockPos c, BlockPos d) {
        // Converti en 2D double
        double ax = a.getX(), az = a.getZ();
        double bx = b.getX(), bz = b.getZ();
        double cx = c.getX(), cz = c.getZ();
        double dx = d.getX(), dz = d.getZ();

        // Si ça s'intersecte en XZ, distance = 0
        if (segmentsIntersect2D(ax, az, bx, bz, cx, cz, dx, dz)) return 0.0;

        // Sinon min des distances point-segment
        double dab = Math.min(distPointSeg2D(ax, az, cx, cz, dx, dz),
                distPointSeg2D(bx, bz, cx, cz, dx, dz));
        double dcd = Math.min(distPointSeg2D(cx, cz, ax, az, bx, bz),
                distPointSeg2D(dx, dz, ax, az, bx, bz));
        return Math.min(dab, dcd);
    }

    private static boolean segmentsIntersect2D(double ax, double az, double bx, double bz,
                                               double cx, double cz, double dx, double dz) {
        double d1 = orient(ax, az, bx, bz, cx, cz);
        double d2 = orient(ax, az, bx, bz, dx, dz);
        double d3 = orient(cx, cz, dx, dz, ax, az);
        double d4 = orient(cx, cz, dx, dz, bx, bz);
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true;
        return false;
    }

    private static double orient(double ax, double az, double bx, double bz, double cx, double cz) {
        return (bx - ax) * (cz - az) - (bz - az) * (cx - ax);
    }

    /** Distance point (px,pz) → segment [sx,sz]-[tx,tz] en 2D. */
    private static double distPointSeg2D(double px, double pz,
                                         double sx, double sz,
                                         double tx, double tz) {
        double vx = tx - sx, vz = tz - sz;
        double wx = px - sx, wz = pz - sz;

        double vv = vx * vx + vz * vz;
        if (vv < 1e-12) return Math.hypot(px - sx, pz - sz); // segment dégénéré

        double t = (wx * vx + wz * vz) / vv;
        if (t <= 0) return Math.hypot(px - sx, pz - sz);
        if (t >= 1) return Math.hypot(px - tx, pz - tz);

        double projx = sx + t * vx, projz = sz + t * vz;
        return Math.hypot(px - projx, pz - projz);
    }
}
