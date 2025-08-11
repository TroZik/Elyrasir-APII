package fr.elyrasirapii.client.gui;

import fr.elyrasirapii.client.network.ClientSelectionManager;
import fr.elyrasirapii.parcels.network.PacketParcelsFinalizeSelection;
import fr.elyrasirapii.server.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class NameParcelScreen extends Screen {

    private EditBox textField;

    public NameParcelScreen() {
        super(Component.literal("Nommer la parcelle"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        textField = new EditBox(this.font, centerX - 100, centerY - 20, 200, 20, Component.literal("Nom de la parcelle"));
        this.addRenderableWidget(textField);

        Button confirmButton = Button.builder(Component.literal("Valider"), (btn) -> {
            String name = textField.getValue();
            if (!name.isBlank()) {
                List<BlockPos> points = ClientSelectionManager.getFullSelection();

                System.out.println("[CLIENT GUI] Nom : " + name);
                System.out.println("[CLIENT GUI] Points : " + points.size());

                PacketHandler.sendToServer(new PacketParcelsFinalizeSelection(name, points));
                Minecraft.getInstance().setScreen(null);
            }
        }).bounds(centerX - 50, centerY + 10, 100, 20).build();

        this.addRenderableWidget(confirmButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        textField.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
