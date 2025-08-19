package fr.elyrasirapii.road.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** SHIFT + clic gauche : supprime le dernier point (pour le RoadType courant). */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRoadClickHandler {

    private ClientRoadClickHandler() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) return;

        var mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;

        long window = mc.getWindow().getWindow();
        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT)  == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shiftDown) return;

        if (!ClientRoadInputHandler.isHoldingRoadStick(p)) return;

        int before = RoadSelectionManager.size();
        if (before <= 0) return;

        RoadSelectionManager.removeLastPoint();
        int after = RoadSelectionManager.size();

        p.displayClientMessage(Component.literal("§cPoint supprimé. Restants: " + after), true);
        event.setCanceled(true);
    }
}
