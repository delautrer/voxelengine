package de.delautrer.game.ui.elements;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.vulkan.texture.VulkanFont;
import de.delautrer.game.ui.UIMeshBuilder;

public class UILabel extends UIElement {
    private String text;

    public UILabel(float width, float height, String text) {
        super(0, 0, width, height);
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible || font == null || text == null) return;

        float textY = y + (height / 2.0f) - 10.0f;
        builder.drawText(text, x, textY, 0.2f, font);
    }
}
