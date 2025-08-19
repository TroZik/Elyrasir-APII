package fr.elyrasirapii.road.client;

import fr.elyrasirapii.client.render.DigitParticleRenderer;
import fr.elyrasirapii.road.utils.RoadLimits;
import fr.elyrasirapii.road.utils.RoadType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Sélection client dédiée aux ROUTES (une sélection par RoadType).
 * - Index 1..N pour cibler un point existant, N+1 pour "nouveau".
 * - Rendu digits via DigitParticleRenderer dans tick().
 */
public final class RoadSelectionManager {

    private static final EnumMap<RoadType, List<BlockPos>> SELECTIONS = new EnumMap<>(RoadType.class);
    private static final EnumMap<RoadType, Integer> SELECTED_INDEX   = new EnumMap<>(RoadType.class);

    private static int frameCounter = 0;

    static {
        for (RoadType t : RoadType.values()) {
            SELECTIONS.put(t, new ArrayList<>());
            SELECTED_INDEX.put(t, 1); // on commence à 1 (humain), N+1 = "nouveau"
        }
    }

    private RoadSelectionManager() {}

    /* ===== Helpers internes liés au type courant ===== */

    private static RoadType mode() {
        return ClientRoadTypeState.get();
    }

    private static List<BlockPos> list() {
        return SELECTIONS.get(mode());
    }

    private static int getIndex() {
        return SELECTED_INDEX.getOrDefault(mode(), 1);
    }

    private static void setIndex(int idx) {
        List<BlockPos> l = list();
        int max = l.size() + 1;                   // N+1 = "nouveau"
        SELECTED_INDEX.put(mode(), Math.max(1, Math.min(idx, max)));
    }

    /* ===== API publique (appel par item/inputs/rendu) ===== */

    /** Ajoute ou remplace le point selon l'index courant (1..N = replace, N+1 = add). */
    public static void addOrReplacePoint(BlockPos pos) {
        if (pos == null) return;
        List<BlockPos> l = list();
        int idx = getIndex(); // 1..N ou N+1

        if (idx > 0 && idx <= l.size()) {
            // replace
            l.set(idx - 1, pos.immutable());
            // après replace : passer au point suivant si possible
            setIndex(idx + 1);
        } else {
            // add
            if (l.size() >= RoadLimits.maxPointsFor(mode())) return;
            l.add(pos.immutable());
            setIndex(l.size() + 1); // curseur sur "nouveau"
        }

        // feedback visuel immédiat
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            int renderIdx = Math.min(idx - 1, l.size() - 1);
            if (renderIdx >= 0) {
                DigitParticleRenderer.renderDigit(mc.level, pos.above(), renderIdx);
            }
        }
    }

    /** Supprime le dernier point de la sélection du type courant. */
    public static void removeLastPoint() {
        List<BlockPos> l = list();
        if (l.isEmpty()) return;
        l.remove(l.size() - 1);
        setIndex(Math.min(getIndex(), l.size() + 1));
    }

    /** Fait défiler l'index sélectionné : +1 (vers la droite / bas), -1 (vers la gauche / haut). */
    public static void cycleSelectedIndex(int direction) {
        List<BlockPos> l = list();
        int max = l.size() + 1;
        int idx = getIndex() + (direction > 0 ? 1 : -1);

        if (idx < 1) idx = max;
        else if (idx > max) idx = 1;

        setIndex(idx);

        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("Point sélectionné (" + mode().display() + ") : #" + idx), true
            );
        }
    }

    public static int getSelectedIndex() { return getIndex(); }

    /** Redéfinit l'index sélectionné (1..N+1). */
    public static void setSelectedIndex(int index) { setIndex(index); }

    public static int size() { return list().size(); }

    public static List<BlockPos> getSelectionView() {
        return Collections.unmodifiableList(list());
    }

    /** Réinitialise la sélection du type donné. */
    public static void resetSelection(RoadType type) {
        if (type == null) return;
        SELECTIONS.get(type).clear();
        SELECTED_INDEX.put(type, 1);
    }

    /** Réinitialise toutes les sélections (tous types). */
    public static void resetAll() {
        for (RoadType t : RoadType.values()) {
            resetSelection(t);
        }
    }

    /** True si la sélection courante a au moins MIN_POINTS. */
    public static boolean isMinimallyValid() {
        return size() >= RoadLimits.MIN_POINTS;
    }

    /* ===== Tick rendu digits : uniquement pour le type courant ===== */

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<BlockPos> l = list();
        if (l.isEmpty()) return;

        frameCounter++;
        if (frameCounter % 10 != 0) return; // ~toutes 10 ticks

        for (int i = 0; i < l.size(); i++) {
            DigitParticleRenderer.renderDigit(mc.level, l.get(i).above(), i);
        }
    }
}
