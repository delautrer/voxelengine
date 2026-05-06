package de.delautrer.game.ui.elements;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;
import java.util.ArrayList;
import java.util.List;

public class UIScrollableList extends UIElement {
    private final List<UIElement> items = new ArrayList<>();
    private float scrollOffset = 0.0f;
    private boolean isDragging = false;

    private final int bgGridX;
    private final int bgGridY;

    public UIScrollableList(float x, float y, float width, float height) {
        this(x, y, width, height, 3);
    }

    public UIScrollableList(float x, float y, float width, float height, int style) {
        super(x, y, width, height);
        switch (style) {
            case 1:
                this.bgGridX = 4;
                this.bgGridY = 0;
                break;
            case 2:
                this.bgGridX = 8;
                this.bgGridY = 0;
                break;
            default:
                bgGridX = 3;
                bgGridY = 0;
                break;
        }
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
            // Getter verwenden, um Felder nicht zu umgehen
            if (item.getY() + item.getHeight() > maxY) maxY = item.getY() + item.getHeight();
            if (item.getY() < minY) minY = item.getY();
        }
        return (maxY - minY) + 40.0f;
    }

    // --- NEU: Rekursiver Input für Layout-Container ---
    private void handleItemInput(UIElement item, InputManager input, float uiMouseX, float uiMouseY, boolean mouseJustPressed, boolean overScrollbar) {
        if (item instanceof UILayout) {
            // Wenn es eine Box ist, schick den Input an alle Kinder in der Box weiter
            for (UIElement child : ((UILayout) item).getChildren()) {
                handleItemInput(child, input, uiMouseX, uiMouseY, mouseJustPressed, overScrollbar);
            }
        } else {
            // Wenn es ein direktes Element ist, werte den Input aus
            if (item instanceof UISlider) {
                ((UISlider) item).handleInput(input, uiMouseX, uiMouseY);
            } else if (item instanceof UIKeybindButton) {
                ((UIKeybindButton) item).handleInput(input, uiMouseX, uiMouseY);
            }

            if (mouseJustPressed && !overScrollbar && item.isHovered(uiMouseX, uiMouseY)) {
                if (item instanceof UIButton) ((UIButton) item).click();
                else if (item instanceof UIConfirmButton) ((UIConfirmButton) item).click();
                else if (item instanceof UIToggleButton) ((UIToggleButton) item).click();
            }
        }
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

        // --- 2. BALKEN ZIEHEN ---
        float scrollPadding = 15.0f;
        float trackHeight = height - (scrollPadding * 2);
        float viewRatio = height / Math.max(1.0f, totalContentHeight);
        float scrollbarHeight = Math.max(20.0f, trackHeight * viewRatio);
        float scrollPercent = (maxScroll != 0) ? scrollOffset / maxScroll : 0;

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

        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // --- 3. INPUT AN KINDER WEITERLEITEN ---
        float clipPadding = 15.0f;
        float clipMinY = this.getY() + clipPadding;
        float clipMaxY = this.getY() + this.getHeight() - clipPadding;

        for (UIElement item : items) {
            float tempY = item.getY() + scrollOffset;

            if (tempY + item.getHeight() > clipMinY && tempY < clipMaxY) {
                float oldY = item.getY();
                // setPosition löst UILayout.pack() aus, dadurch ziehen Kind-Elemente mit!
                item.setPosition(item.getX(), tempY);

                handleItemInput(item, input, uiMouseX, uiMouseY, mouseJustPressed, overScrollbar);

                item.setPosition(item.getX(), oldY);
            }
        }
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        builder.add9Slice(x, y, 0.05f, width, height, bgGridX, bgGridY, 12.0f);

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

        float clipPadding = 15.0f;
        builder.setClipRect(x, y + clipPadding, width, height - (clipPadding * 2));

        float clipMinY = this.y + clipPadding;
        float clipMaxY = this.y + this.height - clipPadding;

        for (UIElement item : items) {
            float originalY = item.getY();
            // Verschiebt das Layout (und alle seine Kinder) in Render-Position
            item.setPosition(item.getX(), originalY + scrollOffset);

            if (item.getY() + item.getHeight() > clipMinY && item.getY() < clipMaxY) {
                item.render(builder, font, mouseX, mouseY);
            }

            // Stellt die originale Position wieder her
            item.setPosition(item.getX(), originalY);
        }

        builder.clearClipRect();
    }
}
