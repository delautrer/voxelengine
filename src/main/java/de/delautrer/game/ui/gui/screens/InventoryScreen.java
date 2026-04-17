package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.gui.Container;
import de.delautrer.game.ui.gui.Screen;
import de.delautrer.game.ui.gui.UIMeshBuilder;

public class InventoryScreen extends Screen {

    private final Container container;
    private float hx, hotbarY, invY, hotbarWidth, hotbarHeight, slotHitboxSize;

    public InventoryScreen(Container container) {
        this.container = container;
    }

    @Override
    protected void onInit() {
        hotbarWidth = 207.0f * pixelScale;
        hotbarHeight = 23.0f * pixelScale;
        slotHitboxSize = 22.0f * pixelScale;

        hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        hotbarY = (float) Math.floor(10.0f * pixelScale);
        invY = (float) Math.floor((height - (3 * hotbarHeight)) / 2.0f);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        int hoveredSlot = getHoveredSlot(mouseX, mouseY);

        for (int visualRow = 0; visualRow < 3; visualRow++) {
            float y = invY + (visualRow * hotbarHeight);
            builder.addAtlasQuad(hx, y, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);
        }

        for (int logicalRow = 0; logicalRow < 3; logicalRow++) {
            float rowY = invY + (logicalRow * hotbarHeight);
            for (int col = 0; col < 9; col++) {
                int slot = 9 + (logicalRow * 9) + col;
                float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                float selectorW = 23.0f * pixelScale;

                if (hoveredSlot == slot) {
                    builder.addAtlasQuad(slotX, rowY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
                }
                builder.drawItem(container.getInventory().getStack(slot), slotX, rowY, 0.0f, selectorW);
            }
        }

        if (container.getMouseStack() != null) {
            float itemSize = 23.0f * pixelScale;
            float invertedMouseY = height - mouseY;
            builder.drawItem(container.getMouseStack(), mouseX - itemSize / 2.0f, invertedMouseY - itemSize / 2.0f, -0.1f, itemSize);
        }
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        float invertedMouseY = height - mouseY;

        if (invertedMouseY >= hotbarY && invertedMouseY <= hotbarY + hotbarHeight) {
            for (int col = 0; col < 9; col++) {
                float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                if (mouseX >= slotX && mouseX <= slotX + slotHitboxSize) return col;
            }
        }

        for (int logicalRow = 0; logicalRow < 3; logicalRow++) {
            float rowY = invY + (logicalRow * hotbarHeight);
            if (invertedMouseY >= rowY && invertedMouseY <= rowY + hotbarHeight) {
                for (int col = 0; col < 9; col++) {
                    float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
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