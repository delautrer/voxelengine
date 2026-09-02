package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.input.InputManager;
import de.delautrer.game.crafting.StonecutterRecipe;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.gui.InventoryConstants;
import de.delautrer.game.ui.gui.container.StonecutterContainer;

import java.util.List;

public class StonecutterScreen extends ContainerScreen {
    private float panelX, panelY, panelW, panelH, padding;

    public static class Rect {
        public final float x, y, w, h;
        public Rect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public StonecutterScreen(StonecutterContainer container) {
        super(container);
    }

    @Override
    protected void onInit() {
        slotSize = InventoryConstants.SLOT_SIZE * pixelScale;

        float hotbarWidth = InventoryConstants.SLOT_SIZE * 9f * pixelScale;
        float containerPixelHeight = 188f * pixelScale;

        padding = 10.0f * pixelScale;
        panelW = hotbarWidth + padding * 2;
        panelH = containerPixelHeight + padding * 2 + (15.0f * pixelScale);

        panelX = (float) Math.floor((width - panelW) / 2.0f);
        panelY = (float) Math.floor((height - panelH) / 2.0f);

        guiX = panelX + padding;
        guiY = panelY + padding;
    }

    private Rect getRecipeButtonRect(int i) {
        int col = i % 6;
        int row = i / 6; // 0 = top, 1 = middle, 2 = bottom
        float x = guiX + (44f + 4f + col * 26f) * pixelScale;
        float y = guiY + (162f - row * 26f) * pixelScale;
        float w = 24f * pixelScale;
        float h = 24f * pixelScale;
        return new Rect(x, y, w, h);
    }

    @Override
    public void handleInput(InputManager input) {
        StonecutterContainer scContainer = (StonecutterContainer) container;
        float mouseX = input.getMouseX();
        float mouseY = input.getMouseY();
        float invertedMouseY = height - mouseY;

        if (input.isActionJustPressed("INTERACT_BREAK")) {
            List<StonecutterRecipe> recipes = scContainer.getCurrentRecipes();
            for (int i = 0; i < recipes.size(); i++) {
                Rect rect = getRecipeButtonRect(i);
                if (mouseX >= rect.x && mouseX <= rect.x + rect.w && invertedMouseY >= rect.y && invertedMouseY <= rect.y + rect.h) {
                    scContainer.selectRecipe(i);
                    return;
                }
            }
        }

        super.handleInput(input);
    }

    @Override
    protected void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY) {
        // 1. Haupt-Hintergrund Panel
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        // Titel "Stonecutter"
        if (font != null) {
            float titleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Stonecutter", panelX + padding, titleY, 0.1f, font);
        }

        // 2. Umrahmung für Input-Slot oben links (12, 162)
        float inputSlotX = guiX + (12f * pixelScale);
        float inputSlotY = guiY + (162f * pixelScale);
        builder.add9Slice(inputSlotX - 2 * pixelScale, inputSlotY - 2 * pixelScale, 0.05f, slotSize + 4 * pixelScale, slotSize + 4 * pixelScale, 0, 0, 2.0f * pixelScale);

        /*
        // 3. Umrahmung für Output-Slot unten links (12, 110)
        float outputSlotX = guiX + (12f * pixelScale);
        float outputSlotY = guiY + (110f * pixelScale);
        builder.add9Slice(outputSlotX - 2 * pixelScale, outputSlotY - 2 * pixelScale, 0.05f, slotSize + 4 * pixelScale, slotSize + 4 * pixelScale, 0, 0, 2.0f * pixelScale);
        */

        // 4. Pfeil nach unten (90° CW gedreht) zwischen Input und Output
        builder.addAtlasQuadRotated90CW(guiX + (16f * pixelScale), guiY + (140f * pixelScale), 0.05f, 16f * pixelScale, 16f * pixelScale, 3, 15, 1, 1, true);

        // 5. Rezept-Panel Hintergrund rechts (Dunkles 9-Slice Tile 1,0)
        float recipePanelX = guiX + (44f * pixelScale);
        float recipePanelY = guiY + (108f * pixelScale);
        float recipePanelW = 164f * pixelScale;
        float recipePanelH = 80f * pixelScale;
        builder.add9Slice(recipePanelX, recipePanelY, 0.05f, recipePanelW, recipePanelH, 1, 0, 4.0f * pixelScale);

        // 6. Rezept-Buttons Grid (6 Cols x 3 Rows, 24x24)
        StonecutterContainer scContainer = (StonecutterContainer) container;
        List<StonecutterRecipe> recipes = scContainer.getCurrentRecipes();
        int selected = scContainer.getSelectedRecipeIndex();

        float invertedMouseY = height - mouseY;
        int hoveredIndex = -1;

        for (int i = 0; i < recipes.size(); i++) {
            Rect rect = getRecipeButtonRect(i);
            boolean isMouseOver = (mouseX >= rect.x && mouseX <= rect.x + rect.w && invertedMouseY >= rect.y && invertedMouseY <= rect.y + rect.h);
            if (isMouseOver) {
                hoveredIndex = i;
            }
            boolean isSelected = (i == selected);
            int tileIndex = (isSelected || isMouseOver) ? 14 : 13;

            // Button Hintergrund
            builder.add9Slice(rect.x, rect.y, 0.08f, rect.w, rect.h, tileIndex, 0, 4.0f * pixelScale);

            StonecutterRecipe recipe = recipes.get(i);
            if (recipe != null && recipe.result != null) {
                float itemSize = InventoryConstants.ITEM_SIZE * pixelScale;
                builder.drawItem(recipe.result, rect.x + (rect.w - itemSize) * 0.5f, rect.y + (rect.h - itemSize) * 0.5f, 0.15f, itemSize);
            }
        }

        // 7. Tooltip bei Hover
        if (hoveredIndex >= 0 && hoveredIndex < recipes.size() && container.getMouseStack() == null) {
            StonecutterRecipe recipe = recipes.get(hoveredIndex);
            if (recipe != null && recipe.result != null && recipe.result.type != null) {
                drawTooltip(builder, recipe.result.type.getName(), mouseX, mouseY);
            }
        }
    }
}
