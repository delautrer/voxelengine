package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.ui.gui.Container;
import de.delautrer.game.ui.gui.UIMeshBuilder;

public class InventoryScreen extends MenuScreen {

    private final Container container;
    private VulkanFont font;
    private float hx, hotbarY, invY, hotbarWidth, hotbarHeight, slotHitboxSize;

    public InventoryScreen(Container container) {
        this.container = container;
    }

    public void setFont(VulkanFont font) {
        this.font = font;
    }

    @Override
    protected void onInit() {
        hotbarWidth = 24f * 9f * pixelScale;
        hotbarHeight = 24f * pixelScale;
        slotHitboxSize = 24.0f * pixelScale;

        hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        hotbarY = height / 2.0f - hotbarHeight * 2.0f;

        invY = hotbarY + hotbarHeight + (10.0f * pixelScale);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        int hoveredSlot = getHoveredSlot(mouseX, mouseY);

        float padding = 10.0f * pixelScale;
        float panelW = hotbarWidth + padding * 2;
        float panelH = (invY + 3 * hotbarHeight - hotbarY) + padding * 2 + (15.0f * pixelScale);
        float panelX = hx - padding;
        float panelY = hotbarY - padding;

        builder.add9Slice(panelX, panelY, 0.3f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        // --- 2. TITEL TEXT ---
        if (font != null) {
            float titleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Inventory", panelX + padding, titleY, 0.4f, font);
        }

        // --- 3. HOTBAR HINTERGRUND ---
        for (int visualCol = 0; visualCol < 9; visualCol++) {
            builder.addAtlasQuad(hx + (visualCol * 24.0f) * pixelScale, hotbarY, 0.2f, 24.0f * pixelScale, 24.0f * pixelScale, 5,0, 1, 1, false);
        }
        //builder.addAtlasQuad(hx, hotbarY, 0.2f, hotbarWidth, hotbarHeight, 1, 1, 9, 1, false);

        // --- 4. INVENTAR HINTERGRUND ---
        for (int visualRow = 0; visualRow < 3; visualRow++) {
            float y = invY + (visualRow * hotbarHeight);
            for (int visualCol = 0; visualCol < 9; visualCol++) {
                builder.addAtlasQuad(hx + (visualCol * 24.0f) * pixelScale, y, 0.2f, 24.0f * pixelScale, 24.0f * pixelScale, 5,0, 1, 1, false);
            }
            //builder.addAtlasQuad(hx, y, 0.2f, hotbarWidth, hotbarHeight, 1, 1, 9, 1, false);
        }

        // --- 5. ITEMS & HOVER-EFFEKTE ---

        // Hotbar Items (Slots 0-8)
        for (int col = 0; col < 9; col++) {

            float slotX = hx + (col * 24.0f) * pixelScale;
            float selectorW = 24.0f * pixelScale;

            if (hoveredSlot == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
            }
            builder.drawItem(container.getInventory().getStack(col), slotX + 2, hotbarY + 2, 0.0f, selectorW - 4);
        }

        // Main Inventory Items (Slots 9-35)
        for (int logicalRow = 0; logicalRow < 3; logicalRow++) {
            float rowY = invY + (logicalRow * hotbarHeight);
            for (int col = 0; col < 9; col++) {
                int slot = 9 + (logicalRow * 9) + col;

                float slotX = hx + (col * 24.0f) * pixelScale;
                float selectorW = 24.0f * pixelScale;

                if (hoveredSlot == slot) {
                    builder.addAtlasQuad(slotX, rowY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
                }
                builder.drawItem(container.getInventory().getStack(slot), slotX + 2, rowY + 2, 0.0f, selectorW - 4);
            }
        }

        // --- 6. ITEM AN DER MAUS ---
        if (container.getMouseStack() != null) {
            float itemSize = 24.0f * pixelScale - 4;
            float invertedMouseY = height - mouseY;
            builder.drawItem(container.getMouseStack(), mouseX - itemSize / 2.0f + 2, invertedMouseY - itemSize / 2.0f + 2 , -0.1f, itemSize);
        }
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        float invertedMouseY = height - mouseY;

        if (invertedMouseY >= hotbarY && invertedMouseY <= hotbarY + hotbarHeight) {
            for (int col = 0; col < 9; col++) {
                float slotX = hx + (col * 24.0f) * pixelScale;
                if (mouseX >= slotX && mouseX <= slotX + slotHitboxSize) return col;
            }
        }

        for (int logicalRow = 0; logicalRow < 3; logicalRow++) {
            float rowY = invY + (logicalRow * hotbarHeight);
            if (invertedMouseY >= rowY && invertedMouseY <= rowY + hotbarHeight) {
                for (int col = 0; col < 9; col++) {
                    float slotX = hx + (col * 24.0f) * pixelScale;
                    if (mouseX >= slotX && mouseX <= slotX + slotHitboxSize) {
                        return 9 + (logicalRow * 9) + col;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
        int slot = getHoveredSlot(mouseX, mouseY);
        if (slot != -1) container.handleSlotClick(slot);
    }
}