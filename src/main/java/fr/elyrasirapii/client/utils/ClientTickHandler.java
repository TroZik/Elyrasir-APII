package fr.elyrasirapii.client.utils;

import fr.elyrasirapii.client.network.ClientSelectionManager;
import fr.elyrasirapii.road.client.RoadSelectionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "elyrasirapii", value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.ClientTickEvent.Phase.END) {
            ClientSelectionManager.tick();         // pour ArchitectStick
           RoadSelectionManager.tick();           // pour RoadStick
        }
    }
}
