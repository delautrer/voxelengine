package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;

public class UIButton extends UIElement {
    private final String text;
    private final Runnable onClick;

    // Diese Grid-Koordinaten müssen evtl. an deine gui.png angepasst werden
    private static final int GRID_X_NORMAL = 0;
    private static final int GRID_Y_NORMAL = 4; // Beispiel-Koordinate für Button-Textur
    private static final int GRID_X_HOVER = 0;
    private static final int GRID_Y_HOVER = 5;  // Beispiel-Koordinate für Hovered-Button-Textur

    public UIButton(float x, float y, float width, float height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
    }

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        boolean hovered = isHovered(mouseX, mouseY);

        // Button Hintergrund zeichnen (Wir nehmen an, ein Button ist 2 "Grid-Units" breit)
        int gridX = hovered ? GRID_X_HOVER : GRID_X_NORMAL;
        int gridY = hovered ? GRID_Y_HOVER : GRID_Y_NORMAL;

        builder.addAtlasQuad(x, y, 0.0f, width, height, gridX, gridY, 2, 1, false);

        // Text zentriert zeichnen
        if (font != null) {
            // Grobe Schätzung für Text-Zentrierung (kann später verfeinert werden)
            float textWidth = text.length() * 12.0f; // Schätzung basierend auf Font-Größe
            float textX = x + (width - textWidth) / 2.0f;
            float textY = y + (height - 24.0f) / 2.0f; // 24.0f = font size

            // Text zeichnen (wir brauchen eine Methode, um Textfarbe zu ändern, falls nötig, aber vorerst standard)
            builder.drawText(text, textX, textY, 0.0f, font);
        }
    }

    public void click() {
        if (isVisible && onClick != null) {
            onClick.run();
        }
    }
}