package fr.elyrasirapii.server.utils;

import fr.elyrasirapii.client.network.PacketDisplayTitle;
import fr.elyrasirapii.parcels.ParcelsManager;
import fr.elyrasirapii.server.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Objects;
import java.util.UUID;

public class onServerTick {




    @SubscribeEvent


    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        // System.out.println("[DEBUG] Tick exécuté pour : " + event.player.getName().getString());

        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        BlockPos pos = player.blockPosition();

        String currentParcel = ParcelsManager.get().getParcelAt(pos); // null si rien
        UUID uuid = player.getUUID();
        String lastParcel = ParcelsManager.get().getPlayerParcelMap().get(uuid);


        if (!Objects.equals(currentParcel, lastParcel)) {
            //System.out.println("[DEBUG] Changement détecté : " + lastParcel + " -> " + currentParcel);
            if (lastParcel != null) {
                // Sortie
                PacketHandler.sendToClient((ServerPlayer) player, new PacketDisplayTitle("Vous quittez : " + lastParcel));
            }
            if (currentParcel != null) {
                // Entrée
                PacketHandler.sendToClient((ServerPlayer) player, new PacketDisplayTitle("Vous entrez : " + currentParcel));
            }

            ParcelsManager.get().getPlayerParcelMap().put(uuid, currentParcel);// maj
        }
    }






}
