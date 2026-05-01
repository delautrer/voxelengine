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

        padding = 10.0f * pixelScale;
        panelW = hotbarWidth + padding * 2;

        float maxSlotY = 168.0f * pixelScale;
        panelH = maxSlotY + padding * 2 + (20.0f * pixelScale);

        guiY = height / 2.0f - panelH / 2.0f + hotbarHeight;

        panelX = guiX - padding;
        panelY = guiY - padding;
    }

    @Override
    protected void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Hintergrund zeichnen
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        // Titel zeichnen
        if (font != null) {
            // Inventar Titel (über den normalen Slots)
            float invTitleY = guiY + (34f * pixelScale) + (3 * 24f * pixelScale) + (2.0f * pixelScale);
            builder.drawText("Inventory", panelX + padding, invTitleY, 0.1f, font);

            // Crafting Titel (ganz oben im Panel)
            float craftTitleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Crafting", panelX + padding + (120f * pixelScale), craftTitleY, 0.1f, font);

            // Ein kleiner Pfeil zwischen Grid und Output (optional, sieht aber gut aus!)
            builder.drawText("->", guiX + (172f * pixelScale), guiY + (136f * pixelScale) + (6f * pixelScale), 0.1f, font);
        }
    }
}