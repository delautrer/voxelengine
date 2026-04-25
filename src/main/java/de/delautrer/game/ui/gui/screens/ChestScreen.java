package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.gui.container.ChestContainer;
import de.delautrer.game.ui.UIMeshBuilder;

public class ChestScreen extends ContainerScreen {

    private float panelX, panelY, panelW, panelH, padding;

    public ChestScreen(ChestContainer container) {
        super(container);
    }

    @Override
    protected void onInit() {
        slotSize = 24f * pixelScale;

        float hotbarWidth = 24f * 9f * pixelScale;

        // Gesamte Höhe des Containers intern berechnen:
        // Kiste (3x24) + Abstand (14) + SpielerGrid (3x24) + Abstand (6) + Hotbar (24) = 188px
        float containerPixelHeight = 188f * pixelScale;

        guiX = (float) Math.floor((width - hotbarWidth) / 2.0f);
        guiY = (height - containerPixelHeight) / 2.0f; // Mittig zentrieren

        padding = 10.0f * pixelScale;
        panelW = hotbarWidth + padding * 2;
        panelH = containerPixelHeight + padding * 2 + (15.0f * pixelScale); // + 15px für Textplatz
        panelX = guiX - padding;
        panelY = guiY - padding;
    }

    @Override
    protected void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY) {
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        if (font != null) {
            float titleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Chest", panelX + padding, titleY, 0.1f, font);

            // "Inventory" Text über dem Spieler-Bereich
            float invTextY = guiY + (3 * 24f + 14f) * pixelScale + (84f * pixelScale); // Höhe ausrechnen
            // builder.drawText("Inventory", panelX + padding, invTextY, 0.1f, font);
        }
    }
}