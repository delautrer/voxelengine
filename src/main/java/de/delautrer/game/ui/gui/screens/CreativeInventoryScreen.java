package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.gui.*;

public class CreativeInventoryScreen extends ContainerScreen {

    private float panelX, panelY, panelW, panelH;
    private final UIInputField searchField;

    public CreativeInventoryScreen(CreativeContainer container) {
        super(container);
        // FIX 1: Richtige Parameter für UIInputField (x, y, w, h, placeholder, maxLength)
        this.searchField = new UIInputField(0, 0, 100, 12, "Suchen...", 20);
    }

    @Override
    protected void onInit() {
        slotSize = 24f * pixelScale;
        float gridW = 24f * 9f * pixelScale;
        float gridH = 34f * pixelScale + (5 * 24f * pixelScale);

        guiX = (float) Math.floor((width - gridW) / 2.0f);
        guiY = (height - gridH) / 2.0f;

        panelW = gridW + 20.0f * pixelScale;
        panelH = gridH + 30.0f * pixelScale;
        panelX = guiX - 10.0f * pixelScale;
        panelY = guiY - 10.0f * pixelScale;

        // Suchfeld positionieren (Nutzt jetzt die neue setSize Methode!)
        searchField.setPosition(guiX + gridW - (80.0f * pixelScale), panelY + panelH - (20.0f * pixelScale));
        searchField.setSize(80.0f * pixelScale, 12.0f * pixelScale);
    }

    @Override
    public void handleInput(de.delautrer.engine.input.InputManager input) {
        // 1. Klick in oder außerhalb der Suchleiste
        if (input.isActionJustPressed("INTERACT_BREAK")) {
            float invertedY = height - input.getMouseY();
            boolean hovered = searchField.isHovered(input.getMouseX(), invertedY);

            // Wenn sich der Fokus ändert, TypingMode aktivieren/deaktivieren
            if (hovered != searchField.isFocused()) {
                searchField.setFocused(hovered);
                input.setTypingMode(hovered);
            }

            if (hovered) return; // Klick im Suchfeld abfangen
        }

        // 2. Wenn das Suchfeld aktiv ist, tippen wir!
        if (searchField.isFocused()) {
            searchField.handleInput(input);
            ((CreativeContainer)container).setSearchText(searchField.getText());

            // "PAUSE" ist die Escape-Taste in deinem InputManager
            if (input.isActionJustPressed("PAUSE")) {
                searchField.setFocused(false);
                input.setTypingMode(false); // TypingMode wieder aus!
            }
            return; // Blockiert Inventar-Klicks und das Schließen des Inventars
        }

        // 3. Standard Container-Input (Klicken auf Slots)
        super.handleInput(input);

        // 4. Scrollen im Creative Inventar
        double scroll = input.consumeScroll();
        if (scroll != 0) {
            CreativeContainer cc = (CreativeContainer) container;
            cc.scrollTo(cc.getScrollOffset() - (float)scroll);
        }
    }

    @Override
    protected void drawBackground(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Basis-Hintergrund
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        if (font != null) {
            builder.drawText("Creative Mode", panelX + 10.0f * pixelScale, panelY + panelH - (18.0f * pixelScale), 0.1f, font);
        }

        // Suchfeld zeichnen
        searchField.render(builder, font, mouseX, mouseY);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        super.render(builder, mouseX, mouseY);

        CreativeContainer cc = (CreativeContainer) container;

        // 1. Virtuelle Items zeichnen
        for (Slot slot : cc.slots) {
            if (slot.inventory == null) {
                Item item = cc.getItemInGrid(slot.slotIndex);
                if (item != null) {
                    float slotX = guiX + (slot.x * pixelScale);
                    float slotY = guiY + (slot.y * pixelScale);
                    builder.drawItem(new ItemStack(item, 1), slotX + 3 * pixelScale, slotY + 3 * pixelScale, 0.3f, slotSize - 6 * pixelScale);
                }
            }
        }

        // 2. Manueller Tooltip für Creative-Slots
        Slot hoveredSlot = getHoveredSlotObj(mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.inventory == null && container.getMouseStack() == null && font != null) {
            Item item = cc.getItemInGrid(hoveredSlot.slotIndex);
            if (item != null) {
                String name = de.delautrer.game.ui.UIUtils.formatItemName(de.delautrer.game.items.ItemRegistry.getId(item));
                drawTooltip(builder, name, mouseX, mouseY);
            }
        }
    }
}