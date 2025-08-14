package fr.elyrasirapii.parcels.selection;

public enum RegionEditMode { PARCEL,     // Mode 1 : parcelles (actuel)
    DISTRICT,   // Mode 2 : quartiers
    CITY,       // Mode 3 : villes (capitale + autres)
    TERRITORY;  // Mode 4 : territoire

    public String displayName() {
        return switch (this) {
            case PARCEL -> "Parcelles";
            case DISTRICT -> "Districts";
            case CITY -> "Villes";
            case TERRITORY -> "Territoire";
        };
    }
}
