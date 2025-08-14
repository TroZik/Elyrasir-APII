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

    public static void tryOpenNamingScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Récupère le mode courant
        var mode = fr.elyrasirapii.parcels.selection.ArchitectModeManager.getMode();

        // Récupère la sélection du mode courant
        // NOTE: si ta méthode s’appelle différemment (ex: getFullSelection()),
        // garde la tienne. Le point important, c’est d’obtenir les points du mode actif.
        java.util.List<net.minecraft.core.BlockPos> points =
                fr.elyrasirapii.client.network.ClientSelectionManager.getFullSelection();

        // Seuils par mode (polygone => au moins 3)
        int minPoints = switch (mode) {
            case PARCEL -> 3;
            case DISTRICT -> 3;
            case CITY -> 3;
            case TERRITORY -> 3;
        };

        // Max par mode (tu as dit 1024 pour District/City/Territory)
        int maxPoints = switch (mode) {
            case PARCEL -> 16;      // ce que tu utilises déjà
            case DISTRICT -> 1024;
            case CITY -> 1024;
            case TERRITORY -> 1024;
        };

        // Vérifs rapides côté client
        if (points.size() < minPoints) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cAjoutez plus de points (min " + minPoints + ")."), true
            );
            playVillagerNo(mc);
            return;
        }
        if (points.size() > maxPoints) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cTrop de points pour ce mode (max " + maxPoints + ")."), true
            );
            playVillagerNo(mc);
            return;
        }

        // Validation géométrique 2D (XZ)
        boolean valid = fr.elyrasirapii.parcels.utils.PolygonUtils.isValidPolygon(points);
        if (!valid) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cSélection invalide : forme incorrecte."), true
            );
            playVillagerNo(mc);
            return;
        }

        // Ouvrir l’écran adapté
        switch (mode) {
            case PARCEL -> mc.setScreen(new fr.elyrasirapii.client.gui.NameParcelScreen());
            case DISTRICT, CITY, TERRITORY ->
                    mc.setScreen(new fr.elyrasirapii.client.gui.NameRegionScreen(mode));
        }
    }

    private static void playVillagerNo(Minecraft mc) {
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f, 1.0f, false
            );
        }
    }
}

