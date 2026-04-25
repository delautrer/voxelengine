package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.gui.container.PlayerContainer;
import de.delautrer.game.ui.UIMeshBuilder;

public class InventoryScreen extends ContainerScreen {

    private float panelX, panelY, panelW, panelH, padding;

    public InventoryScreen(PlayerContainer container) {
        super(container);
    }

    @Override
    protected void onInit() {
        slotSize = 24f * pixelScale;

        float hotbarWidth = 24f * 9f * pixelScale;
        float hotbarHeight = 24f * pixelScale;

        // guiX und guiY legen fest, wo unser Container (Slot X/Y = 0) beginnt.
        guiX = (float) Math.floor((width - hotbarWidth) / 2.0f);
        guiY = height / 2.0f - hotbarHeight * 2.0f;

        // Panel Bounds berechnen (nur für den Hintergrund wichtig)
        padding = 10.0f * pixelScale;
        panelW = hotbarWidth + padding * 2;
        // Höhe: 3 Reihen à 24px + 34px Abstand + Padding
        panelH = (34f * pixelScale + 3 * hotbarHeight) + padding * 2 + (15.0f * pixelScale);
        panelX = guiX - padding;
        panelY = guiY - padding;
    }

    @Override
    protected void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Hintergrund
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        // Titel
        if (font != null) {
            float titleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Inventory", panelX + padding, titleY, 0.1f, font);
        }
    }
}