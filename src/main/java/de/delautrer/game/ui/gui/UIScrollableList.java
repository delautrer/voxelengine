package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;

import java.util.ArrayList;
import java.util.List;

public class UIScrollableList extends UIElement {
    private final List<UIElement> items = new ArrayList<>();
    private float scrollOffset = 0.0f;

    private final int bgGridX = 3;
    private final int bgGridY = 0;

    public UIScrollableList(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    public void addItem(UIElement item) {
        items.add(item);
    }

    public void clear() {
        items.clear();
        scrollOffset = 0.0f;
    }

    // Wird vom MenuScreen aufgerufen
    public void handleInput(InputManager input, float uiMouseX, float uiMouseY) {
        if (!isVisible) return;

        // 1. Scrollrad abfragen, wenn wir über der Liste hovern
        if (isHovered(uiMouseX, uiMouseY)) {
            float scrollDelta = (float) input.consumeScroll();
            // Wenn Y=0 unten ist, bedeutet "runterscrollen", dass der Offset steigt
            scrollOffset += scrollDelta * 35.0f;
        }

        // 2. Klicks an die Kind-Elemente weitergeben
        if (input.isActionJustPressed("INTERACT_BREAK")) {
            for (UIElement item : items) {
                float tempY = item.y + scrollOffset;

                // Nur klickbar, wenn das Item sichtbar im Rahmen ist!
                if (tempY >= this.y && (tempY + item.height) <= (this.y + this.height)) {
                    // Wir verschieben das Item kurz für den Hover-Check
                    float oldY = item.y;
                    item.y = tempY;

                    if (item.isHovered(uiMouseX, uiMouseY)) {
                        if (item instanceof UIButton) ((UIButton) item).click();
                        else if (item instanceof UIConfirmButton) ((UIConfirmButton) item).click();
                    }
                    // Y wiederherstellen!
                    item.y = oldY;
                }
            }
        }
    }

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        // 1. Schöner Panel-Hintergrund (Z = 0.05f)
        builder.add9Slice(x, y, 0.05f, width, height, bgGridX, bgGridY, 12.0f);

        // 2. Items rendern
        for (UIElement item : items) {
            float originalY = item.y;
            item.y = originalY + scrollOffset; // Optisch verschieben

            // SOFTWARE CLIPPING: Nur zeichnen, wenn das Item komplett im Rahmen liegt!
            // (Damit es nicht über den Rand des Panels hinausragt)
            if (item.y >= this.y && (item.y + item.height) <= (this.y + this.height)) {
                item.render(builder, font, mouseX, mouseY);
            }

            // Koordinate wieder zurücksetzen
            item.y = originalY;
        }
    }
}