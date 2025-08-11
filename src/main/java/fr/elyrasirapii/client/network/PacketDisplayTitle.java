package fr.elyrasirapii.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketDisplayTitle {
    private final String message;

    public PacketDisplayTitle(String message) {
        this.message = message;
    }

    public static void encode(PacketDisplayTitle msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.message);
    }

    public static PacketDisplayTitle decode(FriendlyByteBuf buf) {
        return new PacketDisplayTitle(buf.readUtf(32767));
    }

    public static void handle(PacketDisplayTitle msg, Supplier<NetworkEvent.Context> ctx) {
        //   System.out.println("[CLIENT] Réception message : " + msg.message);
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(msg.message), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

