package de.delautrer.game.ui.elements;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.game.ui.UIMeshBuilder;


public class UISeparator extends UIElement {
    private final String text;
    private final float lineHeight;
    private final float r, g, b, a;

    public UISeparator(float width, float height, String text, float lineHeight, float r, float g, float b, float a) {
        super(0, 0, width, height);
        this.text = text;
        this.lineHeight = lineHeight;
        this.r = r; this.g = g; this.b = b; this.a = a;
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        float currentX = x;

        // 1. Text zeichnen (falls vorhanden)
        if (text != null && !text.isEmpty() && font != null) {
            float textY = y + (height / 2.0f) - 10.0f;
            builder.drawText(text, currentX, textY, 0.2f, font);
            currentX += builder.getTextWidth(text, font) + 15.0f; // 15px Abstand zur Linie
        }

        // 2. Linie bis zum rechten Rand ziehen
        float lineWidth = (x + width) - currentX;
        if (lineWidth > 0) {
            float lineY = y + (height / 2.0f) - (lineHeight / 2.0f);
            builder.addRect(currentX, lineY, 0.1f, lineWidth, lineHeight, r, g, b, a);
        }
    }
}
