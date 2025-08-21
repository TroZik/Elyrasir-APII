package fr.elyrasirapii.server.network;

import fr.elyrasirapii.client.network.PacketDisplayTitle;
import fr.elyrasirapii.parcels.network.PacketFinalizeRegionSelection;
import fr.elyrasirapii.parcels.network.PacketParcelsFinalizeSelection;
import fr.elyrasirapii.parcels.network.PacketParcelsResetSelection;
import fr.elyrasirapii.road.network.PacketFinalizeRoadSelection;
import fr.elyrasirapii.road.network.PacketSetRoadHud;
import fr.elyrasirapii.server.utils.laser.PacketSetLaserLines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("elyrasirapii", "main"), // Remplace "examplemod" par ton modid
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {


        CHANNEL.registerMessage(
                nextId(),
                PacketParcelsFinalizeSelection.class,
                PacketParcelsFinalizeSelection::encode,
                PacketParcelsFinalizeSelection::decode,
                PacketParcelsFinalizeSelection::handle
        );


        CHANNEL.registerMessage(
                nextId(),
                PacketParcelsResetSelection.class,
                PacketParcelsResetSelection::toBytes,
                PacketParcelsResetSelection::new,
                PacketParcelsResetSelection::handle
        );

        //packet pour le nom des parcelles/territoire
        CHANNEL.registerMessage(
                nextId(),
                PacketDisplayTitle.class,
                PacketDisplayTitle::encode,
                PacketDisplayTitle::decode,
                PacketDisplayTitle::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                PacketFinalizeRegionSelection.class,
                PacketFinalizeRegionSelection::encode,
                PacketFinalizeRegionSelection::decode,
                PacketFinalizeRegionSelection::handle
        );


        CHANNEL.registerMessage(
                nextId(),
                PacketFinalizeRoadSelection.class,
                PacketFinalizeRoadSelection::encode,
                PacketFinalizeRoadSelection::decode,
                PacketFinalizeRoadSelection::handle
        );


        CHANNEL.registerMessage(
                nextId(),
                PacketSetRoadHud.class,
                PacketSetRoadHud::encode,
                PacketSetRoadHud::decode,
                PacketSetRoadHud::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                PacketSetLaserLines.class,
                PacketSetLaserLines::encode,
                PacketSetLaserLines::decode,
                PacketSetLaserLines::handle
        );

    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToClient(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
