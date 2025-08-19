package fr.elyrasirapii.road.utils;

/**
        * Limites de points pour chaque type de route.
        * Remets ici EXACTEMENT les valeurs de ta V1.
        */
public final class RoadLimits {
    private RoadLimits() {}

    /** Nombre minimum de points pour qu'une route soit considérée valide. */
    public static final int MIN_POINTS = 2;

    // === À AJUSTER avec tes valeurs V1 ===
    public static final int MAX_POINTS_RUE       = 512;
    public static final int MAX_POINTS_AVENUE    = 1024;
    public static final int MAX_POINTS_BOULEVARD = 2048;
    // =====================================

    /** Renvoie la limite (incluse) de points pour le type donné. */
    public static int maxPointsFor(RoadType type) {
        return switch (type) {
            case RUE -> MAX_POINTS_RUE;
            case AVENUE -> MAX_POINTS_AVENUE;
            case BOULEVARD -> MAX_POINTS_BOULEVARD;
        };
    }

    /** Renvoie true si 'count' respecte les bornes pour ce type. */
    public static boolean isCountWithinLimits(RoadType type, int count) {
        return count >= MIN_POINTS && count <= maxPointsFor(type);
    }
}