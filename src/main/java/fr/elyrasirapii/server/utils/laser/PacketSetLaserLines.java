package fr.elyrasirapii.server.utils.laser;


import fr.elyrasirapii.client.render.laser.LaserLine;
import fr.elyrasirapii.client.render.laser.LaserRenderCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSetLaserLines {
    public enum Kind { ROUTE, REGION }
    private final Kind kind;
    private final List<LaserLine> lines;

    public PacketSetLaserLines(Kind kind, List<LaserLine> lines) {
        this.kind = kind; this.lines = lines;
    }

    public static void encode(PacketSetLaserLines msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.kind);
        buf.writeVarInt(msg.lines.size());
        for (LaserLine l : msg.lines) {
            buf.writeDouble(l.x1()); buf.writeDouble(l.y1()); buf.writeDouble(l.z1());
            buf.writeDouble(l.x2()); buf.writeDouble(l.y2()); buf.writeDouble(l.z2());
            buf.writeInt(l.argb());
        }
    }

    public static PacketSetLaserLines decode(FriendlyByteBuf buf) {
        Kind k = buf.readEnum(Kind.class);
        int n = buf.readVarInt();
        List<LaserLine> ls = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double x1 = buf.readDouble(), y1 = buf.readDouble(), z1 = buf.readDouble();
            double x2 = buf.readDouble(), y2 = buf.readDouble(), z2 = buf.readDouble();
            int col = buf.readInt();
            ls.add(new LaserLine(x1,y1,z1,x2,y2,z2,col));
        }
        return new PacketSetLaserLines(k, ls);
    }

    public static void handle(PacketSetLaserLines msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
            ctx.setPacketHandled(true); return;
        }
        ctx.enqueueWork(() -> {
            if (msg.kind == Kind.ROUTE)   LaserRenderCache.setRouteLines(msg.lines);
            else                           LaserRenderCache.setRegionLines(msg.lines);
        });
        ctx.setPacketHandled(true);
    }
}