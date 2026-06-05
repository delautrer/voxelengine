package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.gui.container.CraftingTableContainer;
import de.delautrer.game.ui.UIMeshBuilder;

public class CraftingTableScreen extends ContainerScreen {
    private float panelX, panelY, panelW, panelH, padding;

    public CraftingTableScreen(CraftingTableContainer container) {
        super(container);
    }

    @Override
    protected void onInit() {
        slotSize = 24f * pixelScale;

        float hotbarWidth = 24f * 9f * pixelScale;
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
            builder.drawText("Crafting Table", panelX + padding, titleY, 0.1f, font);

            // Pfeil zeichnen
            builder.drawText("->", guiX + (114f * pixelScale), guiY + (140f * pixelScale) + (6f * pixelScale), 0.1f, font);
        }
    }
}
