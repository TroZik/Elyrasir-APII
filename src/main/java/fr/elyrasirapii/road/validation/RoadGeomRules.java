package fr.elyrasirapii.road.validation;

import fr.elyrasirapii.road.utils.RoadType;

/** Paramètres tunables des règles géométriques des routes. */
public final class RoadGeomRules {
    private RoadGeomRules() {}

    /** Dégagement vertical mini (en blocs) lorsqu'il y a croisement en XZ. */
    public static final int Y_CLEARANCE_BLOCKS = 20;

    /** Largeur nominale (en blocs, point au centre). */
    public static int widthBlocks(RoadType type) {
        return switch (type) {
            case RUE -> 3;
            case AVENUE -> 5;
            case BOULEVARD -> 7;
        };
    }

    /** Tangente de l'angle max pour route diagonale (~22.5° => tan≈0.4142). */
    public static final double MAX_SLOPE_DIAGONAL_TAN = Math.tan(Math.toRadians(22.5));

    /** Tangente de l'angle max pour route “axiale” (~45° => tan=1). */
    public static final double MAX_SLOPE_AXIS_TAN = 1.0;

    /**
     * Diagonalité : si 0.5 <= |dx|/|dz| <= 2 → considéré comme “diagonal”.
     * Sinon “axial” (majoritairement aligné X ou Z).
     */
    public static boolean isDiagonal(int dx, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax == 0 || az == 0) return false; // parfaitement axial
        double r = (ax >= az) ? (ax / (double) az) : (az / (double) ax);
        return r <= 2.0; // dans le cône diagonal
    }

    /** Marge d'espacement mini “plates” (en blocs) à ajouter au demi-gabarit. */
    public static final double EXTRA_CLEARANCE_FLAT = 1.0;

    /** Marge d'espacement mini “fortes pentes” (en blocs) — on autorise plus serré. */
    public static final double EXTRA_CLEARANCE_STEEP = 0.25;

    /** Interpolation linéaire [0..1] → lerp(a,b,t). */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
    }
}
