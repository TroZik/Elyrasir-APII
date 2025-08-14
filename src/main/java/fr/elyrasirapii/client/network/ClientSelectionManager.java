package fr.elyrasirapii.client.network;

import fr.elyrasirapii.client.render.DigitParticleRenderer;
import fr.elyrasirapii.parcels.selection.RegionEditMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import fr.elyrasirapii.parcels.selection.ArchitectModeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class ClientSelectionManager {

    private static final EnumMap<RegionEditMode, List<BlockPos>> SELECTIONS =
            new EnumMap<>(RegionEditMode.class);
    private static final EnumMap<RegionEditMode, Integer> SELECTED_INDEX =
            new EnumMap<>(RegionEditMode.class);

    private static int frameCounter = 0;

    static {
        for (RegionEditMode mode : RegionEditMode.values()) {
            SELECTIONS.put(mode, new ArrayList<>());
            SELECTED_INDEX.put(mode, 1); // on commence à 1, comme demandé
        }
    }

    /* ========== Helpers internes ========== */

    private static List<BlockPos> currentList() {
        return SELECTIONS.get(ArchitectModeManager.getMode());
    }

    private static int getIndex() {
        return SELECTED_INDEX.getOrDefault(ArchitectModeManager.getMode(), 1);
    }

    private static void setIndex(int idx) {
        RegionEditMode m = ArchitectModeManager.getMode();
        int max = currentList().size() + 1;
        SELECTED_INDEX.put(m, Math.max(1, Math.min(idx, max)));
    }

    /* ========== API publique (inchangée côté appelants) ========== */

    public static void addPoint(BlockPos pos) {
        List<BlockPos> list = currentList();
        if (list.size() >= 1024) return; // garde-fou générique ; les limites par mode viendront ailleurs
        list.add(pos);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            DigitParticleRenderer.renderDigit(mc.level, pos.above(), list.size() - 1);
        }
    }

    public static void addOrReplacePoint(BlockPos pos) {
        List<BlockPos> list = currentList();
        int idx = getIndex();

        if (idx > 0 && idx <= list.size()) {
            list.set(idx - 1, pos);
        } else {
            list.add(pos);
            setIndex(list.size());
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            int renderIdx = (idx > 0 && idx <= list.size()) ? (idx - 1) : (list.size() - 1);
            DigitParticleRenderer.renderDigit(mc.level, pos.above(), renderIdx);
        }
    }

    public static List<BlockPos> getFullSelection() {
        return Collections.unmodifiableList(currentList());
    }

    public static int size() {
        return currentList().size();
    }

    public static void clear() {
        currentList().clear();
        setIndex(1);
    }

    public static void resetSelection() {
        // réinitialise uniquement le mode courant
        RegionEditMode m = ArchitectModeManager.getMode(); // (tu as renommé getCurrentMode -> getMode)
        SELECTIONS.get(m).clear();
        SELECTED_INDEX.put(m, 1);
    }

    public static void resetSelection(RegionEditMode mode) {
        if (mode == null) return;
        SELECTIONS.get(mode).clear();
        SELECTED_INDEX.put(mode, 1);
    }

    public static void resetAllSelections() {
        for (RegionEditMode m : RegionEditMode.values()) {
            SELECTIONS.get(m).clear();
            SELECTED_INDEX.put(m, 1);
        }
    }

    public static boolean isComplete() {
        return currentList().size() >= 3;
    }

    public static void removeLastPoint() {
        List<BlockPos> list = currentList();
        if (!list.isEmpty()) {
            list.remove(list.size() - 1);
            setIndex(Math.min(getIndex(), list.size() + 1));
        }
    }

    public static void cycleSelectedIndex(int direction) {
        List<BlockPos> list = currentList();
        int max = list.size() + 1;
        int idx = getIndex() + (direction > 0 ? 1 : -1);

        if (idx < 1) idx = max;
        else if (idx > max) idx = 1;

        setIndex(idx);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("Point sélectionné ("
                            + ArchitectModeManager.getMode().displayName() + ") : #" + idx),
                    true
            );
        }
    }

    public static int getSelectedIndex() {
        return getIndex();
    }

    public static void setSelectedIndex(int index) {
        setIndex(index);
    }

    public static List<BlockPos> getSelectedPoints() {
        return currentList();
    }

    /* ========== Rendu particules en boucle : uniquement pour le mode courant ========== */

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<BlockPos> list = currentList();
        if (list.isEmpty()) return;

        frameCounter++;
        if (frameCounter % 10 != 0) return; // toutes ~10 ticks

        for (int i = 0; i < list.size(); i++) {
            BlockPos p = list.get(i);
            DigitParticleRenderer.renderDigit(mc.level, p.above(), i);
        }
    }
}