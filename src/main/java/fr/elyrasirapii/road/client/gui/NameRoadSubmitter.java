package fr.elyrasirapii.road.client.gui;

import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.road.client.RoadSelectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Étape réseau à venir :
 *  - Ici on enverra PacketFinalizeRoadSelection(name, type, points)
 *
 * Pour l’instant : message + (optionnel) reset de la sélection du type courant.
 */
public final class NameRoadSubmitter {
    private NameRoadSubmitter() {}

    public static void submit(String name, RoadType type) {
        var mc = Minecraft.getInstance();
        var player = mc.player;

        // TODO réseau : envoyer le packet avec (name, type, points)
        if (player != null) {
            player.displayClientMessage(
                    Component.literal("§aRoute prête à envoyer: \"" + name + "\" (" + type.display() + "), "
                            + RoadSelectionManager.size() + " points"), true);
        }

        // décommenter pour reset immédiat :
        // RoadSelectionManager.resetSelection(type);
    }
}
