package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.gui.container.FurnaceContainer;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.gui.InventoryConstants;
import de.delautrer.game.blocks.entities.FurnaceBlockEntity;
import de.delautrer.game.items.ItemStack;

public class FurnaceScreen extends ContainerScreen {
    private float panelX, panelY, panelW, panelH, padding;

    public FurnaceScreen(FurnaceContainer container) {
        super(container);
    }

    @Override
    protected void onInit() {
        slotSize = InventoryConstants.SLOT_SIZE * pixelScale;

        float hotbarWidth = InventoryConstants.SLOT_SIZE * 9f * pixelScale;
        float containerPixelHeight = 188f * pixelScale;

        guiX = (float) Math.floor((width - hotbarWidth) / 2.0f);
        guiY = (height - containerPixelHeight) / 2.0f;

        padding = 10.0f * pixelScale;
        panelW = hotbarWidth + padding * 2;
        panelH = containerPixelHeight + padding * 2 + (15.0f * pixelScale);
        
        panelX = (float) Math.floor((width - panelW) / 2.0f);
        panelY = (float) Math.floor((height - panelH) / 2.0f);
        
        guiX = panelX + padding;
        guiY = panelY + padding;
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
        float flameX = guiX + 70f * pixelScale;
        float flameY = guiY + 144f * pixelScale;
        float flameW = 16f * pixelScale;
        float flameH = 16f * pixelScale;

        // Hintergrund-Textur (GridX=1, GridY=15, flipV=true da Vulkan Y invertiert)
        builder.addAtlasQuad(flameX, flameY, 0.05f, flameW, flameH, 1, 15, 1, 1, true);

        if (burnTime > 0 && maxBurnTime > 0) {
            float fuelRatio = burnTime / (float) maxBurnTime;
            float litH = flameH * fuelRatio;
            
            if (litH > 0) {
                // Zeichne die farbige Textur (X:0, Y:15) abgeschnitten (clipped) von unten nach oben
                builder.addAtlasQuadClippedV(flameX, flameY, 0.06f, flameW, litH, flameH, 0, 15, 1, 1, true);
            }
        }

        // 2. Fortschritts-Pfeil/Balken zeichnen (zwischen den Spalten)
        float arrowX = guiX + 100f * pixelScale;
        float arrowY = guiY + 144f * pixelScale;
        float arrowW = 16f * pixelScale;
        float arrowH = 16f * pixelScale;

        // Pfeil-Hintergrund (GridX=3, GridY=15)
        builder.addAtlasQuad(arrowX, arrowY, 0.05f, arrowW, arrowH, 3, 15, 1, 1, true);

        if (cookTime > 0 && maxCookTime > 0) {
            float cookRatio = cookTime / (float) maxCookTime;
            float fillW = arrowW * cookRatio;
            
            if (fillW > 0) {
                // Gefüllter Pfeil (GridX=2, GridY=15) abgeschnitten von links nach rechts
                builder.addAtlasQuadClippedH(arrowX, arrowY, 0.06f, fillW, arrowW, arrowH, 2, 15, 1, 1, true);
            }
        }

        if (font != null) {
            float titleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Furnace", panelX + padding, titleY, 0.1f, font);

            // Pfeiltext zeichnen
            //builder.drawText("->", arrowX + 5f * pixelScale, arrowY + 11f * pixelScale, 0.07f, font);
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        super.render(builder, mouseX, mouseY);

        // Hover-Tooltip für die Flamme
        float flameX = guiX + 70f * pixelScale;
        float flameY = guiY + 144f * pixelScale;
        float flameW = 16f * pixelScale;
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
