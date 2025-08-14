package fr.elyrasirapii.client.gui;

import fr.elyrasirapii.parcels.selection.ArchitectModeManager;
import fr.elyrasirapii.parcels.selection.RegionEditMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NameRegionScreen extends Screen {

    private final RegionEditMode mode;
    private EditBox nameField;

    public NameRegionScreen(RegionEditMode mode) {
        super(Component.literal(switch (mode) {
            case DISTRICT -> "Nommer le district";
            case CITY -> "Nommer la ville";
            case TERRITORY -> "Nommer le territoire";
            default -> "Nommer";
        }));
        this.mode = mode;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        nameField = new EditBox(this.font, cx - 120, cy - 20, 240, 20,
                Component.literal("Nom"));
        nameField.setMaxLength(64);
        this.addRenderableWidget(nameField);

        Button confirm = Button.builder(Component.literal("Valider"), (btn) -> {
            String name = nameField.getValue().trim();
            if (name.isEmpty()) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("§cVeuillez entrer un nom."), true
                    );
                }
                return;
            }
            // Récupère les points du mode courant (même source que pour les parcelles, mais c’est le set du mode en cours)
            java.util.List<net.minecraft.core.BlockPos> points =
                    fr.elyrasirapii.client.network.ClientSelectionManager.getFullSelection();

            // Envoi au serveur
            fr.elyrasirapii.server.network.PacketHandler.sendToServer(
                    new fr.elyrasirapii.parcels.network.PacketFinalizeRegionSelection(this.mode, name, points)
            );
            System.out.println("[CLIENT] Region naming confirmed. Mode=" + mode + " Name=" + name);

            Minecraft.getInstance().setScreen(null);
        }).bounds(cx - 50, cy + 10, 100, 20).build();

        this.addRenderableWidget(confirm);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        nameField.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
