package fr.elyrasirapii.client.utils;

import fr.elyrasirapii.client.network.ClientSelectionManager;
import fr.elyrasirapii.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "elyrasirapii", value = Dist.CLIENT)
public class ClientInputHandler {
    private static boolean wasLeftClickPressed = false;

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) return;

        // Vérifie que le joueur tient bien l'ArchitectStick
        ItemStack heldItem = mc.player.getMainHandItem();
        if (!heldItem.is(ModItems.ARCHITECT_STICK.get())) return;

        // Vérifie si CTRL est pressé
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean isCtrlDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

        if (!isCtrlDown) return;

        // Consomme l'événement pour éviter le changement de hotbar
        event.setCanceled(true);

        double scrollDelta = event.getScrollDelta();
        if (scrollDelta == 0) return;

        int direction = scrollDelta > 0 ? 1 : -1;
        ClientSelectionManager.cycleSelectedIndex(direction);

        // Ramène la sélection sur l’ArchitectStick (au cas où)
        mc.player.getInventory().selected = mc.player.getInventory().findSlotMatchingItem(heldItem);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Vérifie que le joueur tient bien l'ArchitectStick
        ItemStack heldItem = mc.player.getMainHandItem();
        if (!heldItem.is(ModItems.ARCHITECT_STICK.get())) return;

        long window = mc.getWindow().getWindow();
        boolean isCtrlDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

        boolean isLeftClickPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (isCtrlDown && isLeftClickPressed && !wasLeftClickPressed) {
            if (!ClientSelectionManager.getSelectedPoints().isEmpty()) {
                int removedIndex = ClientSelectionManager.getSelectedPoints().size();
                ClientSelectionManager.removeLastPoint();
                ClientSelectionManager.setSelectedIndex(removedIndex); // Revenir sur point à poser
                mc.player.displayClientMessage(Component.literal("§cPoint " + removedIndex + " supprimé."), true);
            } else {
                mc.player.displayClientMessage(Component.literal("§7Aucun point à supprimer."), true);
            }
        }

        wasLeftClickPressed = isLeftClickPressed;
    }
}
