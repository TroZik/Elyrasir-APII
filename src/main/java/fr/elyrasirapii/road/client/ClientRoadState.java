package fr.elyrasirapii.road.client;

/**
 * Accès singleton à la sélection courante (client).
 * Utilisation : ClientRoadState.get() -> ClientRoadSelection
 */
public final class ClientRoadState {
    private static final ClientRoadSelection INSTANCE = new ClientRoadSelection();

    private ClientRoadState() {}

    /** Renvoie l'instance unique de sélection client. */
    public static ClientRoadSelection get() {
        return INSTANCE;
    }
}
