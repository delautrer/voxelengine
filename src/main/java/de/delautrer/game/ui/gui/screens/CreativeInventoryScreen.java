package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.gui.Container;
import de.delautrer.game.ui.gui.UIMeshBuilder;

import java.util.ArrayList;
import java.util.List;

public class CreativeInventoryScreen extends MenuScreen {

    private final Container container;
    private final List<Item> allItems;
    private VulkanFont font;

    private float hx, hotbarY, gridY, slotHitboxSize, hotbarWidth, hotbarHeight;
    private final int cols = 9;
    private int rows;

    public CreativeInventoryScreen(Container container) {
        this.container = container;
        this.allItems = new ArrayList<>(ItemRegistry.getAll().values());
        this.rows = (int) Math.ceil((double) allItems.size() / cols);
        if (this.rows == 0) this.rows = 1;
    }

    public void setFont(VulkanFont font) {
        this.font = font;
    }

    @Override
    protected void onInit() {
        hotbarWidth = 207.0f * pixelScale;
        hotbarHeight = 23.0f * pixelScale;
        slotHitboxSize = 22.0f * pixelScale;

        hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        hotbarY = (float) Math.floor(10.0f * pixelScale);
        gridY = hotbarY + hotbarHeight + (10.0f * pixelScale);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        int hoveredSlot = getHoveredSlot(mouseX, mouseY);

        // --- 1. SCHÖNES 9-SLICE HINTERGRUND-PANEL ---
        float padding = 10.0f * pixelScale;
        float panelW = hotbarWidth + padding * 2;
        float panelH = (gridY + (rows * hotbarHeight) - hotbarY) + padding * 2 + (15.0f * pixelScale);
        float panelX = hx - padding;
        float panelY = hotbarY - padding;

        builder.add9Slice(panelX, panelY, 0.3f, panelW, panelH, 0, 0, 8.0f * pixelScale);

        // --- 2. TITEL TEXT ---
        if (font != null) {
            float titleY = panelY + panelH - (15.0f * pixelScale);
            builder.drawText("Kreativmodus", panelX + padding, titleY, 0.4f, font);
        }

        // --- 3. HOTBAR & GRID HINTERGRUND ---
        builder.addAtlasQuad(hx, hotbarY, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);

        for (int row = 0; row < rows; row++) {
            float rowY = gridY + (row * hotbarHeight);
            builder.addAtlasQuad(hx, rowY, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);
        }

        // --- 4. ITEMS & SELEKTOR ---
        for (int col = 0; col < 9; col++) {
            float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
            float selectorW = 23.0f * pixelScale;

            if (hoveredSlot == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
            }
            builder.drawItem(container.getInventory().getStack(col), slotX, hotbarY, 0.0f, selectorW);
        }

        for (int row = 0; row < rows; row++) {
            float rowY = gridY + (row * hotbarHeight);
            for (int col = 0; col < 9; col++) {
                int itemIndex = (row * cols) + col;
                int virtualSlotId = 9 + itemIndex;
                float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                float selectorW = 23.0f * pixelScale;

                if (hoveredSlot == virtualSlotId && itemIndex < allItems.size()) {
                    builder.addAtlasQuad(slotX, rowY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
                }

                if (itemIndex < allItems.size()) {
                    Item item = allItems.get(itemIndex);
                    builder.drawItem(new ItemStack(item, 1), slotX, rowY, 0.0f, selectorW);
                }
            }
        }

        // --- 5. MAUS ITEM ---
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

        for (int row = 0; row < rows; row++) {
            float rowY = gridY + (row * hotbarHeight);
            if (invertedMouseY >= rowY && invertedMouseY <= rowY + hotbarHeight) {
                for (int col = 0; col < 9; col++) {
                    float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
                    if (mouseX >= slotX && mouseX <= slotX + slotHitboxSize) {
                        return 9 + (row * cols) + col;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
        int slot = getHoveredSlot(mouseX, mouseY);
        if (slot == -1) return;

        if (slot < 9) {
            container.handleSlotClick(slot);
        } else {
            int itemIndex = slot - 9;
            if (itemIndex < allItems.size()) {
                Item clickedItem = allItems.get(itemIndex);

                if (container.getMouseStack() == null) {
                    container.setMouseStack(new ItemStack(clickedItem, 64));
                } else if (container.getMouseStack().type == clickedItem) {
                    container.setMouseStack(null);
                } else {
                    container.setMouseStack(new ItemStack(clickedItem, 64));
                }
            }
        }
    }
}