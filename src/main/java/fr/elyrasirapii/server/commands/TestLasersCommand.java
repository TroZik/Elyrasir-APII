package fr.elyrasirapii.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import fr.elyrasirapii.client.render.laser.LaserLine;
import fr.elyrasirapii.server.utils.laser.PacketSetLaserLines;
import fr.elyrasirapii.server.network.PacketHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class TestLasersCommand {
    private TestLasersCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("testlasers")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("on").executes(ctx -> on(ctx.getSource())))
                .then(Commands.literal("off").executes(ctx -> off(ctx.getSource()))));
    }

    private static int on(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer sp)) return 0;

        double x = sp.getX(), y = sp.getY() + 1.0, z = sp.getZ();
        List<LaserLine> lines = new ArrayList<>();
        // 3 segments en étoiles autour du joueur
        lines.add(new LaserLine(x - 10, y, z, x + 10, y, z, 0xFFFF0000)); // rouge
        lines.add(new LaserLine(x, y, z - 10, x, y, z + 10, 0xFF00FF00)); // vert
        lines.add(new LaserLine(x - 8, y, z - 8, x + 8, y, z + 8, 0xFF0000FF)); // bleu

        PacketHandler.sendToClient(sp, new PacketSetLaserLines(PacketSetLaserLines.Kind.ROUTE, lines));
        // et vide les REGIONS pour l’instant
        PacketHandler.sendToClient(sp, new PacketSetLaserLines(PacketSetLaserLines.Kind.REGION, List.of()));
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Test lasers ON"), false);
        return 1;
    }

    private static int off(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer sp)) return 0;
        PacketHandler.sendToClient(sp, new PacketSetLaserLines(PacketSetLaserLines.Kind.ROUTE, List.of()));
        PacketHandler.sendToClient(sp, new PacketSetLaserLines(PacketSetLaserLines.Kind.REGION, List.of()));
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Test lasers OFF"), false);
        return 1;
    }
}
