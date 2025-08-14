package fr.elyrasirapii.parcels.item;

import fr.elyrasirapii.client.network.ClientSelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ArchitectStickItem extends Item {
    public ArchitectStickItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                // Appel indirect à la GUI via une classe client-only
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    fr.elyrasirapii.client.items.ArchitectStickClientHandler.tryOpenNamingScreen();

                });
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (player == null) return InteractionResult.PASS;

        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    fr.elyrasirapii.client.items.ArchitectStickClientHandler.tryOpenNamingScreen();
                });
            } else {
                ClientSelectionManager.addOrReplacePoint(pos);
                player.displayClientMessage(
                        Component.literal("Point " + ClientSelectionManager.getSelectedIndex() + " sélectionné : " + pos.toShortString()),
                        true
                );
            }
        }

        return InteractionResult.SUCCESS;
    }

}
