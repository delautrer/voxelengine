package de.delautrer.game.ui.gui.screens;

import de.delautrer.Constants;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.UIInputField;
import de.delautrer.game.ui.gui.container.CreativeContainer;
import de.delautrer.game.ui.gui.container.Slot;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.ui.UIUtils;
import de.delautrer.game.ui.gui.InventoryConstants;

public class CreativeInventoryScreen extends ContainerScreen {

    private float panelX, panelY, panelW, panelH;
    private final UIInputField searchField;

    public CreativeInventoryScreen(CreativeContainer container) {
        super(container);
        this.searchField = new UIInputField(0, 0, 100, 12, "Search...", 20);
    }

    @Override
    protected void onInit() {
        slotSize = InventoryConstants.SLOT_SIZE * pixelScale;
        float gridW = InventoryConstants.SLOT_SIZE * 9f * pixelScale;
        // Abstand 34px wie im Survival-Inventar
        float gridH = 34f * pixelScale + (6 * InventoryConstants.SLOT_SIZE * pixelScale);

        guiX = (float) Math.floor((width - gridW) / 2.0f);
        guiY = (height - gridH) / 2.0f;

        panelW = gridW + 20.0f * pixelScale;
        panelH = gridH + 30.0f * pixelScale;
        panelX = guiX - 10.0f * pixelScale;
        panelY = guiY - 10.0f * pixelScale;

        // Suchfeld positionieren (Oben rechts)
        searchField.setPosition(guiX + gridW - (80.0f * pixelScale), guiY + (6 * InventoryConstants.SLOT_SIZE * pixelScale) + 35.0f * pixelScale);
        searchField.setSize(80.0f * pixelScale, 14.0f * pixelScale);
    }

    @Override
    public void handleInput(InputManager input) {
        float mouseX = input.getMouseX();
        float mouseY = height - input.getMouseY();
        CreativeContainer cc = (CreativeContainer) container;

        // 1. ESC Handling für Suchfeld Fokus
        if (searchField.isFocused() && input.isActionJustPressed("PAUSE")) {
            searchField.setFocused(false);
            input.setTypingMode(false);
            input.consumeAction("PAUSE"); // Verhindert das Schließen des Inventars
            return;
        }

        // 2. Tab-Klicks abfangen
        if (input.isActionJustPressed("INTERACT_BREAK")) {
            float tabW = 28 * pixelScale;
            float tabH = 32 * pixelScale;
            float startY = panelY + panelH;

            for (int i = 0; i < CreativeContainer.CreativeTab.values().length; i++) {
                CreativeContainer.CreativeTab tab = CreativeContainer.CreativeTab.values()[i];
                float tx = (tab == CreativeContainer.CreativeTab.SEARCH) ? (panelX + panelW - tabW) : (panelX + i * (tabW + 2 * pixelScale));
                
                if (mouseX >= tx && mouseX <= tx + tabW && mouseY >= startY && mouseY <= startY + tabH) {
                    cc.setTab(tab);
                    return;
                }
            }
        }

        // 3. Suche (nur im SEARCH Tab)
        if (cc.getCurrentTab() == CreativeContainer.CreativeTab.SEARCH) {
            if (input.isActionJustPressed("INTERACT_BREAK")) {
                boolean hovered = searchField.isHovered(mouseX, mouseY);
                if (hovered != searchField.isFocused()) {
                    searchField.setFocused(hovered);
                    input.setTypingMode(hovered);
                }
                if (hovered) return;
            }

            if (searchField.isFocused()) {
                searchField.handleInput(input);
                cc.setSearchText(searchField.getText());
                return;
            }
        } else {
            if (searchField.isFocused()) {
                searchField.setFocused(false);
                input.setTypingMode(false);
            }
        }

        super.handleInput(input);

        // 4. Scrollen
        double scroll = input.consumeScroll();
        if (scroll != 0) {
            cc.setScrollOffset(cc.getScrollOffset() - (int) Math.signum(scroll));
        }
    }

