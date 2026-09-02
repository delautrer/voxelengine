package de.delautrer.game.ui.elements;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.game.ui.UIMeshBuilder;

public class UIProgressBar extends UIElement {
    private float progress = 0.0f;

    private static final float CORNER_SIZE = 4.0f;

    public UIProgressBar(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    public void setProgress(float progress) {
        // Stellt sicher, dass der Wert immer zwischen 0.0 und 1.0 bleibt
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    public float getProgress() {
        return this.progress;
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible)
            return;

        // 1. HINTERGRUND (Z = 0.1f)
        builder.add9Slice(x, y, 0.1f, width, height, 1, 0, CORNER_SIZE);

        // 2. FÜLLUNG (Z = 0.2f)
        float padding = CORNER_SIZE;

        float currentFillWidth = (width - 2.0f * padding) * progress;

        if (currentFillWidth > 0.0f) {
            builder.add9Slice(x + padding, y + padding, 0.2f, currentFillWidth, height - 2.0f * padding, 2, 0, CORNER_SIZE);
        }
    }
}
