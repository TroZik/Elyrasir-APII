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
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        BlockPos pos = player.blockPosition();
        UUID uuid = player.getUUID();

        String currentParcel = ParcelsManager.get().getParcelAt(pos);   // peut contenir "@fichier"
        String lastParcel    = ParcelsManager.get().getPlayerParcelMap().get(uuid);

        // On normalise juste pour l’AFFICHAGE (nom sans l’@...).
        String currDisplay = displayOnlyName(currentParcel);
        String lastDisplay = displayOnlyName(lastParcel);

        if (!Objects.equals(currentParcel, lastParcel)) {
            if (lastParcel != null) {
                PacketHandler.sendToClient((ServerPlayer) player,
                        new PacketDisplayTitle("Vous quittez : " + lastDisplay));
            }
            if (currentParcel != null) {
                PacketHandler.sendToClient((ServerPlayer) player,
                        new PacketDisplayTitle("Vous entrez : " + currDisplay));
            }
            // On stocke l’identifiant complet pour la logique interne
            ParcelsManager.get().getPlayerParcelMap().put(uuid, currentParcel);
        }
    }

    private static String displayOnlyName(String raw) {
        if (raw == null) return null;
        int at = raw.indexOf('@');
        return at >= 0 ? raw.substring(0, at) : raw;
    }





}
