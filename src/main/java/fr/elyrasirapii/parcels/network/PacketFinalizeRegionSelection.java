package fr.elyrasirapii.parcels.network;

import fr.elyrasirapii.parcels.RegionManager;
import fr.elyrasirapii.parcels.selection.ArchitectModeManager;
import fr.elyrasirapii.parcels.selection.RegionEditMode;
//import fr.elyrasirapii.parcels.validation.*;
import fr.elyrasirapii.server.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketFinalizeRegionSelection {
    private final RegionEditMode mode;
    private final String name;
    private final List<BlockPos> points;

    public PacketFinalizeRegionSelection(RegionEditMode mode, String name, List<BlockPos> points) {
        this.mode = mode;
        this.name = name;
        this.points = points;
    }

    public static void encode(PacketFinalizeRegionSelection msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.mode);
        buf.writeUtf(msg.name);
        buf.writeInt(msg.points.size());
        for (BlockPos pos : msg.points) buf.writeBlockPos(pos);
    }

    public static PacketFinalizeRegionSelection decode(FriendlyByteBuf buf) {
        RegionEditMode mode = buf.readEnum(RegionEditMode.class);
        String name = buf.readUtf(32767);
        int size = buf.readInt();
        List<BlockPos> pts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) pts.add(buf.readBlockPos());
        return new PacketFinalizeRegionSelection(mode, name, pts);
    }

    public static void handle(PacketFinalizeRegionSelection msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            boolean ok = RegionManager.get().finalizeRegionFromClient(player, msg.mode, msg.name, msg.points);
            // Reset la sélection côté client (mode courant) si OK
            if (ok) {
                PacketHandler.sendToClient(player, new PacketParcelsResetSelection());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
