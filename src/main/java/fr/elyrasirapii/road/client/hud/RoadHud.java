package fr.elyrasirapii.road.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoadHud {
    private static String current = "";

    private RoadHud() {}

    /** Définit le texte du HUD ("" pour effacer). Appelé par PacketSetRoadHud. */
    public static void setText(String text) { current = text == null ? "" : text; }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post e) {
        if (current.isEmpty()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics g = e.getGuiGraphics();
        var font = mc.font;
        int x = 6, y = 6;
        g.drawString(font, current, x + 1, y + 1, 0x000000, false);
        g.drawString(font, current, x, y, 0xFFFFFF, false);
    }
}
