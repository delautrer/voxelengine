package de.delautrer.game.ui.elements;

public class UIVBox extends UILayout {

    public UIVBox(float x, float y, float spacing) {
        super(x, y, spacing);
    }

    @Override
    public void pack() {
        float maxWidth = 0.0f;
        float totalHeight = 0.0f;

        // 1. Pass: Gesamtgröße ausmessen
        for (UIElement child : children) {
            if (child.getWidth() > maxWidth) maxWidth = child.getWidth();
            totalHeight += child.getHeight() + spacing;
        }
        this.width = maxWidth;
        this.height = Math.max(0, totalHeight - spacing);

        // 2. Pass: Positionieren (von OBEN nach UNTEN)
        // Wir starten an der obersten Kante der Box und gehen nach unten.
        float currentY = this.y + this.height;
        for (UIElement child : children) {
            // Wir ziehen erst die Höhe des Elements ab, um seine Y-Position (unten links) zu bekommen
            currentY -= child.getHeight();

            // X zentrieren
            float childX = this.x + (this.width / 2.0f) - (child.getWidth() / 2.0f);

            child.setPosition(childX, currentY);

            // Spacing für das nächste Element abziehen
            currentY -= spacing;
        }
    }
}