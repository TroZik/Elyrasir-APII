package fr.elyrasirapii.road.client.gui;

import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.road.client.RoadSelectionManager;
import fr.elyrasirapii.road.network.PacketFinalizeRoadSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import fr.elyrasirapii.server.network.PacketHandler; // <-- ton handler réseau (SimpleChannel)

public final class NameRoadSubmitter {
    private NameRoadSubmitter() {}

    public static void submit(String name, RoadType type) {
        var mc = Minecraft.getInstance();
        var player = mc.player;

        var points = RoadSelectionManager.getSelectionView();
        var pkt = new PacketFinalizeRoadSelection(name, type, points);

        // Envoi C2S via ton SimpleChannel existant
        PacketHandler.CHANNEL.sendToServer(pkt);

        if (player != null) {
            player.displayClientMessage(
                    Component.literal("§aEnvoi de la route \"" + name + "\" (" + type.display() + ")…"), true);
        }
        // Si tu veux: reset local ici ou après ACK serveur
        // RoadSelectionManager.resetSelection(type);
    }
}
