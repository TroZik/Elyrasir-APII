package fr.elyrasirapii.client.render.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LaserRenderer {
    private LaserRenderer() {}

    // épaisseur apparente (en blocs) : demi-écart latéral
    private static final double HALF_OFFSET = 0.1; // monte à 0.08 si tu veux plus épais

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<LaserLine> lines = LaserRenderCache.snapshotAll();
        if (lines.isEmpty()) return;

        PoseStack pose = e.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());

        // Si tu veux forcer l’affichage “par-dessus tout” pour debug:
        // RenderSystem.disableDepthTest();

        for (LaserLine l : lines) {
            int a = (l.argb() >>> 24) & 0xFF;
            int r = (l.argb() >>> 16) & 0xFF;
            int g = (l.argb() >>> 8)  & 0xFF;
            int b = (l.argb())        & 0xFF;

            // direction en XZ
            double dx = l.x2() - l.x1();
            double dz = l.z2() - l.z1();
            double len = Math.hypot(dx, dz);
            double ox = 0.0, oz = 0.0;
            if (len > 1e-6) {
                // vecteur perpendiculaire normalisé en XZ
                ox = -(dz / len) * HALF_OFFSET;
                oz =  (dx / len) * HALF_OFFSET;
            }

            // trois traits parallèles : -offset, 0, +offset
            //drawLine(vc, pose, l.x1() - ox, l.y1(), l.z1() - oz, l.x2() - ox, l.y2(), l.z2() - oz, r,g,b,a);
            drawLine(vc, pose, l.x1()      , l.y1(), l.z1()      , l.x2()      , l.y2(), l.z2()      , r,g,b,a);
           // drawLine(vc, pose, l.x1() + ox, l.y1(), l.z1() + oz, l.x2() + ox, l.y2(), l.z2() + oz, r,g,b,a);
        }

        buffers.endBatch(RenderType.lines());
        // RenderSystem.enableDepthTest();

        pose.popPose();
    }

    private static void drawLine(VertexConsumer vc, PoseStack pose,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 int r, int g, int b, int a) {
        vc.vertex(pose.last().pose(), (float)x1, (float)y1, (float)z1)
                .color(r,g,b,a).normal(0,1,0).endVertex();
        vc.vertex(pose.last().pose(), (float)x2, (float)y2, (float)z2)
                .color(r,g,b,a).normal(0,1,0).endVertex();
    }
}
