package fr.elyrasirapii.parcels.network;

import fr.elyrasirapii.client.network.ClientSelectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketParcelsResetSelection {
    public PacketParcelsResetSelection() {}

    public PacketParcelsResetSelection(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            resetClientSelection();
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void resetClientSelection() {
        ClientSelectionManager.resetSelection();
    }
}
