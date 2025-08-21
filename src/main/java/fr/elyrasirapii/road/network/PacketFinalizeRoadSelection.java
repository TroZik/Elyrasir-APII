package fr.elyrasirapii.road.network;

import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.road.server.RoadServerSaver;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketFinalizeRoadSelection {
    private final String name;
    private final RoadType type;
    private final List<BlockPos> points;

    public PacketFinalizeRoadSelection(String name, RoadType type, List<BlockPos> points) {
        this.name = name;
        this.type = type;
        this.points = points;
    }

    public static void encode(PacketFinalizeRoadSelection msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name, 128);
        buf.writeEnum(msg.type);
        buf.writeVarInt(msg.points.size());
        for (BlockPos p : msg.points) buf.writeBlockPos(p);
    }

    public static PacketFinalizeRoadSelection decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(128);
        RoadType type = buf.readEnum(RoadType.class);
        int n = buf.readVarInt();
        List<BlockPos> pts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) pts.add(buf.readBlockPos());
        return new PacketFinalizeRoadSelection(name, type, pts);
    }

    public static void handle(PacketFinalizeRoadSelection msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var sender = ctx.getSender(); // null en client -> ici on est C2S donc ok
            if (sender == null) return;
            var server = sender.getServer();

            // Sauvegarde serveur (pas de double vérif, usage admin)
            RoadServerSaver.saveRoute(server, sender, msg.name, msg.type, msg.points);
        });
        ctx.setPacketHandled(true);
    }
}
