package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.gui.container.FurnaceContainer;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.blocks.entities.FurnaceBlockEntity;
import de.delautrer.game.items.ItemStack;

public class FurnaceScreen extends ContainerScreen {
    private float panelX, panelY, panelW, panelH, padding;

    public FurnaceScreen(FurnaceContainer container) {
        super(container);
    }

    @Override
    protected void onInit() {
        slotSize = 24f * pixelScale;

        float hotbarWidth = 24f * 9f * pixelScale;
        float containerPixelHeight = 188f * pixelScale;

        guiX = (float) Math.floor((width - hotbarWidth) / 2.0f);
        guiY = (height - containerPixelHeight) / 2.0f;

        padding = 10.0f * pixelScale;
        panelW = hotbarWidth + padding * 2;
        panelH = containerPixelHeight + padding * 2 + (15.0f * pixelScale);
        panelX = guiX - padding;
        panelY = guiY - padding;
    }

    @Override
    protected void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Hintergrund zeichnen
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        FurnaceContainer furnaceContainer = (FurnaceContainer) getContainer();
        FurnaceBlockEntity furnace = furnaceContainer.getFurnaceInventory().getFurnace();

        int burnTime = furnace.getBurnTime();
        int maxBurnTime = furnace.getMaxBurnTime();
        int cookTime = furnace.getCookTime();
        int maxCookTime = furnace.getMaxCookTime();

        // 1. Brennstoff-Flamme zeichnen (zwischen Slot 1 und 0)
        float flameX = guiX + 54f * pixelScale;
        float flameY = guiY + 132f * pixelScale;
        float flameW = 24f * pixelScale;
        float flameH = 16f * pixelScale;

        // Dunkler Hintergrund für die Flamme
        builder.addRect(flameX, flameY, 0.05f, flameW, flameH, 0.15f, 0.15f, 0.15f, 1.0f);

        if (burnTime > 0 && maxBurnTime > 0) {
            float fuelRatio = burnTime / (float) maxBurnTime;
            float litH = flameH * fuelRatio;
            // Orange-rote Flamme (oben verankert, schrumpft nach oben)
            builder.addRect(flameX, flameY, 0.06f, flameW, litH, 1.0f, 0.18f, 0.0f, 1.0f);
        }

        // 2. Fortschritts-Pfeil/Balken zeichnen (zwischen den Spalten)
        float arrowX = guiX + 84f * pixelScale;
        float arrowY = guiY + 128f * pixelScale;
        float arrowW = 24f * pixelScale;

        // Dunkler Balken-Hintergrund
        builder.addRect(arrowX, arrowY + 4f * pixelScale, 0.05f, arrowW, 8f * pixelScale, 0.15f, 0.15f, 0.15f, 1.0f);

        if (cookTime > 0) {
            float cookRatio = cookTime / (float) maxCookTime;
            // Grüner Fortschritt
            builder.addRect(arrowX, arrowY + 4f * pixelScale, 0.06f, arrowW * cookRatio, 8f * pixelScale, 0.0f, 1.0f, 0.12f, 1.0f);
        }

        if (font != null) {
            float titleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Furnace", panelX + padding, titleY, 0.1f, font);

            // Pfeiltext zeichnen
            builder.drawText("->", arrowX + 5f * pixelScale, arrowY + 11f * pixelScale, 0.07f, font);
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        super.render(builder, mouseX, mouseY);

        // Hover-Tooltip für die Flamme
        float flameX = guiX + 54f * pixelScale;
        float flameY = guiY + 132f * pixelScale;
        float flameW = 24f * pixelScale;
        float flameH = 16f * pixelScale;

        float invertedMouseY = height - mouseY;
        if (mouseX >= flameX && mouseX <= flameX + flameW &&
                invertedMouseY >= flameY && invertedMouseY <= flameY + flameH) {

            FurnaceContainer furnaceContainer = (FurnaceContainer) getContainer();
            FurnaceBlockEntity furnace = furnaceContainer.getFurnaceInventory().getFurnace();
            int burnTime = furnace.getBurnTime();

            // Treibstoff im Slot berechnen
            int totalFuelBurnTime = 0;
            ItemStack fuelStack = furnace.getInventory().getStack(1);
            if (fuelStack != null) {
                int fuelBurn = de.delautrer.game.crafting.FurnaceRecipeManager.getBurnTime(fuelStack.type);
                totalFuelBurnTime = fuelBurn * fuelStack.amount;
            }

            float currentSec = burnTime / 20.0f;
            float backupSec = totalFuelBurnTime / 20.0f;
            String tooltipText = String.format("Burn time: %.1fs (+%.1fs)", currentSec, backupSec);

            drawTooltip(builder, tooltipText, mouseX, mouseY);
        }
    }
}