    @Override
    protected void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY) {
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        drawTabs(builder, mouseX, mouseY);

        CreativeContainer cc = (CreativeContainer) container;
        if (font != null) {
            builder.drawText(cc.getCurrentTab().title, panelX + 10.0f * pixelScale, guiY + (6 * InventoryConstants.SLOT_SIZE * pixelScale) + 39.0f * pixelScale, 0.1f, font);
        }

        if (cc.getCurrentTab() == CreativeContainer.CreativeTab.SEARCH) {
            searchField.render(builder, font, mouseX, mouseY);
        }
        
        // Delete Slot Visualisierung (Premium Look)
        for (Slot slot : cc.slots) {
            if (slot.slotIndex == -1) {
                float sx = guiX + slot.x * pixelScale;
                float sy = guiY + slot.y * pixelScale;
                
                // Hintergrund: Standard Slot-Hintergrund (9-Slice)
                builder.add9Slice(sx, sy, 0.1f, slotSize, slotSize, 0, 0, 2 * pixelScale);
                
                // Rote Füllung (Semi-transparentes Dunkelrot)
                builder.addRect(sx + 2 * pixelScale, sy + 2 * pixelScale, 0.11f, slotSize - 4 * pixelScale, slotSize - 4 * pixelScale, 0.5f, 0.05f, 0.05f, 0.7f);
                
                if (font != null) {
                    // Das X etwas dicker und zentrierter
                    builder.drawText("X", sx + 9 * pixelScale, sy + 8 * pixelScale, 0.2f, font);
                }
            }
        }
    }

    private void drawTabs(UIMeshBuilder builder, float mouseX, float mouseY) {
        CreativeContainer cc = (CreativeContainer) container;
        float tabW = 28 * pixelScale;
        float tabH = 28 * pixelScale;
        float startY = panelY + panelH;

        for (int i = 0; i < CreativeContainer.CreativeTab.values().length; i++) {
            CreativeContainer.CreativeTab tab = CreativeContainer.CreativeTab.values()[i];
            float x = (tab == CreativeContainer.CreativeTab.SEARCH) ? (panelX + panelW - tabW) : (panelX + i * (tabW - 1 * pixelScale));
            boolean active = cc.getCurrentTab() == tab;

            builder.add9Slice(x, startY, 0.1f, tabW, tabH, active ? 14 : 13, 0, 8 * pixelScale);

            Item iconItem = ItemRegistry.get(Constants.NAMESPACE + ":" + tab.iconId);
            if (iconItem != null) {
                float itemSize = InventoryConstants.CREATIVE_TAB_ITEM_SIZE * pixelScale;
                builder.drawItem(new ItemStack(iconItem, 1), x + (tabW - itemSize) * 0.5f,
                        startY + (tabH - itemSize) * 0.5f, 0.2f, itemSize);
            }
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        super.render(builder, mouseX, mouseY);

        CreativeContainer cc = (CreativeContainer) container;

        for (Slot slot : cc.slots) {
            if (slot.inventory == null && slot.slotIndex != -1) {
                Item item = cc.getItemInGrid(slot.slotIndex);
                if (item != null) {
                    float slotX = guiX + (slot.x * pixelScale);
                    float slotY = guiY + (slot.y * pixelScale);
                    if (slotX >= -100) { // Sichtbarkeits-Check
                        float itemSize = InventoryConstants.ITEM_SIZE * pixelScale;
                        builder.drawItem(new ItemStack(item, 1), slotX + (slotSize - itemSize) * 0.5f,
                                slotY + (slotSize - itemSize) * 0.5f, 0.3f, itemSize);
                    }
                }
            }
        }

        float tabW = 28 * pixelScale;
        float tabH = 32 * pixelScale;
        float startY = panelY + panelH;
        for (int i = 0; i < CreativeContainer.CreativeTab.values().length; i++) {
            CreativeContainer.CreativeTab tab = CreativeContainer.CreativeTab.values()[i];
            float tx = (tab == CreativeContainer.CreativeTab.SEARCH) ? (panelX + panelW - tabW) : (panelX + i * (tabW + 2 * pixelScale));
            if (mouseX >= tx && mouseX <= tx + tabW && mouseY >= startY && mouseY <= startY + tabH) {
                drawTooltip(builder, tab.title, mouseX, mouseY);
            }
        }

        Slot hoveredSlot = getHoveredSlotObj(mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.inventory == null && hoveredSlot.slotIndex != -1 && container.getMouseStack() == null && font != null) {
            Item item = cc.getItemInGrid(hoveredSlot.slotIndex);
            if (item != null) {
                String name = UIUtils.formatItemName(ItemRegistry.getId(item));
                drawTooltip(builder, name, mouseX, mouseY);
            }
        } else if (hoveredSlot != null && hoveredSlot.slotIndex == -1) {
            drawTooltip(builder, "Delete Item", mouseX, mouseY);
        }
    }
}
