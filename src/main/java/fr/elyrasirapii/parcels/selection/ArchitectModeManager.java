package fr.elyrasirapii.parcels.selection;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ArchitectModeManager {private static RegionEditMode current = RegionEditMode.PARCEL;

    public static RegionEditMode getMode() {
        return current;
    }

    public static void setMode(RegionEditMode mode) {
        current = mode;
        showHudMode();
    }

    public static void nextMode() {
        RegionEditMode[] vals = RegionEditMode.values();
        int idx = (current.ordinal() + 1) % vals.length;
        current = vals[idx];
        showHudMode();
    }

    public static void prevMode() {
        RegionEditMode[] vals = RegionEditMode.values();
        int idx = (current.ordinal() - 1 + vals.length) % vals.length;
        current = vals[idx];
        showHudMode();
    }

    public static int getMaxPointsForCurrentMode() {
        return switch (current) {
            case PARCEL -> 16;
            case DISTRICT, CITY, TERRITORY -> 1024;
        };
    }

    private static void showHudMode() {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("Mode: " + current.displayName()),
                    true
            );
        }
    }
}
