package de.delautrer.game.ui.elements;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;
import java.util.Locale;
import java.util.function.Consumer;



public class UISlider extends UIElement {
    private final float min, max, step;
    private float value;
    private final String prefix;
    private final Consumer<Float> onChange;
    private boolean isDragging = false;

    public UISlider(float x, float y, float width, float height, String prefix, float min, float max, float step, float startValue, Consumer<Float> onChange) {
        super(x, y, width, height);
        this.prefix = prefix;
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = startValue;
        this.onChange = onChange;
    }

    public void handleInput(InputManager input, float mouseX, float mouseY) {
        if (!isVisible) return;

        boolean mouseDown = input.isActionActive("INTERACT_BREAK");
        boolean justPressed = input.isActionJustPressed("INTERACT_BREAK");

        if (justPressed && isHovered(mouseX, mouseY)) {
            isDragging = true;
        }
        if (!mouseDown) {
            isDragging = false;
        }

        if (isDragging) {
            float percent = (mouseX - this.x) / this.width;
            percent = Math.max(0.0f, Math.min(1.0f, percent));

            // Step anwenden (Runden auf die nächste Stufe)
            float rawValue = min + (percent * (max - min));
            this.value = Math.round(rawValue / step) * step;

            // Clamping, um Rechenfehler an den Rändern abzufangen
            this.value = Math.max(min, Math.min(max, this.value));

            if (onChange != null) {
                onChange.accept(this.value);
            }
        }
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        builder.add9Slice(x, y, 0.1f, width, height, 0, 0, 8.0f);

        float percent = (value - min) / (max - min);
        float knobWidth = 16.0f;
        float knobX = x + ((width - knobWidth) * percent);

        int knobGridX = (isHovered(mouseX, mouseY) || isDragging) ? 1 : 0;
        builder.add9Slice(knobX, y, 0.15f, knobWidth, height, knobGridX, 0, 8.0f);

        if (font != null) {
            String formattedValue;
            if (step % 1 == 0) {
                formattedValue = String.valueOf((int) value); // Keine Kommastellen
            } else {
                formattedValue = String.format(Locale.US, "%.1f", value); // 1 Kommastelle
            }

            String text = prefix + ": " + formattedValue;
            float textWidth = builder.getTextWidth(text, font);
            float textY = y + (height - 24.0f + 10f) / 2.0f;
            builder.drawText(text, x + (width / 2.0f) - (textWidth / 2.0f), textY, 0.2f, font);
        }
    }
}
