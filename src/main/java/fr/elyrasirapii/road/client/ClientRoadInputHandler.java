package fr.elyrasirapii.road.client;

import fr.elyrasirapii.road.utils.RoadType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * - CTRL + molette : change RoadType courante
 * - SHIFT + molette : navigue index points (1..N + N+1 "nouveau") pour le RoadType courant
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRoadInputHandler {

    private ClientRoadInputHandler() {}

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!isHoldingRoadStick(player)) return;

        long window = mc.getWindow().getWindow();
        boolean ctrlDown  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL)  == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT)    == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT)   == GLFW.GLFW_PRESS;

        double delta = event.getScrollDelta();
        if (delta == 0) return;

        if (ctrlDown && !shiftDown) {
            // Cycle type
            RoadType before = ClientRoadTypeState.get();
            if (delta < 0) ClientRoadTypeState.next(); else ClientRoadTypeState.prev();
            RoadType now = ClientRoadTypeState.get();

            // feedback + l'index existant du type courant reste tel quel (sélections mémorisées par type)
            player.displayClientMessage(Component.literal("§bType: " + now.display()), true);
            event.setCanceled(true);
            return;
        }

        if (shiftDown && !ctrlDown) {
            // Navigue points pour le type courant
            RoadSelectionManager.cycleSelectedIndex(delta < 0 ? +1 : -1);
            event.setCanceled(true);
        }
    }

    /** Check provisoire tant que l'item n'est pas au registry. */
    public static boolean isHoldingRoadStick(Player p) {
        return isRoadStick(p.getMainHandItem()) || isRoadStick(p.getOffhandItem());
    }

    private static boolean isRoadStick(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String key = stack.getDescriptionId(); // ex: "item.elyrasirapii.road_stick"
        return key != null && key.endsWith(".road_stick");
    }
}
