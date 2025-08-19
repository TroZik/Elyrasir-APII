package fr.elyrasirapii.road.item;

import fr.elyrasirapii.road.utils.RoadLimits;
import fr.elyrasirapii.road.client.ClientRoadInputHandler;
import fr.elyrasirapii.road.client.ClientRoadTypeState;
import fr.elyrasirapii.road.client.RoadSelectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * RoadStick (client-only pour l’instant).
 * - Right click (sans Sneak) : add/replace point via RoadSelectionManager (par type courant).
 * - Shift + Right click : réservé au GUI (étape 2) -> pass.
 */
public class RoadStickItem extends Item {

    public RoadStickItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // --- Shift + clic droit : ouvrir le GUI (client), avec pré-checks ---
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                return InteractionResultHolder.pass(stack); // côté serveur: no-op
            }
            // pré-check minimal (évite d'ouvrir si < 2 points)
            if (!fr.elyrasirapii.road.client.RoadSelectionManager.isMinimallyValid()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§eSélection insuffisante (au moins "
                                + fr.elyrasirapii.road.utils.RoadLimits.MIN_POINTS + " points)."), true);
                return InteractionResultHolder.success(stack);
            }

            // ouvrir l'écran
            fr.elyrasirapii.road.client.gui.RoadGui.openNaming();
            return InteractionResultHolder.success(stack);
        }

        // --- reste: clic droit normal (ajout/remplacement) ---
        if (!level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }
        if (!fr.elyrasirapii.road.client.ClientRoadInputHandler.isHoldingRoadStick(player)) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult hit = currentOrDoRaycast(player, 64.0D);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cAucun bloc visé."), true);
            return InteractionResultHolder.success(stack);
        }

        int size = fr.elyrasirapii.road.client.RoadSelectionManager.size();
        int max  = fr.elyrasirapii.road.utils.RoadLimits.maxPointsFor(fr.elyrasirapii.road.client.ClientRoadTypeState.get());
        if (fr.elyrasirapii.road.client.RoadSelectionManager.getSelectedIndex() == size + 1 && size >= max) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§eLimite atteinte (" + max + " points) pour "
                            + fr.elyrasirapii.road.client.ClientRoadTypeState.get().display()), true);
            return InteractionResultHolder.success(stack);
        }

        fr.elyrasirapii.road.client.RoadSelectionManager.addOrReplacePoint(hit.getBlockPos());
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§aPoint ok ["
                        + fr.elyrasirapii.road.client.RoadSelectionManager.size() + "] ("
                        + fr.elyrasirapii.road.client.ClientRoadTypeState.get().display() + ")"), true
        );

        return InteractionResultHolder.success(stack);
    }


    private BlockHitResult currentOrDoRaycast(Player player, double reach) {
        HitResult global = Minecraft.getInstance().hitResult;
        if (global instanceof BlockHitResult bhr && global.getType() == HitResult.Type.BLOCK) {
            return bhr;
        }
        return doRaycast(player, reach);
    }

    private BlockHitResult doRaycast(Player player, double reach) {
        var from = player.getEyePosition(1.0F);
        var to   = from.add(player.getLookAngle().scale(reach));
        var ctx  = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        return player.level().clip(ctx);
    }
}
