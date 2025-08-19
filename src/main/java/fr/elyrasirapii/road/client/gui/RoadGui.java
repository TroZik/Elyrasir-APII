package fr.elyrasirapii.road.client.gui;

import net.minecraft.client.Minecraft;

public final class RoadGui {
    private RoadGui() {}

    public static void openNaming() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new NameRoadScreen());
    }
}
