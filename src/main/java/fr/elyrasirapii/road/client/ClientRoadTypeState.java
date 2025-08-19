package fr.elyrasirapii.road.client;

import fr.elyrasirapii.road.utils.RoadType;

/** État client du type de route courant (Rue/Avenue/Boulevard). */
public final class ClientRoadTypeState {
    private static RoadType current = RoadType.RUE;

    private ClientRoadTypeState() {}

    public static RoadType get() { return current; }

    public static void set(RoadType t) {
        if (t != null) current = t;
    }

    public static void next() { current = current.next(); }

    public static void prev() { current = current.prev(); }
}
