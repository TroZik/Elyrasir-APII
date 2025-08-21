package fr.elyrasirapii.client.render.laser;

import fr.elyrasirapii.road.utils.RoadType;

public final class LaserColors {
    private LaserColors() {}

    // ARGB (0xAARRGGBB)
    public static final int OPAQUE = 0xFF000000;

    // Routes
    public static int colorFor(RoadType t) {
        return switch (t) {
            case RUE       -> 0xFF2ECC71; // vert
            case AVENUE    -> 0xFFE74C3C; // rouge
            case BOULEVARD -> 0xFF3498DB; // bleu
        };
    }

    // Régions
    public static final int TERRITORY = 0xFF000000; // noir
    public static final int CITY      = 0xFFFFFFFF; // blanc

    // Palette districts/parcelles (alternance)
    private static final int[] PALETTE = new int[] {
            0xFFFFA500, // orange vif
            0xFFF1C27D, // orange pâle (sable)
            0xFF9B59B6, // violet
            0xFFB39DDB, // violet clair
            0xFF1ABC9C, // turquoise
            0xFF76D7C4, // turquoise clair
            0xFFF39C12, // ambré
            0xFFF8C471  // ambré clair
    };
    public static int palette(int idx) {
        return PALETTE[Math.floorMod(idx, PALETTE.length)];
    }
}