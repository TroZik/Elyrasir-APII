package fr.elyrasirapii.client.network;

import fr.elyrasirapii.client.render.DigitParticleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientSelectionManager {

    private static int frameCounter = 1;
    private static int selectedIndex = 1;


    private static final List<BlockPos> selectedPoints = new ArrayList<>();

    public static void addPoint(BlockPos pos) {
        if (selectedPoints.size() < 16) { // Limite à 16 points
            selectedPoints.add(pos);

            // Affichage du chiffre en particules
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                int index = selectedPoints.size() - 1;
                DigitParticleRenderer.renderDigit(mc.level, pos, index);
            }
        }
    }

    public static List<BlockPos> getFullSelection() {
        return Collections.unmodifiableList(selectedPoints);
    }

    public static void clear() {
        selectedPoints.clear();
    }

    public static int size() {
        return selectedPoints.size();
    }

    public static boolean isComplete() {
        return selectedPoints.size() >= 3; // Exemple : on considère 3+ points = prêt à valider
    }



    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null && mc.player != null && !selectedPoints.isEmpty()) {
            frameCounter++;
            if (frameCounter % 10 != 0) return; // ≈ toutes les 10 ticks

            for (int i = 0; i < selectedPoints.size(); i++) {
                BlockPos pos = selectedPoints.get(i);
                DigitParticleRenderer.renderDigit(mc.level, pos.above(), i);
            }
        }
    }

    public static void cycleSelectedIndex(int direction) {
        int max = selectedPoints.size() + 1; // +1 pour le point "à poser"
        selectedIndex += direction;

        if (selectedIndex < 1) {
            selectedIndex = max;
        } else if (selectedIndex > max) {
            selectedIndex = 1;
        }

        // Affichage temporaire (à remplacer dans l'event overlay)
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("Point sélectionné : #" + selectedIndex), true
            );
        }
    }

    public static int getSelectedIndex() {
        return selectedIndex;
    }

    public static void addOrReplacePoint(BlockPos pos) {
        if (selectedIndex > 0 && selectedIndex <= selectedPoints.size()) {
            // Remplacer le point existant
            selectedPoints.set(selectedIndex - 1, pos);
        } else if (selectedPoints.size() < 16) {
            // Ajouter un nouveau point
            selectedPoints.add(pos);
            selectedIndex = selectedPoints.size(); // Se placer sur le nouveau
        }

        // Affichage chiffre
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            int index = selectedIndex > 0 ? selectedIndex - 1 : selectedPoints.size() - 1;
            DigitParticleRenderer.renderDigit(mc.level, pos.above(), index);
        }
    }

    public static void removeLastPoint() {
        if (!selectedPoints.isEmpty()) {
            selectedPoints.remove(selectedPoints.size() - 1);
        }
    }

    public static List<BlockPos> getSelectedPoints() {
        return selectedPoints;
    }

    public static void setSelectedIndex(int index) {
        selectedIndex = Math.max(1, Math.min(index, selectedPoints.size() + 1));
    }

    public static void resetSelection() {
        selectedPoints.clear();
        selectedIndex = 1;
    }

}
