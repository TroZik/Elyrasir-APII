package fr.elyrasirapii.parcels.network;

import fr.elyrasirapii.parcels.ParcelsManager;
import fr.elyrasirapii.server.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketParcelsFinalizeSelection {
    private final String name;
    private final List<BlockPos> points;

    public PacketParcelsFinalizeSelection(String name, List<BlockPos> points) {
        this.name = name;
        this.points = points;
    }

    public static void encode(PacketParcelsFinalizeSelection msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeInt(msg.points.size());
        for (BlockPos pos : msg.points) {
            buf.writeBlockPos(pos);
        }
    }

    public static PacketParcelsFinalizeSelection decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(32767);
        int size = buf.readInt();
        List<BlockPos> points = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            points.add(buf.readBlockPos());
        }
        return new PacketParcelsFinalizeSelection(name, points);
    }

    public static void handle(PacketParcelsFinalizeSelection msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player != null) {

                // 🔍 DEBUG
                System.out.println("[DEBUG] Serveur : réception du packet PacketFinalizeSelection");
                System.out.println("[DEBUG] Nom de la parcelle : " + msg.name);
                System.out.println("[DEBUG] Nombre de points : " + msg.points.size());
                for (BlockPos pos : msg.points) {
                    System.out.println("[DEBUG] Point reçu : " + pos);
                }

                ParcelsManager.get().finalizeSelectionFromClient(player, msg.name, msg.points);

                if (player instanceof ServerPlayer serverPlayer) {
                    PacketHandler.sendToClient(serverPlayer, new PacketParcelsResetSelection());
                }

            }
        });
        ctx.get().setPacketHandled(true);
    }

}
