package fr.elyrasirapii.road.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.elyrasirapii.road.utils.RoadLimits;
import fr.elyrasirapii.road.utils.RoadType;
import fr.elyrasirapii.road.client.ClientRoadTypeState;
import fr.elyrasirapii.road.client.RoadSelectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/**
 * Demande un nom à l'utilisateur. Le type de route est implicite (ClientRoadTypeState).
 * Pré-checks:
 *  - au moins MIN_POINTS
 *  - longueur 1..64
 *  - caractères simples (a-z0-9_- espace) -> personnalisable
 *
 * À la validation, on appelle RoadNamingSubmitter.submit(name, type, points)
 * (stub client pour l'instant ; réseau à l'étape suivante).
 */
public class NameRoadScreen extends Screen {

    private static final int MAX_LEN = 64;
    private static final Pattern SAFE = Pattern.compile("[a-zA-Z0-9 _\\-]{1,64}");

    private EditBox nameBox;
    private Button validateBtn;
    private Button cancelBtn;
    private Component errorMsg = Component.empty();

    public NameRoadScreen() {
        super(Component.literal("Nommer la route"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Champ nom
        nameBox = new EditBox(this.font, centerX - 120, centerY - 10, 240, 20, Component.literal("Nom"));
        nameBox.setMaxLength(MAX_LEN);
        nameBox.setFocused(true);
        addRenderableWidget(nameBox);

        // Bouton Valider
        validateBtn = Button.builder(Component.literal("Valider"), b -> onValidate())
                .bounds(centerX - 120, centerY + 20, 110, 20)
                .build();
        addRenderableWidget(validateBtn);

        // Bouton Annuler
        cancelBtn = Button.builder(Component.literal("Annuler"), b -> onCancel())
                .bounds(centerX + 10, centerY + 20, 110, 20)
                .build();
        addRenderableWidget(cancelBtn);

        super.init();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        RenderSystem.enableBlend();

        int centerX = this.width / 2;
        int top = this.height / 2 - 50;

        RoadType type = ClientRoadTypeState.get();
        int count = RoadSelectionManager.size();
        int min   = RoadLimits.MIN_POINTS;
        int max   = RoadLimits.maxPointsFor(type);

        // Titre
        gfx.drawCenteredString(this.font, this.title, centerX, top, 0xFFFFFF);

        // Rappel (type + points)
        gfx.drawCenteredString(this.font,
                "Type: " + type.display() + "   Points: " + count + " (min " + min + ", max " + max + ")",
                centerX, top + 14, 0xAAAAAA);

        // Champ
        nameBox.render(gfx, mouseX, mouseY, partialTick);

        // Message d'erreur
        if (!errorMsg.getString().isEmpty()) {
            gfx.drawCenteredString(this.font, errorMsg, centerX, top + 34 + 20, 0xFF5555);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter -> valider si possible
        if (keyCode == 257 || keyCode == 335) {
            onValidate();
            return true;
        }
        // Escape -> annuler
        if (keyCode == 256) {
            onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onValidate() {
        String name = nameBox.getValue().trim();
        RoadType type = ClientRoadTypeState.get();
        var pts = fr.elyrasirapii.road.client.RoadSelectionManager.getSelectionView();
        int count = pts.size();

        // Pré-checks simples
        if (count < RoadLimits.MIN_POINTS) {
            setError("Sélection insuffisante (" + count + "/" + RoadLimits.MIN_POINTS + ").");
            return;
        }
        if (name.isEmpty()) { setError("Le nom est vide."); return; }
        if (name.length() > MAX_LEN) { setError("Nom trop long (" + name.length() + ">" + MAX_LEN + ")."); return; }
        if (!SAFE.matcher(name).matches()) {
            setError("Caractères non autorisés. Autorisés: a-z A-Z 0-9 espace - _");
            return;
        }

        // ===== Vérifs géométriques des routes =====

        // 1) Auto-intersection XZ avec tolérance Y (ex: tunnel sous une route en hauteur)
        if (fr.elyrasirapii.road.validation.RoadGeometryUtils
                .hasSelfIntersectionXZWithYClearance(pts, fr.elyrasirapii.road.validation.RoadGeomRules.Y_CLEARANCE_BLOCKS)) {
            setError("La route se coupe elle-même (croisement trop proche en Y).");
            return;
        }

        // 2) Pentes par segment (axial <= 45°, diagonal <= 22.5°)
        var slopeFail = fr.elyrasirapii.road.validation.RoadGeometryUtils.firstSlopeViolation(pts);
        if (slopeFail != null && !slopeFail.ok) {
            int seg = slopeFail.indexA + 1; // humain
            double deg = Math.toDegrees(Math.atan(slopeFail.slopeTan));
            double degMax = Math.toDegrees(Math.atan(slopeFail.maxTan));
            setError(String.format("Pente trop forte au segment #%d (%.1f° > %.1f°).", seg, deg, degMax));
            return;
        }

        // 3) Espacement mini intra-route (largeur selon type + marge selon pente locale)
        boolean spacingOk = fr.elyrasirapii.road.validation.RoadGeometryUtils.hasMinSpacingWithinRoadOk(pts, type);
        if (!spacingOk) {
            setError("Segments trop proches entre eux pour ce type de route.");
            return;
        }

        // ===== OK: Soumission client (réseau viendra après) =====
        NameRoadSubmitter.submit(name, type);
        net.minecraft.client.Minecraft.getInstance().setScreen(null);
    }

    private void onCancel() {
        Minecraft.getInstance().setScreen(null);
    }

    private void setError(String msg) {
        this.errorMsg = Component.literal(msg);
    }
}