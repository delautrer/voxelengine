package de.delautrer.game.ui.elements;

public class UIHBox extends UILayout {

    public UIHBox(float x, float y, float spacing) {
        super(x, y, spacing);
    }

    @Override
    public void pack() {
        float maxHeight = 0.0f;
        float totalWidth = 0.0f;

        // 1. Pass: Größe ausmessen
        for (UIElement child : children) {
            if (child.getHeight() > maxHeight) maxHeight = child.getHeight();
            totalWidth += child.getWidth() + spacing;
        }
        this.height = maxHeight;
        this.width = Math.max(0, totalWidth - spacing);

        // 2. Pass: Positionieren (und vertikal in der HBox zentrieren)
        float currentX = this.x;
        for (UIElement child : children) {
            float childY = this.y + (this.height / 2.0f) - (child.getHeight() / 2.0f);
            child.setPosition(currentX, childY);
            currentX += child.getWidth() + spacing;
        }
    }
}