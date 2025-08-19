package fr.elyrasirapii.road.utils;

public enum RoadType {
    RUE("Rue"),
    AVENUE("Avenue"),
    BOULEVARD("Boulevard");

    private final String display;

    RoadType(String display) {
        this.display = display;
    }

    /** Type suivant dans l'ordre défini par l'enum. */
    public RoadType next() {
        RoadType[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }

    /** Type précédent dans l'ordre défini par l'enum. */
    public RoadType prev() {
        RoadType[] vals = values();
        return vals[(this.ordinal() - 1 + vals.length) % vals.length];
    }

    /** Libellé pour affichage (hotbar / tooltip). */
    public String display() {
        return display;
    }
}