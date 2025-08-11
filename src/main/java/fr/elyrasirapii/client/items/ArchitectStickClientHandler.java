package fr.elyrasirapii.client.items;

import fr.elyrasirapii.client.gui.NameParcelScreen;
import fr.elyrasirapii.client.network.ClientSelectionManager;
import fr.elyrasirapii.parcels.utils.PolygonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class ArchitectStickClientHandler {

    public static void openNamingScreen() {
        Minecraft mc = Minecraft.getInstance();

        if (!PolygonUtils.isValidPolygon(ClientSelectionManager.getFullSelection())) {
            if (mc.player != null && mc.level != null) {
                mc.player.displayClientMessage(
                        Component.literal("§cSélection invalide : forme incorrecte."), true
                );
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        SoundEvents.VILLAGER_NO,
                        SoundSource.PLAYERS,
                        1.0f, 1.0f,
                        false
                );
            }
            return;
        }

        mc.setScreen(new NameParcelScreen());
    }
}
