package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;

public class UIProgressBar extends UIElement {
    private float progress = 0.0f;

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
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        // 1. HINTERGRUND (Z = 0.1f)
        builder.addAtlasQuad(x, y, 0.1f, width, height, 8, 0, 8, 1, MENU_GRID_SIZE, false);

        // 2. FÜLLUNG (Z = 0.2f)
        float padding = 4.0f;

        float maxFillWidth = width - (padding * 2);
        float currentFillWidth = maxFillWidth * progress;

        if (currentFillWidth > 0.0f) {
            builder.addCroppedAtlasQuad(x - 15f, y + padding, 0.2f, currentFillWidth, height - (padding * 2), 0, 1, progress, MENU_GRID_SIZE);
        }
    }
}