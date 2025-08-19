package fr.elyrasirapii.road.client;

import fr.elyrasirapii.road.utils.RoadLimits;
import fr.elyrasirapii.road.utils.RoadType;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * État local (client) de la sélection de route en cours.
 * - Type courant (Rue/Avenue/Boulevard)
 * - Liste ordonnée de points (BlockPos), immutables à l'ajout
 */
public class ClientRoadSelection {

    private RoadType currentType = RoadType.RUE;
    private final List<BlockPos> points = new ArrayList<>();

    // NEW: index du point "courant" (0..size). size() = emplacement de création.
    private int selectedIndex = 0;

    /* ==== Type & cyclage ==== */

    public RoadType getType() { return currentType; }

    public void setType(RoadType type) {
        if (type == null) return;
        this.currentType = type;
        clampToLimit();
        clampSelectedIndex();
    }

    public void cycleNext() { this.currentType = this.currentType.next(); clampToLimit(); clampSelectedIndex(); }
    public void cyclePrev() { this.currentType = this.currentType.prev(); clampToLimit(); clampSelectedIndex(); }

    /* ==== Points ==== */

    public List<BlockPos> getPointsView() { return Collections.unmodifiableList(points); }
    public int size() { return points.size(); }
    public BlockPos lastPoint() { return points.isEmpty() ? null : points.get(points.size() - 1); }

    public boolean canAddPoint() { return points.size() < RoadLimits.maxPointsFor(currentType); }

    /** Ajoute un point à la fin, et avance le curseur. */
    public boolean addPoint(BlockPos pos) {
        if (pos == null) return false;
        if (!canAddPoint()) return false;
        points.add(pos.immutable());
        // NEW: après ajout, le curseur pointe sur l'emplacement "nouveau" suivant
        selectedIndex = points.size();
        return true;
    }

    /** Remplace le point à l'index si valide. */
    public boolean replacePoint(int index, BlockPos pos) {
        if (pos == null) return false;
        if (index < 0 || index >= points.size()) return false;
        points.set(index, pos.immutable());
        // NEW: après remplacement, on avance d'un cran si possible (comme “passer au point supérieur”)
        selectedIndex = Math.min(points.size(), index + 1);
        return true;
    }

    /** Supprime le dernier point et ajuste le curseur. */
    public BlockPos removeLast() {
        if (points.isEmpty()) return null;
        BlockPos removed = points.remove(points.size() - 1);
        selectedIndex = Math.min(selectedIndex, points.size());
        return removed;
    }

    public void clear() { points.clear(); selectedIndex = 0; }

    /* ==== Sélection de point ==== */

    /** Index du point sélectionné (0..size). size() = emplacement "nouveau". */
    public int getSelectedIndex() { return selectedIndex; }

    /** Fixe l'index de sélection (borné à 0..size). */
    public void setSelectedIndex(int idx) {
        selectedIndex = Math.max(0, Math.min(idx, points.size()));
    }

    /** Move sélection +1 (bornée). */
    public void selectNext() { setSelectedIndex(selectedIndex + 1); }

    /** Move sélection -1 (bornée). */
    public void selectPrev() { setSelectedIndex(selectedIndex - 1); }

    /* ==== Aides ==== */

    private void clampToLimit() {
        int max = RoadLimits.maxPointsFor(currentType);
        if (points.size() > max) {
            points.subList(max, points.size()).clear();
        }
    }

    private void clampSelectedIndex() {
        selectedIndex = Math.max(0, Math.min(selectedIndex, points.size()));
    }

    public boolean isMinimallyValid() { return points.size() >= RoadLimits.MIN_POINTS; }
}
