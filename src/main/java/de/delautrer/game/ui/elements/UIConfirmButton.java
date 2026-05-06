package de.delautrer.game.ui.elements;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.game.ui.UIMeshBuilder;


public class UIConfirmButton extends UIElement {
    private String normalText;
    private String confirmText;
    private boolean isWaitingForConfirm = false;
    private Runnable onConfirm;


    private static final int GRID_X_NORMAL = 0;
    private static final int GRID_Y_NORMAL = 0;
    private static final int GRID_X_HOVER = 1;
    private static final int GRID_Y_HOVER = 0;
    private static final float CORNER_SIZE = 8.0f;

    public UIConfirmButton(float x, float y, float width, float height, String normalText, String confirmText, Runnable onConfirm) {
        super(x, y, width, height);
        this.normalText = normalText;
        this.confirmText = confirmText;
        this.onConfirm = onConfirm;
    }

    public void click() {
        if (!isWaitingForConfirm) {
            isWaitingForConfirm = true; // Erster Klick: In den Bestätigungs-Modus wechseln
        } else {
            if (onConfirm != null) onConfirm.run(); // Zweiter Klick: Aktion ausführen!
            isWaitingForConfirm = false;
        }
    }

    // Wenn die Maus den Button verlässt, brechen wir den "Wirklich?" Modus ab
    public void updateHoverState(float mouseX, float mouseY) {
        if (isWaitingForConfirm && !isHovered(mouseX, mouseY)) {
            isWaitingForConfirm = false;
        }
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible) return;
        updateHoverState(mouseX, mouseY);

        if (isWaitingForConfirm) {
            builder.addRect(x, y, 0.1f, width, height, 0.8f, 0.2f, 0.2f, 1.0f); // Rot
        } else {
            boolean hovered = isHovered(mouseX, mouseY);

            int gridX = hovered ? GRID_X_HOVER : GRID_X_NORMAL;
            int gridY = hovered ? GRID_Y_HOVER : GRID_Y_NORMAL;

            builder.add9Slice(x, y, 0.1f, width, height, gridX, gridY, CORNER_SIZE);
        }

        // Text zentriert
        String text = isWaitingForConfirm ? confirmText : normalText;
        float textWidth = builder.getTextWidth(text, font);
        float textX = x + (width / 2.0f) - (textWidth / 2.0f);

        builder.drawText(text, textX, y + (height / 2.0f) - 10.0f, 0.2f, font);
    }
}
