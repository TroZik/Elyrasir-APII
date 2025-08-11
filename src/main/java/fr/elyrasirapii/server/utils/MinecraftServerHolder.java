package fr.elyrasirapii.server.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MinecraftServerHolder {
    public static MinecraftServer INSTANCE;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        INSTANCE = event.getServer();
    }
}

