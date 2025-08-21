package fr.elyrasirapii.road.network;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetRoadHud {
    private final String text; // "" pour effacer

    public PacketSetRoadHud(String text) {
        this.text = text == null ? "" : text;
    }

    public static void encode(PacketSetRoadHud msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.text, 256);
    }

    public static PacketSetRoadHud decode(FriendlyByteBuf buf) {
        return new PacketSetRoadHud(buf.readUtf(256));
    }

    public static void handle(PacketSetRoadHud msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
            return;
        }


        ctx.enqueueWork(() -> fr.elyrasirapii.road.client.hud.RoadHud.setText(msg.text));
        ctx.setPacketHandled(true);
    }
}
