package de.delautrer.game.ui.elements;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;

import java.util.ArrayList;
import java.util.List;

public class UIScrollableList extends UIElement {
    private final List<UIElement> items = new ArrayList<>();
    private float scrollOffset = 0.0f;
    private boolean isDragging = false;

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

    private float getContentHeight() {
        if (items.isEmpty()) return 0;
        float maxY = -999999f;
        float minY = 999999f;
        for (UIElement item : items) {
            if (item.y + item.height > maxY) maxY = item.y + item.height;
            if (item.y < minY) minY = item.y;
        }
        return (maxY - minY) + 40.0f;
    }

    public void handleInput(InputManager input, float uiMouseX, float uiMouseY) {
        if (!isVisible) return;

        float totalContentHeight = getContentHeight();
        float maxScroll = totalContentHeight - height;
        if (maxScroll < 0) maxScroll = 0;

        // --- 1. MAUSRAD ---
        if (isHovered(uiMouseX, uiMouseY)) {
            float scrollDelta = (float) input.consumeScroll();
            if (scrollDelta != 0.0f) {
                scrollOffset -= scrollDelta * 40.0f;
            }
        }

        // --- 2. BALKEN ZIEHEN (Mit Padding!) ---
        float scrollPadding = 15.0f; // Abstand oben und unten für den Balken
        float trackHeight = height - (scrollPadding * 2); // Die nutzbare Spur

        float viewRatio = height / Math.max(1.0f, totalContentHeight);
        float scrollbarHeight = Math.max(20.0f, trackHeight * viewRatio); // Relativ zur neuen Spurhöhe!

        float scrollPercent = (maxScroll != 0) ? scrollOffset / maxScroll : 0;

        // Die extremen Y-Grenzen des Balkens
        float minY = y + scrollPadding;
        float maxY = y + height - scrollPadding - scrollbarHeight;

        float scrollbarY = minY + (maxY - minY) * (1.0f - scrollPercent);

        boolean mousePressed = input.isActionActive("INTERACT_BREAK");
        boolean mouseJustPressed = input.isActionJustPressed("INTERACT_BREAK");

        boolean overScrollbar = uiMouseX >= x + width - 15.0f && uiMouseX <= x + width &&
                uiMouseY >= scrollbarY && uiMouseY <= scrollbarY + scrollbarHeight;

        if (mouseJustPressed && overScrollbar) {
            isDragging = true;
        }

        if (!mousePressed) {
            isDragging = false;
        }

        if (isDragging) {
            float dragY = uiMouseY - (scrollbarHeight / 2.0f);

            if (dragY < minY) dragY = minY;
            if (dragY > maxY) dragY = maxY;

            float percent = 1.0f - ((dragY - minY) / (maxY - minY));
            scrollOffset = percent * maxScroll;
        }

        // --- 3. CLAMPING ---
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // --- 4. KLICKS ---
        if (mouseJustPressed && !overScrollbar) {
            float clipPadding = 15.0f;
            float clipMinY = this.y + clipPadding;
            float clipMaxY = this.y + this.height - clipPadding;

            for (UIElement item : items) {
                float tempY = item.y + scrollOffset;

                if (tempY + item.height > clipMinY && tempY < clipMaxY) {
                    float oldY = item.y;
                    item.y = tempY;

                    if (item.isHovered(uiMouseX, uiMouseY)) {
                        if (item instanceof UIButton) ((UIButton) item).click();
                        else if (item instanceof UIConfirmButton) ((UIConfirmButton) item).click();
                    }
                    item.y = oldY;
                }
            }
        }
    }

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        // 1. Panel-Hintergrund
        builder.add9Slice(x, y, 0.05f, width, height, bgGridX, bgGridY, 12.0f);

        // 2. Scrollbalken (Mit Padding!)
        float totalContentHeight = getContentHeight();
        if (totalContentHeight > height) {
            float maxScroll = totalContentHeight - height;

            float scrollPadding = 20.0f;
            float trackHeight = height - (scrollPadding * 2);

            float viewRatio = height / totalContentHeight;
            float scrollbarHeight = Math.max(20.0f, trackHeight * viewRatio);

            float scrollPercent = scrollOffset / maxScroll;

            float minY = y + scrollPadding;
            float maxY = y + height - scrollPadding - scrollbarHeight;
            float scrollbarY = minY + (maxY - minY) * (1.0f - scrollPercent);

            builder.add9Slice(x + width - 15.0f, scrollbarY, 0.15f, 10.0f, scrollbarHeight, 15, 0, 4.0f);
        }

        // 3. Schere an!
        float clipPadding = 15.0f;
        builder.setClipRect(x, y + clipPadding, width, height - (clipPadding * 2));

        float clipMinY = this.y + clipPadding;
        float clipMaxY = this.y + this.height - clipPadding;

        for (UIElement item : items) {
            float originalY = item.y;
            item.y = originalY + scrollOffset;

            if (item.y + item.height > clipMinY && item.y < clipMaxY) {
                item.render(builder, font, mouseX, mouseY);
            }

            item.y = originalY;
        }

        // 4. Schere aus!
        builder.clearClipRect();
    }
}