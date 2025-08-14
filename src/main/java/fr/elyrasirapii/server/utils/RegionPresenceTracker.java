package fr.elyrasirapii.server.utils;

import fr.elyrasirapii.server.regions.RegionLookup;
import fr.elyrasirapii.server.regions.RegionLookup.RegionAt;
import fr.elyrasirapii.server.network.PacketHandler;
import fr.elyrasirapii.client.network.PacketDisplayTitle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "elyrasirapii") // adapte si différent
public final class RegionPresenceTracker {

    private static final Map<UUID, String> lastTerritory = new HashMap<>();
    private static final Map<UUID, String> lastCity      = new HashMap<>();
    private static final Map<UUID, String> lastDistrict  = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide)   return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        var server = sp.getServer();
        if (server == null) return;

        BlockPos pos = sp.blockPosition();
        RegionAt now = RegionLookup.findAt(server, pos);

        UUID id = sp.getUUID();

        String prevTerr = lastTerritory.get(id);
        String prevCity = lastCity.get(id);
        String prevDist = lastDistrict.get(id);

        String curTerr = now.territoryOrNull();
        String curCity = now.cityOrNull();
        String curDist = now.districtOrNull();

        // DISTRICT change
        if (!Objects.equals(prevDist, curDist)) {
            if (prevDist != null) {
                PacketHandler.sendToClient(sp, new PacketDisplayTitle("Vous quittez le district : " + prevDist));
            }
            if (curDist != null) {
                PacketHandler.sendToClient(sp, new PacketDisplayTitle("Vous entrez dans le district : " + curDist));
            }
            lastDistrict.put(id, curDist);
        }

        // CITY change (indépendant du district pour gérer le cas “aucun district”)
        if (!Objects.equals(prevCity, curCity)) {
            if (prevCity != null) {
                PacketHandler.sendToClient(sp, new PacketDisplayTitle("Vous quittez la ville : " + prevCity));
            }
            if (curCity != null) {
                PacketHandler.sendToClient(sp, new PacketDisplayTitle("Vous entrez dans la ville : " + curCity));
            }
            lastCity.put(id, curCity);
        }

        // TERRITORY change
        if (!Objects.equals(prevTerr, curTerr)) {
            if (prevTerr != null) {
                PacketHandler.sendToClient(sp, new PacketDisplayTitle("Vous quittez le territoire : " + prevTerr));
            }
            if (curTerr != null) {
                PacketHandler.sendToClient(sp, new PacketDisplayTitle("Vous entrez dans le territoire : " + curTerr));
            }
            lastTerritory.put(id, curTerr);
        }
    }
}
