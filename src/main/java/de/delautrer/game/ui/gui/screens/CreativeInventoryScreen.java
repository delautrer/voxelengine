package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;
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

    // --- NEU: Scroll Variablen ---
    private int scrollRow = 0;
    private int maxScrollRow = 0;
    private int visibleRows;

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
        hotbarWidth = 24f * 9f * pixelScale;
        hotbarHeight = 24f * pixelScale;
        slotHitboxSize = 24.0f * pixelScale;

        hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        hotbarY = height / 2.0f - hotbarHeight * 2.0f;

        gridY = hotbarY + hotbarHeight + (10.0f * pixelScale);

        // --- NEU: Scroll Logik vorbereiten ---
        this.visibleRows = Math.min(9, rows); // Maximal 9 Reihen gleichzeitig sichtbar
        this.maxScrollRow = Math.max(0, rows - visibleRows);
    }

    // --- NEU: Mausrad fangen ---
    @Override
    public void handleInput(InputManager input) {
        super.handleInput(input);
        double scroll = input.consumeScroll();
        if (scroll != 0) {
            scrollRow -= (int) Math.signum(scroll);
            if (scrollRow < 0) scrollRow = 0;
            if (scrollRow > maxScrollRow) scrollRow = maxScrollRow;
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        int hoveredSlot = getHoveredSlot(mouseX, mouseY);

        float padding = 10.0f * pixelScale;
        float panelW = hotbarWidth + padding * 2 + (maxScrollRow > 0 ? 15.0f * pixelScale : 0); // Platz für Scrollbar
        float panelH = (gridY + (visibleRows * hotbarHeight) - hotbarY) + padding * 2 + (15.0f * pixelScale);
        float panelX = hx - padding;
        float panelY = hotbarY - padding;

        builder.add9Slice(panelX, panelY, 0.3f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        // --- 2. TITEL TEXT ---
        if (font != null) {
            float titleY = panelY + panelH - (18.0f * pixelScale);
            builder.drawText("Creative-Inventory", panelX + padding, titleY, 0.4f, font);
        }

        // --- 3. HOTBAR & GRID HINTERGRUND ---
        for (int visualCol = 0; visualCol < 9; visualCol++) {
            builder.addAtlasQuad(hx + (visualCol * 24.0f) * pixelScale, hotbarY, 0.2f, 24.0f * pixelScale, 24.0f * pixelScale, 5,0, 1, 1, false);
        }
        //builder.addAtlasQuad(hx, hotbarY, 0.2f, hotbarWidth, hotbarHeight, 1, 1, 9, 1, false);


        for (int visualRow = 0; visualRow < rows; visualRow++) {
            float y = gridY + (visualRow * hotbarHeight);
            for (int visualCol = 0; visualCol < 9; visualCol++) {
                builder.addAtlasQuad(hx + (visualCol * 24.0f) * pixelScale, y, 0.2f, 24.0f * pixelScale, 24.0f * pixelScale, 5,0, 1, 1, false);
            }
            //builder.addAtlasQuad(hx, y, 0.2f, hotbarWidth, hotbarHeight, 1, 1, 9, 1, false);
        }

        // --- 4. SCROLLBAR ZEICHNEN ---
        if (maxScrollRow > 0) {
            float scrollbarX = hx + hotbarWidth + 5.0f * pixelScale;
            float scrollbarY = gridY;
            float scrollbarW = 10.0f * pixelScale;
            float scrollbarH = visibleRows * hotbarHeight;

            builder.addRect(scrollbarX, scrollbarY, 0.2f, scrollbarW, scrollbarH, 0.1f, 0.1f, 0.1f, 1.0f);

            float handleH = Math.max(15.0f * pixelScale, scrollbarH * ((float)visibleRows / rows));
            float progress = (float) scrollRow / maxScrollRow;
            float handleY = scrollbarY + progress * (scrollbarH - handleH);

            builder.addRect(scrollbarX + 2*pixelScale, handleY, 0.1f, scrollbarW - 4*pixelScale, handleH, 0.6f, 0.6f, 0.6f, 1.0f);
        }

        // --- 5. ITEMS & SELEKTOR ---
        for (int col = 0; col < 9; col++) {
            float slotX = hx + (col * 24.0f) * pixelScale;
            float selectorW = 24.0f * pixelScale;

            if (hoveredSlot == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
            }
            builder.drawItem(container.getInventory().getStack(col), slotX + 2, hotbarY + 2, 0.0f, selectorW - 4);
        }

        for (int visualRow = 0; visualRow < visibleRows; visualRow++) {
            int actualRow = scrollRow + visualRow;
            float rowY = gridY + (visualRow * hotbarHeight);

            for (int col = 0; col < 9; col++) {
                int itemIndex = (actualRow * cols) + col;
                int virtualSlotId = 9 + (visualRow * cols) + col;

                float slotX = hx + (col * 24.0f) * pixelScale;
                float selectorW = 24.0f * pixelScale;

                if (hoveredSlot == virtualSlotId && itemIndex < allItems.size()) {
                    builder.addAtlasQuad(slotX, rowY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
                }

                if (itemIndex < allItems.size()) {
                    Item item = allItems.get(itemIndex);
                    builder.drawItem(new ItemStack(item, 1), slotX + 2, rowY + 2, 0.0f, selectorW - 4);
                }
            }
        }

        // --- 6. MAUS ITEM ---
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

        for (int visualRow = 0; visualRow < visibleRows; visualRow++) {
            float rowY = gridY + (visualRow * hotbarHeight);
            if (invertedMouseY >= rowY && invertedMouseY <= rowY + hotbarHeight) {
                for (int col = 0; col < 9; col++) {
                    float slotX = hx + (col * 24.0f) * pixelScale;
                    if (mouseX >= slotX && mouseX <= slotX + slotHitboxSize) {
                        return 9 + (visualRow * cols) + col;
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
            int virtualIndex = slot - 9;
            int visualRow = virtualIndex / cols;
            int col = virtualIndex % cols;
            int actualRow = scrollRow + visualRow;
            int itemIndex = actualRow * cols + col;

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